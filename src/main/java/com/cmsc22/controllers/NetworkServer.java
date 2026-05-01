package com.cmsc22.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * NetworkServer — authoritative game server for PassengerRush multiplayer.
 *
 * Protocol (matches NetworkClient.java exactly):
 *
 *   S→C  ASSIGNED:<playerId>
 *        Sent immediately when a client connects. playerId is 1 or 2.
 *
 *   C→S  INPUT:<playerId>:<action>
 *        Client sends this each time a key is active (e.g. "UP", "DOWN", "LEFT",
 *        "RIGHT", "BOARD"). Server updates that player's state accordingly.
 *
 *   S→C  STATE:<p1x>:<p1y>:<p1pass>:<p1pts>:<p2x>:<p2y>:<p2pass>:<p2pts>
 *        Broadcast to both clients on every server tick (20 Hz).
 *
 * How to run:
 *   javac NetworkServer.java
 *   java com.cmsc22.server.NetworkServer          (uses default port 5000)
 *   java com.cmsc22.server.NetworkServer 6000     (custom port)
 */
public class NetworkServer {

    // Must match NetworkClient.DEFAULT_PORT
    public static final int DEFAULT_PORT = 5000;

    // Tick rate — how many times per second the server broadcasts STATE.
    // 20 Hz keeps it lightweight for TCP; bump to 30 if the game feels laggy.
    private static final int TICK_RATE_HZ = 20;
    private static final long TICK_MS = 1000L / TICK_RATE_HZ;

    // Grid cell size in pixels (must match client's CELL_SIZE)
    private static final int CELL_SIZE = 30;

    // Map dimensions in cells (from spec: 46 cols × 27 rows, 1380×800 canvas)
    private static final int COLS = 46;
    private static final int ROWS = 27;

    // Movement speed in pixels per tick
    private static final double SPEED = 3.0;

    // Max passengers a jeepney can carry
    private static final int CAPACITY = 14;

    // ----- Mutable game state (only touched by the tick thread) -----

    // Player positions
    private double p1x = 2 * CELL_SIZE;   // spawn cell (2, 2) for player 1
    private double p1y = 2 * CELL_SIZE;
    private double p2x = 43 * CELL_SIZE;  // spawn cell (43, 24) for player 2
    private double p2y = 24 * CELL_SIZE;

    // Passengers currently on board
    private int p1pass = 0;
    private int p2pass = 0;

    // Cumulative delivery points
    private int p1pts = 0;
    private int p2pts = 0;

    // Last known input per player (updated by ClientHandler threads, read by tick)
    // Volatile so the tick thread always sees the latest write from a handler thread.
    private volatile String p1LastAction = "IDLE";
    private volatile String p2LastAction = "IDLE";

    // ----- Connected clients -----

    private final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    // ----- Server socket -----

    private ServerSocket serverSocket;
    private boolean running = false;

