# PassengerRush — CMSC 137 Networked Game Specification

## Context

PassengerRush is an existing 1v1 jeepney game written in JavaFX 17 for an OOP class two years
ago. The current course (CMSC 137, Second Semester AY 2025–2026) requires a Java networked
game with a minimum of **4 players**, delivered across two graded milestones plus an optional
bonus:

- **Milestone 1** (due 2026-05-01): basic single-user game
- **Milestone 2** (due 2026-05-13): networked version
- **Bonus** (final week): in-game chat (only credited if M1 and M2 are both delivered)

Class rules forbid use of game engines / pre-built game loops; 2D sprite/graphics libraries
are allowed. This means JavaFX is fine but LibGDX is not.

The team is 3 people, GitHub-tracked. The codebase will be rebuilt around this spec.

**Goal of this rebuild:** turn the existing JavaFX 1v1 prototype into a 4-player free-for-all
networked game (server-authoritative, hybrid TCP+UDP) while reusing assets, MVC structure,
and game mechanics. Maintain a clean separation that lets a headless server run the
authoritative simulation, with JavaFX clients rendering snapshots.

## High-level decisions

| Area | Decision |
|---|---|
| Tech stack | JavaFX 17 + Maven (kept). No game engines. |
| Game mode | 4-player free-for-all, 90-second timed round, highest score wins (ties → multi-winner) |
| Network topology | Dedicated server process; clients connect via IP:port |
| Authority | Server-authoritative state; clients send inputs only |
| Transport | Hybrid: TCP for lobby/handshake/chat/end-of-game, UDP for per-tick INPUT and STATE |
| Wire format | Custom delimited strings (`MSG\|key=val\|...`) — zero deps, easy to inspect |
| Lobby | Host IP entry → players list with ready toggles → host starts |
| M1 / M2 split | M1 = solo + 3 bots locally; M2 = real network clients replace bots; Bonus = chat |
| Tick rate | Server simulates and broadcasts STATE at 30 Hz; client renders @ 60 Hz; client sends INPUT @ 20 Hz |

## Architecture: 3 Maven modules

```
PassengerRush/                          (parent POM, packaging=pom)
├── shared/      pure Java, NO JavaFX   models, GameWorld, Protocol
├── server/      pure Java, NO JavaFX   ServerMain, Acceptor, ClientHandler, GameSession
└── client/      depends on JavaFX      Main, scenes, ClientGameLoop, NetworkClient, BotController
```

Why split:
- `shared` is the wire-format and simulation contract — same `Jeepney`/`Snapshot` classes on
  both server and client → no class-version drift
- `server` is JavaFX-free, packagable as a runnable jar, runnable on any lab machine
- `client` is the only module that pulls JavaFX dependencies → packaging stays clean

### Concurrency contract

Server (3 thread types, N+2 threads total for N players):

1. **Acceptor** — `serverSocket.accept()` loop, spawns one `ClientHandler` per connection.
2. **ClientHandler × N** — blocking TCP reads, parses lobby/chat/leave messages,
   pushes events to one `ConcurrentLinkedQueue` consumed by the tick thread.
   All TCP writes to that handler's `BufferedWriter` are wrapped in `synchronized(writer)`.
3. **GameTickThread** — at 30 Hz: drains UDP INPUTs (non-blocking
   `DatagramSocket.setSoTimeout(0)` + drain), drains TCP event queue, steps `GameWorld`,
   encodes STATE and sends via UDP to each registered client address, encodes any
   LOBBY/END/CHAT and writes via TCP.

The tick thread is the **only** thread that touches `GameWorld` or the UDP socket.
This eliminates locks on game state.

Client (3 threads total):

1. **JavaFX FX thread** — `AnimationTimer` at 60 Hz: reads `volatile Snapshot
   snapshotRef`, renders, captures input. Every third frame (~20 Hz) sends UDP `INPUT`.
2. **UdpReader** — blocking `receive()` loop; parses STATE; writes `snapshotRef`.
3. **TcpReader** — lobby/chat/end messages; pushes to FX via `Platform.runLater(...)`.

