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

    // Player 1 state — written by Client 1, broadcast to Client 2
    private volatile double p1x = 0, p1y = 0;
    private volatile int p1pass = 0, p1pts = 0;

    // Player 2 state — written by Client 2, broadcast to Client 1
    private volatile double p2x = 0, p2y = 0;
    private volatile int p2pass = 0, p2pts = 0;

    private volatile ClientHandler player1 = null;
    private volatile ClientHandler player2 = null;

    private ServerSocket serverSocket;
    private boolean running = false;

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

    private void acceptClients() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();

                if (player1 != null && player2 != null) {
                    System.out.println("[Server] Rejected extra connection — server full.");
                    socket.close();
                    continue;
                }

                int assignedId = (player1 == null) ? 1 : 2;
                ClientHandler handler = new ClientHandler(socket, assignedId);

                if (assignedId == 1) player1 = handler;
                else                 player2 = handler;

                Thread t = new Thread(handler, "ClientHandler-P" + assignedId);
                t.setDaemon(true);
                t.start();

                System.out.println("[Server] Player " + assignedId + " connected from " + socket.getInetAddress());

                if (player1 != null && player2 != null) {
                    System.out.println("[Server] Both players connected — game started!");
                }

            } catch (IOException e) {
                if (running) System.err.println("[Server] Accept error: " + e.getMessage());
            }
        }
    }

    // Broadcasts STATE to both clients at 20 Hz
    // FORMAT: STATE:p1x:p1y:p1pass:p1pts:p2x:p2y:p2pass:p2pts
    private void startTickLoop() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TickThread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            if (player1 == null || player2 == null) return;

            String state = "STATE:" + p1x + ":" + p1y + ":" + p1pass + ":" + p1pts
                    + ":" + p2x + ":" + p2y + ":" + p2pass + ":" + p2pts;

            player1.send(state);
            player2.send(state);

        }, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    private void onDisconnect(int playerId) {
        System.out.println("[Server] Player " + playerId + " disconnected.");
        if (playerId == 1) player1 = null;
        else               player2 = null;
    }

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
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                send("ASSIGNED:" + playerId);
                System.out.println("[Server] Sent ASSIGNED:" + playerId);

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

        // FORMAT: POSITION:<playerId>:<x>:<y>:<passengers>:<points>
        private void handleMessage(String raw) {
            if (raw == null || raw.isBlank()) return;
            String[] p = raw.split(":");

            if (p[0].equals("POSITION") && p.length >= 6) {
                try {
                    double x    = Double.parseDouble(p[2]);
                    double y    = Double.parseDouble(p[3]);
                    int    pass = Integer.parseInt(p[4]);
                    int    pts  = Integer.parseInt(p[5]);

                    if (playerId == 1) { p1x = x; p1y = y; p1pass = pass; p1pts = pts; }
                    else               { p2x = x; p2y = y; p2pass = pass; p2pts = pts; }

                } catch (NumberFormatException e) {
                    System.err.println("[Server] Bad POSITION: " + raw);
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