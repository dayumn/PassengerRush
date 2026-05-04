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

    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final Jeepney jeepney1;
    private final Jeepney jeepney2;

    private int playerId = -1;
    private boolean connected = false;
    private boolean running = false;

    public NetworkClient(String host, int port, Jeepney jeepney1, Jeepney jeepney2) {
        this.host = host;
        this.port = port;
        this.jeepney1 = jeepney1;
        this.jeepney2 = jeepney2;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
        System.out.println("[Client] Listening for server messages.");
    }

    public void sendPosition(double x, double y, int passengers, int points, String direction) {
        if (!connected || playerId == -1) return;
        send("POSITION:" + playerId + ":" + x + ":" + y + ":" + passengers + ":" + points + ":" + direction);
    }

    public void disconnect() {
        running = false;
        connected = false;
        try { if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }

    public int getPlayerId()     { return playerId; }
    public boolean isConnected() { return connected && running; }

    private void send(String message) {
        if (out != null) out.println(message);
    }

    private void handleMessage(String message) {
        if (message == null || message.isBlank()) return;
        String[] parts = message.split(":");

        switch (parts[0]) {

            case "ASSIGNED" -> {
                if (parts.length >= 2) {
                    playerId = Integer.parseInt(parts[1]);
                    System.out.println("[Client] I am Player " + playerId);
                }
            }

            case "STATE" -> {
                if (parts.length >= 10) applyState(parts);
            }

            default -> System.out.println("[Client] Unknown message: " + message);
        }
    }

    // KEY LOGIC:
    // - jeepney1 is THIS client's car. It moves locally in GameTimer. NEVER update it here.
    // - jeepney2 is the OPPONENT's car. Always update it from server STATE.
    //
    // The server always sends STATE with Player 1's data first, Player 2's data second.
    // So:
    //   If I am Player 1 → my data is p1*, opponent data is p2* → update jeepney2 with p2*
    //   If I am Player 2 → my data is p2*, opponent data is p1* → update jeepney2 with p1*
    private void applyState(String[] parts) {
        try {
            double p1x    = Double.parseDouble(parts[1]);
            double p1y    = Double.parseDouble(parts[2]);
            int    p1pass = Integer.parseInt(parts[3]);
            int    p1pts  = Integer.parseInt(parts[4]);
            String p1dir   = parts[5];

            double p2x    = Double.parseDouble(parts[6]);
            double p2y    = Double.parseDouble(parts[7]);
            int    p2pass = Integer.parseInt(parts[8]);
            int    p2pts  = Integer.parseInt(parts[9]);
            String p2dir   = parts[10];

            Platform.runLater(() -> {
                if (playerId == 1) {
                    // I am Player 1 — update jeepney2 (opponent) with Player 2's data
                    if (p2x != 0 && p2y != 0) {
                        jeepney2.setXPos(p2x);
                        jeepney2.setYPos(p2y);
                        jeepney2.setPassengers(p2pass);
                        setAbsolutePoints(jeepney2, p2pts);
                        jeepney2.setDirectionImage(p2dir);
                    }

                } else if (playerId == 2) {
                    // I am Player 2 — update jeepney2 (opponent) with Player 1's data
                    if (p1x != 0 && p1y != 0) {
                        jeepney2.setXPos(p1x);
                        jeepney2.setYPos(p1y);
                        jeepney2.setPassengers(p1pass);
                        setAbsolutePoints(jeepney2, p1pts);
                        jeepney2.setDirectionImage(p1dir);
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