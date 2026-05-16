package com.cmsc22.controllers;

import com.cmsc22.models.Jeepney;
import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class NetworkClient {

    public static final int DEFAULT_PORT = 5050;

    private final String  host;
    private final int     port;
    private Jeepney jeepney1; // this client's jeepney — never overwritten from server
    private Jeepney jeepney2; // opponent's jeepney — updated from server STATE

    private Socket       socket;
    private PrintWriter  out;
    private BufferedReader in;

    private int     playerId  = -1;
    private boolean connected = false;
    private boolean running   = false;
    
    private double spawnX = 0;
    private double spawnY = 0;

    // FIX #1 — listener that GameTimer registers to know when to start
    public interface GameStartListener { void onGameStart(); }
    private GameStartListener gameStartListener = null;

    // FIX #3 — listener that GameTimer registers to know when opponent fell
    public interface FellListener { void onOpponentFell(); }
    private FellListener fellListener = null;

    public interface LobbyUpdateListener { void onLobbyUpdate(boolean p1c, boolean p1r, boolean p2c, boolean p2r); }
    private LobbyUpdateListener lobbyUpdateListener = null;

    public NetworkClient(String host, int port, Jeepney jeepney1, Jeepney jeepney2) {
        this.host     = host;
        this.port     = port;
        this.jeepney1 = jeepney1;
        this.jeepney2 = jeepney2;
    }

    public void setJeepneys(Jeepney j1, Jeepney j2) {
        this.jeepney1 = j1;
        this.jeepney2 = j2;
        if (this.jeepney1 != null && (spawnX != 0 || spawnY != 0)) {
            this.jeepney1.setXPos(spawnX);
            this.jeepney1.setYPos(spawnY);
        }
    }

    public void setLobbyUpdateListener(LobbyUpdateListener listener) {
        this.lobbyUpdateListener = listener;
    }

    // FIX #1 — GameTimer calls this to be notified when START arrives
    public void setGameStartListener(GameStartListener listener) {
        this.gameStartListener = listener;
    }

    // FIX #3 — GameTimer calls this to be notified when opponent fell
    public void setFellListener(FellListener listener) {
        this.fellListener = listener;
    }

    public boolean connect() {
        try {
            socket    = new Socket(host, port);
            out       = new PrintWriter(socket.getOutputStream(), true);
            in        = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            System.out.println("[Client] Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Could not connect: " + e.getMessage());
            return false;
        }
    }

    public void startListening() {
        if (!connected) return;
        running = true;

        Thread t = new Thread(() -> {
            try {
                String message;
                while (running && (message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (SocketException e) {
                if (running) System.err.println("[Client] Connection lost.");
            } catch (IOException e) {
                System.err.println("[Client] Read error: " + e.getMessage());
            } finally {
                running = false;
            }
        }, "NetworkClient-Listener");

        t.setDaemon(true);
        t.start();
    }

    // FORMAT: POSITION:<id>:<x>:<y>:<pass>:<pts>:<dir>
    public void sendPosition(double x, double y, int passengers, int points, String direction) {
        if (!connected || playerId == -1) return;
        send("POSITION:" + playerId + ":" + x + ":" + y + ":" + passengers + ":" + points + ":" + direction);
    }

    // FIX #3 — tell server this client's jeepney fell into manhole
    public void sendFell() {
        if (!connected || playerId == -1) return;
        send("FELL:" + playerId);
    }

    public void sendReady() {
        if (!connected || playerId == -1) return;
        send("READY:" + playerId);
    }

    public void sendStartGame() {
        if (!connected) return;
        send("START_GAME");
    }

    // FIX #5 — tell server this client is leaving so slot is freed
    public void sendLeave() {
        if (!connected) return;
        send("LEAVE");
    }

    public void disconnect() {
        sendLeave();
        running   = false;
        connected = false;
        try { if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }

    public int     getPlayerId()  { return playerId;          }
    public boolean isConnected()  { return connected && running; }

    private void send(String message) {
        if (out != null) out.println(message);
    }

    private void handleMessage(String message) {
        if (message == null || message.isBlank()) return;
        String[] parts = message.split(":");

        switch (parts[0]) {

            // FIX #2 — ASSIGNED now includes spawn position
            // FORMAT: ASSIGNED:<id>:<spawnX>:<spawnY>
            case "ASSIGNED" -> {
                if (parts.length >= 4) {
                    playerId = Integer.parseInt(parts[1]);
                    spawnX = Double.parseDouble(parts[2]);
                    spawnY = Double.parseDouble(parts[3]);
                    Platform.runLater(() -> {
                        if (jeepney1 != null) {
                            jeepney1.setXPos(spawnX);
                            jeepney1.setYPos(spawnY);
                        }
                    });
                    System.out.println("[Client] I am Player " + playerId
                            + " spawning at " + spawnX + "," + spawnY);
                }
            }

            // FIX #1 — server says both players are ready, start the game
            case "START" -> {
                System.out.println("[Client] Received START — game beginning.");
                Platform.runLater(() -> {
                    if (gameStartListener != null) gameStartListener.onGameStart();
                });
            }

            case "LOBBY_STATE" -> {
                if (parts.length >= 5) {
                    boolean p1c = Boolean.parseBoolean(parts[1]);
                    boolean p1r = Boolean.parseBoolean(parts[2]);
                    boolean p2c = Boolean.parseBoolean(parts[3]);
                    boolean p2r = Boolean.parseBoolean(parts[4]);
                    Platform.runLater(() -> {
                        if (lobbyUpdateListener != null) {
                            lobbyUpdateListener.onLobbyUpdate(p1c, p1r, p2c, p2r);
                        }
                    });
                }
            }

            // STATE format:
            // STATE:p1x:p1y:p1pass:p1pts:p1dir:p1visible:p2x:p2y:p2pass:p2pts:p2dir:p2visible
            case "STATE" -> {
                if (parts.length >= 13) applyState(parts);
            }

            // FIX #3 — opponent fell into manhole, hide their jeepney
            case "FELL" -> {
                if (parts.length >= 2) {
                    int whoFell = Integer.parseInt(parts[1]);
                    // Only act if it's the OPPONENT who fell (not us)
                    if (whoFell != playerId) {
                        Platform.runLater(() -> {
                            if (fellListener != null) fellListener.onOpponentFell();
                        });
                    }
                }
            }

            default -> System.out.println("[Client] Unknown: " + message);
        }
    }

    // KEY LOGIC:
    // jeepney1 = local player — NEVER updated from server
    // jeepney2 = opponent     — always updated from server STATE
    //
    // If I am Player 1 → opponent data is p2* fields
    // If I am Player 2 → opponent data is p1* fields
    private void applyState(String[] parts) {
        try {
            double  p1x       = Double.parseDouble(parts[1]);
            double  p1y       = Double.parseDouble(parts[2]);
            int     p1pass    = Integer.parseInt(parts[3]);
            int     p1pts     = Integer.parseInt(parts[4]);
            String  p1dir     = parts[5];
            boolean p1visible = Boolean.parseBoolean(parts[6]);

            double  p2x       = Double.parseDouble(parts[7]);
            double  p2y       = Double.parseDouble(parts[8]);
            int     p2pass    = Integer.parseInt(parts[9]);
            int     p2pts     = Integer.parseInt(parts[10]);
            String  p2dir     = parts[11];
            boolean p2visible = Boolean.parseBoolean(parts[12]);

            Platform.runLater(() -> {
                if (playerId == 1 && jeepney2 != null) {
                    // I am Player 1 — update opponent (jeepney2) with p2 data
                    if (p2x != 0 || p2y != 0) {
                        jeepney2.setXPos(p2x);
                        jeepney2.setYPos(p2y);
                        jeepney2.setPassengers(p2pass);
                        setAbsolutePoints(jeepney2, p2pts);
                        jeepney2.setDirectionImage(p2dir);
                        jeepney2.setVisible(p2visible); // FIX #3
                    }
                } else if (playerId == 2 && jeepney2 != null) {
                    // I am Player 2 — update opponent (jeepney2) with p1 data
                    if (p1x != 0 || p1y != 0) {
                        jeepney2.setXPos(p1x);
                        jeepney2.setYPos(p1y);
                        jeepney2.setPassengers(p1pass);
                        setAbsolutePoints(jeepney2, p1pts);
                        jeepney2.setDirectionImage(p1dir);
                        jeepney2.setVisible(p1visible); // FIX #3
                    }
                }
            });

        } catch (NumberFormatException e) {
            System.err.println("[Client] Bad STATE: " + e.getMessage());
        }
    }

    private void setAbsolutePoints(Jeepney j, int serverTotal) {
        int delta = serverTotal - j.getPoints();
        if (delta != 0) j.incrementPoints(delta);
    }
}