    // ----- Entry point -----

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[Server] Invalid port argument, using default " + DEFAULT_PORT);
            }
        }
        new NetworkServer().start(port);
    }

    // ----- Start / stop -----

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("[Server] Listening on port " + port);
            System.out.println("[Server] Waiting for 2 players...");

            startTickLoop();
            acceptClients();

        } catch (IOException e) {
            System.err.println("[Server] Could not start: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: " + e.getMessage());
        }
    }

    // ----- Accept loop (main thread) -----

    /**
     * Blocks accepting connections until we have 2 players.
     * After that, further connection attempts are rejected (server is full).
     */
    private void acceptClients() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();

                if (clients.size() >= 2) {
                    // Server is full — politely close the extra connection
                    System.out.println("[Server] Extra connection from "
                            + socket.getInetAddress() + " rejected (server full).");
                    socket.close();
                    continue;
                }

                // Assign player ID: first client = 1, second = 2
                int playerId = clients.isEmpty() ? 1 : 2;

                ClientHandler handler = new ClientHandler(socket, playerId);
                clients.put(playerId, handler);

                Thread t = new Thread(handler, "ClientHandler-P" + playerId);
                t.setDaemon(true);
                t.start();

                System.out.println("[Server] Player " + playerId + " connected from "
                        + socket.getInetAddress());

                if (clients.size() == 2) {
                    System.out.println("[Server] Both players connected. Game started!");
                }

            } catch (IOException e) {
                if (running) {
                    System.err.println("[Server] Accept error: " + e.getMessage());
                }
            }
        }
    }

    // ----- Game tick loop -----

    /**
     * Runs at TICK_RATE_HZ. This is the ONLY place that:
     *   - reads lastAction for each player
     *   - updates positions and game state
     *   - broadcasts STATE to all connected clients
     *
     * Keeping all state mutations here means we never need locks on game state.
     */
    private void startTickLoop() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameTickThread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                tick();
            } catch (Exception e) {
                System.err.println("[Server] Tick error: " + e.getMessage());
            }
        }, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        // Only run game logic when both players are connected
        if (clients.size() < 2) return;

        // --- Apply inputs ---
        applyMovement(1, p1LastAction);
        applyMovement(2, p2LastAction);

        // --- Broadcast STATE ---
        String state = buildStateMessage();
        broadcast(state);
    }

    /**
     * Moves a player based on their last-known action.
     * Clamp positions to canvas bounds (1380 × 800 from spec).
     */
    private void applyMovement(int playerId, String action) {
        double maxX = (COLS - 1) * CELL_SIZE;
        double maxY = (ROWS - 1) * CELL_SIZE;

        if (playerId == 1) {
            switch (action) {
                case "UP"    -> p1y = Math.max(0, p1y - SPEED);
                case "DOWN"  -> p1y = Math.min(maxY, p1y + SPEED);
                case "LEFT"  -> p1x = Math.max(0, p1x - SPEED);
                case "RIGHT" -> p1x = Math.min(maxX, p1x + SPEED);
                case "BOARD" -> handleBoard(1);
                // IDLE → no movement
            }
        } else {
            switch (action) {
                case "UP"    -> p2y = Math.max(0, p2y - SPEED);
                case "DOWN"  -> p2y = Math.min(maxY, p2y + SPEED);
                case "LEFT"  -> p2x = Math.max(0, p2x - SPEED);
                case "RIGHT" -> p2x = Math.min(maxX, p2x + SPEED);
                case "BOARD" -> handleBoard(2);
            }
        }
    }

    /**
     * Placeholder for passenger board/unload logic.
     *
     * In your full game, check whether the jeepney is in a loading or unloading
     * zone (via mapGrid), then increment pass/pts accordingly.
     * For now this just demonstrates the hook point.
     */
    private void handleBoard(int playerId) {
        if (playerId == 1) {
            if (p1pass < CAPACITY) {
                p1pass++;
                System.out.println("[Server] P1 boarded a passenger. Total: " + p1pass);
            }
        } else {
            if (p2pass < CAPACITY) {
                p2pass++;
                System.out.println("[Server] P2 boarded a passenger. Total: " + p2pass);
            }
        }
    }

    /**
     * Formats the STATE message exactly as NetworkClient.applyGameState() expects:
     * STATE:<p1x>:<p1y>:<p1pass>:<p1pts>:<p2x>:<p2y>:<p2pass>:<p2pts>
     */
    private String buildStateMessage() {
        return "STATE"
                + ":" + p1x
                + ":" + p1y
                + ":" + p1pass
                + ":" + p1pts
                + ":" + p2x
                + ":" + p2y
                + ":" + p2pass
                + ":" + p2pts;
    }

    // ----- Broadcast helpers -----

    /** Send a message to all currently connected clients. */
    private void broadcast(String message) {
        for (ClientHandler handler : clients.values()) {
            handler.send(message);
        }
    }

    /** Send a message to one specific player. */
    private void sendToPlayer(int playerId, String message) {
        ClientHandler handler = clients.get(playerId);
        if (handler != null) {
            handler.send(message);
        }
    }

    // ----- Handle disconnection -----

    private void onClientDisconnected(int playerId) {
        clients.remove(playerId);
        System.out.println("[Server] Player " + playerId + " disconnected. "
                + "Waiting for reconnect or new player...");
    }

    // =========================================================================
    // Inner class: ClientHandler
    // One per connected player. Runs on its own thread.
    // Only responsibility: read INPUT messages from the client and update the
    // volatile lastAction field. All game logic stays in the tick thread.
    // =========================================================================

    private class ClientHandler implements Runnable {

        private final Socket socket;
        private final int playerId;
        private PrintWriter out;
        private boolean connected = false;

        ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                connected = true;

                // Tell the client which player slot they are
                send("ASSIGNED:" + playerId);

                String line;
                while (connected && (line = in.readLine()) != null) {
                    handleMessage(line);
                }

            } catch (SocketException e) {
                if (connected) {
                    System.err.println("[ClientHandler-P" + playerId + "] Connection lost: "
                            + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("[ClientHandler-P" + playerId + "] Read error: "
                        + e.getMessage());
            } finally {
                connected = false;
                closeQuietly();
                onClientDisconnected(playerId);
            }
        }

        /**
         * Parse incoming messages from this client.
         * Format:  INPUT:<playerId>:<action>
         *
         * We write to a volatile field, so the tick thread picks it up on the next tick.
         * No lock needed: one writer (this thread) + one reader (tick thread) on a
         * volatile is safe in Java.
         */
        private void handleMessage(String message) {
            if (message == null || message.isBlank()) return;

            String[] parts = message.split(":");

            switch (parts[0]) {
                case "INPUT" -> {
                    // INPUT:<playerId>:<action>
                    if (parts.length < 3) return;
                    String action = parts[2].toUpperCase();
                    if (playerId == 1) {
                        p1LastAction = action;
                    } else {
                        p2LastAction = action;
                    }
                }
                default ->
                    System.out.println("[ClientHandler-P" + playerId
                            + "] Unknown message: " + message);
            }
        }

        /** Thread-safe send — PrintWriter.println is synchronized internally. */
        void send(String message) {
            if (out != null && connected) {
                out.println(message);
            }
        }

        private void closeQuietly() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
