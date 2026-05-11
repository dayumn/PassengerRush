package com.cmsc22.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkServer {

    public static final int DEFAULT_PORT = 5050;
    private static final int TICK_RATE_HZ = 20;
    private static final long TICK_MS = 1000L / TICK_RATE_HZ;

    // Spawn positions — adjust these to change where each player starts
    public static final double P1_SPAWN_X = 630, P1_SPAWN_Y = 180;
    public static final double P2_SPAWN_X = 810, P2_SPAWN_Y = 180;

    // Player 1 state
    private volatile double  p1x = P1_SPAWN_X, p1y = P1_SPAWN_Y;
    private volatile int     p1pass = 0, p1pts = 0;
    private volatile String  p1dir  = "DOWN";
    private volatile boolean p1visible = true;

    // Player 2 state
    private volatile double  p2x = P2_SPAWN_X, p2y = P2_SPAWN_Y;
    private volatile int     p2pass = 0, p2pts = 0;
    private volatile String  p2dir  = "DOWN";
    private volatile boolean p2visible = true;

    private volatile ClientHandler player1   = null;
    private volatile ClientHandler player2   = null;
    private volatile boolean       gameStarted = false;

    private ServerSocket serverSocket;
    private boolean      running = false;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException ignored) {}
        }
        new NetworkServer().start(port);
    }

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

    // FIX #5 — slots are freed on LEAVE/disconnect so reconnection works
    private void acceptClients() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();

                int assignedId = -1;
                if      (player1 == null) assignedId = 1;
                else if (player2 == null) assignedId = 2;

                if (assignedId == -1) {
                    System.out.println("[Server] Rejected — server full.");
                    socket.close();
                    continue;
                }

                // Reset that player's state for a fresh round
                if (assignedId == 1) {
                    p1x = P1_SPAWN_X; p1y = P1_SPAWN_Y;
                    p1pass = 0; p1pts = 0; p1dir = "DOWN"; p1visible = true;
                } else {
                    p2x = P2_SPAWN_X; p2y = P2_SPAWN_Y;
                    p2pass = 0; p2pts = 0; p2dir = "DOWN"; p2visible = true;
                }

                ClientHandler handler = new ClientHandler(socket, assignedId);
                if (assignedId == 1) player1 = handler;
                else                 player2 = handler;

                Thread t = new Thread(handler, "ClientHandler-P" + assignedId);
                t.setDaemon(true);
                t.start();

                System.out.println("[Server] Player " + assignedId + " connected from "
                        + socket.getInetAddress());

                // FIX #1 — only send START when both players are connected
                if (player1 != null && player2 != null && !gameStarted) {
                    gameStarted = true;
                    new Thread(() -> {
                        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                        broadcast("START");
                        System.out.println("[Server] Sent START to both players.");
                    }).start();
                }

            } catch (IOException e) {
                if (running) System.err.println("[Server] Accept error: " + e.getMessage());
            }
        }
    }

    // Tick loop — broadcasts STATE to both clients
    // FORMAT: STATE:p1x:p1y:p1pass:p1pts:p1dir:p1visible:p2x:p2y:p2pass:p2pts:p2dir:p2visible
    private void startTickLoop() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TickThread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            if (player1 == null || player2 == null) return;
            String state = "STATE:"
                    + p1x + ":" + p1y + ":" + p1pass + ":" + p1pts + ":" + p1dir + ":" + p1visible
                    + ":" + p2x + ":" + p2y + ":" + p2pass + ":" + p2pts + ":" + p2dir + ":" + p2visible;
            player1.send(state);
            player2.send(state);
        }, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    private void broadcast(String message) {
        if (player1 != null) player1.send(message);
        if (player2 != null) player2.send(message);
    }

    // FIX #5 — free slot on disconnect so next client can fill it
    private void onDisconnect(int playerId) {
        System.out.println("[Server] Player " + playerId + " disconnected — slot freed.");
        if (playerId == 1) player1 = null;
        else               player2 = null;
        gameStarted = false; // allow next pair to get a fresh START
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int    playerId;
        private PrintWriter  out;
        private boolean      connected = false;

        ClientHandler(Socket socket, int playerId) {
            this.socket   = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                connected = true;

                // FIX #2 — send spawn position inside ASSIGNED message
                // FORMAT: ASSIGNED:<id>:<spawnX>:<spawnY>
                double spawnX = (playerId == 1) ? P1_SPAWN_X : P2_SPAWN_X;
                double spawnY = (playerId == 1) ? P1_SPAWN_Y : P2_SPAWN_Y;
                send("ASSIGNED:" + playerId + ":" + spawnX + ":" + spawnY);

                String line;
                while (connected && (line = in.readLine()) != null) {
                    handleMessage(line);
                }

            } catch (SocketException e) {
                if (connected) System.err.println("[P" + playerId + "] Lost connection.");
            } catch (IOException e) {
                System.err.println("[P" + playerId + "] IO error: " + e.getMessage());
            } finally {
                connected = false;
                closeQuietly();
                onDisconnect(playerId);
            }
        }

        private void handleMessage(String raw) {
            if (raw == null || raw.isBlank()) return;
            String[] p = raw.split(":");

            switch (p[0]) {

                // FORMAT: POSITION:<id>:<x>:<y>:<pass>:<pts>:<dir>
                case "POSITION" -> {
                    if (p.length < 7) return;
                    try {
                        double x    = Double.parseDouble(p[2]);
                        double y    = Double.parseDouble(p[3]);
                        int    pass = Integer.parseInt(p[4]);
                        int    pts  = Integer.parseInt(p[5]);
                        String dir  = p[6];
                        if (playerId == 1) { p1x=x; p1y=y; p1pass=pass; p1pts=pts; p1dir=dir; }
                        else               { p2x=x; p2y=y; p2pass=pass; p2pts=pts; p2dir=dir; }
                    } catch (NumberFormatException e) {
                        System.err.println("[Server] Bad POSITION: " + raw);
                    }
                }

                // FIX #3 — client hit manhole, mark invisible and broadcast
                case "FELL" -> {
                    if (playerId == 1) p1visible = false;
                    else               p2visible = false;
                    broadcast("FELL:" + playerId);
                    System.out.println("[Server] Player " + playerId + " fell.");
                }

                // FIX #5 — client leaving, free the slot
                case "LEAVE" -> {
                    System.out.println("[Server] Player " + playerId + " sent LEAVE.");
                    connected = false;
                }
            }
        }

        void send(String message) {
            if (out != null && connected) out.println(message);
        }

        private void closeQuietly() {
            try { if (!socket.isClosed()) socket.close(); }
            catch (IOException ignored) {}
        }
    }
}