The cardinal JavaFX rule: **no UI mutation from a network thread without
`Platform.runLater`**. The render hot path avoids `runLater` entirely by reading a
single `volatile` reference.

## Networking protocol (wire format)

Two channels per client. UDP datagrams stay under 1 KB.

### Connection lifecycle

```
Client                                                        Server
  │── TCP CONNECT :5555 ────────────────────────────────►       │
  │── HELLO|name=bry|version=1 ──────────────────────────►      │  allocate playerId
  │ ◄────────── WELCOME|id=2|udpPort=5556|tickRateHz=30 ────────│
  │── (open UDP socket, send first packet) ──────────────►      │
  │── UDP_HELLO|id=2 ────────────────────────────────────►      │  registers udpAddr for id
  │ ◄────────── LOBBY|count=4|p1=bry:ready|p2=joy:notready|... ─│  (re-sent on every change)
  │── READY ──────────────────────────────────────────────►     │
  │ ◄────────── START|tick=0|seed=42|durationMs=90000 ──────────│  (host triggers start)
  │ ═══ GAME LOOP (UDP) ═══════════════════════════════════════│
  │── INPUT|tick=120|keys=W,D,E ─────────────────────────►      │
  │ ◄────── STATE|tick=120|t=12345|j=1:120.5,300.0,0,4,2,0;... ─│
  │ ═══ END ═══════════════════════════════════════════════════│
  │ ◄────────── END|winner=2|scores=1:8,2:14,3:11,4:9 ──────────│  (TCP)
```

### Message catalog

| Msg | Channel | Direction | Body |
|---|---|---|---|
| `HELLO` | TCP | C→S | `name=…\|version=1` |
| `WELCOME` | TCP | S→C | `id=…\|udpPort=…\|tickRateHz=30` |
| `UDP_HELLO` | UDP | C→S | `id=…` (registers UDP address) |
| `LOBBY` | TCP | S→C (broadcast) | `count=…\|p1=name:state\|p2=…` (resent on change) |
| `READY` / `UNREADY` | TCP | C→S | (no body) |
| `START` | TCP | S→C (broadcast) | `tick=0\|seed=…\|durationMs=90000` |
| `INPUT` | UDP | C→S | `tick=…\|keys=W,A,S,D,E` (subset of pressed keys) |
| `STATE` | UDP | S→C | `tick=…\|t=…\|j=id:x,y,dir,score,load,inv;…\|pwr=…\|pas=…\|man=…` |
| `END` | TCP | S→C (broadcast) | `winner=id\|scores=id:n,…` |
| `CHAT` (bonus) | TCP | C→S→all | `from=id\|msg=…` |
| `LEAVE` | TCP | C→S | (no body) |

### Loss / out-of-order handling

- Each STATE carries `tick`. Client drops any with `tick < lastSeen`.
- INPUT loss is invisible — server reuses last INPUT for that player on the next tick.
- TCP disconnect → server marks player gone, removes their jeep, broadcasts new LOBBY/STATE,
  client returns to title with toast.

**Why custom strings over JSON / Java serialization**: zero dependencies, every message fits
in a single UDP datagram, easy to demo with `nc` / `tcpdump` ("look prof, our packets"),
matches the README's existing stylistic suggestion.

## Game logic adaptation

**Carry over from existing code (unchanged or near-unchanged):**

- 1380×800 canvas, 30 px cell grid (46 cols × 27 rows)
- `mapGrid` cell semantics: 0 = blocked, 1 = road, 2 = unload zone, 3/6/8/9 = loading zones (terminals)
- 3-second dwell to load passengers, 8-second dwell to unload at terminal
- Capacity = 14 passengers
- Power-ups: speed (self boost), crack (slow target), insurance (one-collision invincibility)
- Manhole obstacle: hit = reset to spawn + drop all passengers

**Generalized 1v1 → 4-player:**

