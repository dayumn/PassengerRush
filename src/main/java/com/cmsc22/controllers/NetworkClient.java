package com.cmsc22.controllers;

import com.cmsc22.models.Jeepney;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

// NetworkClient - connects this game instance to a NetworkServer.
// 1. Create a NetworkClient and call connect().
// 2. Call startListening() to begin the background thread.
// 3. Inside GameTimer.handle(), call sendInput(action).
// 4. The client automatically updates jeepney1/jeepney2 via Platform.runLater.
// 5. Call disconnect() when the game ends.

public class NetworkClient {
    // Default port that NetworkServer listens on.
    public static final int DEFAULT_PORT = 5000;

    private final String host;
    private final int port;

    // Holders for sending and receiving
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final Jeepney jeepney1;
    private final Jeepney jeepney2;

    // Player ID assigned by the server (1 or 2).
    private int playerId = -1;
    private boolean connected = false;
    private boolean running = false;

    // host - IP address of NetworkServer machine.
    // port - Port number the server is listening on.
    public NetworkClient(String host, int port, Jeepney jeepney1, Jeepney jeepney2) {
        this.host = host;
        this.port = port;
        this.jeepney1 = jeepney1;
        this.jeepney2 = jeepney2;
    }

    // Create sockets and open input/output streams
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            System.out.println("[NetworkClient] Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[NetworkClient] Could not connect: " + e.getMessage());
            return false;
        }
    }

    // Starts a background thread that continuously reads messages from the server.
    public void startListening() {
        if (!connected) {
            System.err.println("[NetworkClient] startListening() called before connect().");
            return;
        }

        running = true;

        Thread listenerThread = new Thread(() -> {
            try {
                String message = in.readLine();
                while (running && message != null) {
                    handleServerMessage(message);
                }
            } catch (SocketException e) {
                if (running) {
                    System.err.println("[NetworkClient] Connection lost: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("[NetworkClient] Read error: " + e.getMessage());
            } finally {
                running = false;
            }
        }, "NetworkClient-Listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
        System.out.println("[NetworkClient] Listener thread started.");
    }

    // Sends a player-input command to the server.
    // Call this from your GameTimer.handle() every time a key is active:
    // if (activeKeys.contains(KeyCode.W)) => networkClient.sendInput("UP");
    // else if (activeKeys.contains(KeyCode.S)) => networkClient.sendInput("DOWN");
    public void sendInput(String action) {
        if (!connected || playerId == -1)
            return;
        send("INPUT:" + playerId + ":" + action);
    }

    // Close the socket
    public void disconnect() {
        running = false;
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[NetworkClient] Error closing socket: " + e.getMessage());
        }
        System.out.println("[NetworkClient] Disconnected.");
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return connected && running;
    }

    private void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // Handle server messages appropriately
    private void handleServerMessage(String message) {
        if (message == null || message.isBlank())
            return;

        String[] parts = message.split(":");

        switch (parts[0]) {
            // Tentative Messages lemme know how server is implemented - hugz
            // Player ID assignment
            // Format: ASSIGNED:<playerId>
            case "ASSIGNED":
                if (parts.length >= 2) {
                    playerId = Integer.parseInt(parts[1]);
                    System.out.println("[NetworkClient] Assigned as Player " + playerId);
                }
                break;

            // Game State (Coordinates and Points and stuff)
            // Format: STATE:<p1x>:<p1y>:<p1pass>:<p1pts>:<p2x>:<p2y>:<p2pass>:<p2pts>
            case "STATE":
                if (parts.length >= 9) {
                    applyGameState(parts);
                }
                break;

            default:
                System.out.println("[NetworkClient] Unknown message: " + message);
                break;
        }
    }

    // Change the game state appropriately
    private void applyGameState(String[] parts) {
        try {
            final double p1x = Double.parseDouble(parts[1]);
            final double p1y = Double.parseDouble(parts[2]);
            final int p1pass = Integer.parseInt(parts[3]);
            final int p1pts = Integer.parseInt(parts[4]);

            final double p2x = Double.parseDouble(parts[5]);
            final double p2y = Double.parseDouble(parts[6]);
            final int p2pass = Integer.parseInt(parts[7]);
            final int p2pts = Integer.parseInt(parts[8]);

            // Always update the UI on the JavaFX thread
            Platform.runLater(() -> {
                // Jeepney 1
                jeepney1.setXPos(p1x);
                jeepney1.setYPos(p1y);
                jeepney1.setPassengers(p1pass);
                // Points: set the absolute total received from server
                setAbsolutePoints(jeepney1, p1pts);

                // Jeepney 2
                jeepney2.setXPos(p2x);
                jeepney2.setYPos(p2y);
                jeepney2.setPassengers(p2pass);
                setAbsolutePoints(jeepney2, p2pts);
            });

        } catch (NumberFormatException e) {
            System.err.println("[NetworkClient] Malformed STATE message: " + e.getMessage());
        }
    }

    // Jeepney only has incrementPoints(), so we need to sync real server points
    private void setAbsolutePoints(Jeepney jeepney, int serverTotal) {
        int delta = serverTotal - jeepney.getPoints();
        if (delta != 0) {
            jeepney.incrementPoints(delta);
        }
    }

}