| Concern | Today (1v1) | Networked (4P) |
|---|---|---|
| Players | hardcoded `jeepney1`, `jeepney2` | `Map<Integer playerId, Jeepney>` |
| Spawns | 2 fixed cells | 4 fixed spawn cells, one per slot |
| Skin | hardcoded `Jeep1*.png` / `Jeep2*.png` | `skinId ∈ {1,2,3,4}` chosen in lobby; client looks up images |
| Input | scene KeyEvents → branch on player1/2 | client always uses WASD + E locally → encoded into UDP `INPUT` |
| Pairwise collision | `j1.collidesWith(j2)` | nested loop over players; both lose passengers + 2 s freeze |
| Round length | 180 s | **90 s** |
| Win | higher `points` after 180 s | highest `points` after 90 s; multi-winner shown on tie |

**Tweaks for 4-player fairness:**

- Power-ups respawn every 7 s (was 10 s), max 3 onscreen (was 2)
- 1.5 s spawn-invincibility after manhole reset to prevent grief loops
- Manhole relocates every 20 s (currently placed once and never moves)

**Bot AI (Milestone 1 only):**

- A* / BFS on the grid, cost-weighted to avoid manholes
- Policy: if `passengers < capacity` → nearest passenger; else → nearest unload zone
- 30 % chance to use power-up each second when held
- `BotController` produces synthetic `INPUT` messages locally — `GameWorld` doesn't know
  the difference between a bot input and a UDP-sourced one

## File layout (new + modified)

Files retired from the existing tree: `controllers/GameTimer.java` (split into `GameWorld`
in shared + `ClientGameLoop` in client) and `views/LoadingArea.java` (folded into `MapGrid`
as pure data).

```
PassengerRush/
├── pom.xml                              [MODIFY → parent POM, packaging=pom]
├── shared/
│   ├── pom.xml                          [NEW]
│   └── src/main/java/com/cmsc22/shared/
│       ├── models/
│       │   ├── Sprite.java              [MODIFY → no JavaFX; x/y/w/h only]
│       │   ├── Jeepney.java             [MODIFY → add skinId, drop Image fields]
│       │   ├── Passenger.java           [MODIFY → no Image]
│       │   ├── PowerUp.java             [MODIFY → no Image]
│       │   └── Manhole.java             [MODIFY → no Image]
│       ├── game/
│       │   ├── GameWorld.java           [NEW → headless deterministic sim]
│       │   ├── MapGrid.java             [NEW → extracted from GameStage]
│       │   ├── Input.java               [NEW → enum/bitmask of pressed keys]
│       │   └── Snapshot.java            [NEW → immutable state record]
│       └── net/
│           ├── Protocol.java            [NEW → encode/decode]
│           ├── MsgType.java             [NEW → enum]
│           └── NetConstants.java        [NEW → default ports, tick rate]
├── server/
│   ├── pom.xml                          [NEW → depends on shared]
│   └── src/main/java/com/cmsc22/server/
│       ├── ServerMain.java              [NEW → CLI entry: java -jar pr-server.jar 5555]
│       ├── Acceptor.java                [NEW]
│       ├── ClientHandler.java           [NEW → TCP read loop per player]
│       ├── GameSession.java             [NEW → owns GameTickThread + UDP socket]
│       └── PlayerSlot.java              [NEW → id, name, tcpWriter, udpAddr, ready]
└── client/
    ├── pom.xml                          [NEW → depends on shared + javafx]
    └── src/main/java/com/cmsc22/client/
        ├── Main.java                    [MOVE/MODIFY from views/Main.java]
        ├── views/
        │   ├── TitleScene.java          [NEW → Solo / Multiplayer / Quit]
        │   ├── LobbyScene.java          [NEW → host IP, players list, ready]
        │   ├── GameStage.java           [MODIFY → render-from-snapshot only]
        │   ├── GameOverScene.java       [MODIFY → multi-player scoreboard]
        │   ├── ChatPanel.java           [NEW (bonus)]
        │   └── Graphics.java            [MODIFY → skin lookup by skinId]
        ├── controllers/
        │   ├── ClientGameLoop.java      [REPLACES GameTimer.java; AnimationTimer]
        │   ├── NetworkClient.java       [NEW → TCP/UDP sockets + reader threads]
        │   ├── BotController.java       [NEW (M1 only)]
        │   └── InputCapture.java        [NEW → keyboard → Input bitmask]
        └── module-info.java             [MODIFY → opens com.cmsc22.client.views]
```

## Critical existing functions to reuse

These survive the refactor with minor adjustments — do NOT rewrite from scratch:

- `controllers/GameTimer.java` `handlePassengerPickup`, `loadPassengers`,
  `handlePassengerUnload`, `unloadPassengers`, `isInLoadingZone`, `isInUnloadZone`,
  `handlePowerUps`, `handlePowerUpActivation`, `activateSpeedBoost`, `activateCrack`,
  `placeManhole`, `handleManholeCollision`, `spawnPowerUp`, `spawnPassengers`,
  `placeInitialPassengers`, `canMoveTo`, `getGridX`, `getGridY` — port these into
  `shared/game/GameWorld.java`, swap JavaFX `Timeline` for tick-based countdowns
  (`int ticksLeft`).
- `views/GameStage.java` `mapGrid` literal (lines 48–76) — copy to `shared/game/MapGrid.java`
  as a `static final int[][]`.
- `models/Jeepney.java` getters/setters for points, passengers, speed — keep, drop the
  `Image` fields and `render(GraphicsContext)` method; rendering moves to the client view.
- All sprites under `src/main/resources/assets/images/` — copy into
  `client/src/main/resources/assets/images/`.

## Verification

### Milestone 1 (single-user)

- `mvn -pl shared test` passes:
  - passenger pickup increments load after 3 s in zone
  - unload at terminal after 8 s adds `load` to `score`, resets `load` to 0
  - manhole hit resets position + drops passengers
  - power-up effects (speed, crack, insurance) apply and expire correctly
  - 90 s timeout produces correct winner (incl. multi-way tie)
  - **determinism check**: two `GameWorld(seed=42)` fed identical input sequences produce
    identical final states (sets us up for sane M2 debugging)
- `mvn -pl client javafx:run` → choose "Practice (Solo)" → 1 human + 3 bots play a full 90 s
  round; bots visibly pick up + deliver passengers; scoreboard updates; game-over scene
  shows winner.

### Milestone 2 (networked)

- `java -jar server/target/pr-server.jar 5555 --seed 42` starts cleanly, prints
  listening TCP and UDP ports.
- 4 client instances on the same LAN connect, lobby fills, host clicks Start, all 4 play a
  synchronized round. Final scoreboards match across all clients.
- Kill one client mid-round: jeep is removed, server stays up, round ends correctly for the
  remaining 3.
- `-Dpr.netlog=true` produces a readable log of `HELLO/WELCOME/INPUT/STATE/END` lines —
  bring this to the demo.
- Manual responsiveness check on LAN: <100 ms perceived input lag (note in report).

### Bonus (chat)

- Chat panel works in lobby and in-game; T to focus, Enter to send, Esc to dismiss.
- All 4 clients receive every message in send-order; messages truncated to 120 chars and
  stripped of control characters server-side.

## Risks / gotchas to watch

1. **JavaFX class-version drift between server and client**: prevented by sharing one `shared`
   jar. Don't duplicate model classes across modules.
2. **`mapGrid` indexing bug**: existing code does `mapGrid[gridY][gridX]` — preserve that
   row-major convention when porting.
3. **`Timeline`-based delays in `GameTimer`**: these depend on JavaFX's animation pulse and
   wall-clock time. In headless `GameWorld`, replace with tick counters (`ticksLeft--` per
   `step()`).
4. **NAT / campus Wi-Fi AP isolation**: dedicated server requires all 4 clients reachable.
   Demo on a wired LAN or a single laptop's hotspot. Document this in the report.
5. **UDP packet size**: STATE for 4 players ≈ 200 bytes — well within MTU. If we add more
   features, keep it under ~1200 bytes to be safe.
6. **5-week timeline pressure**: keep bot AI dumb; focus polish on networking demo.
