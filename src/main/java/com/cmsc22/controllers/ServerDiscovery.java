package com.cmsc22.controllers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServerDiscovery {

    private static final int DISCOVERY_PORT = 5051;          // UDP broadcast port
    private static final String DISCOVERY_MSG = "PASSENGERRUSH_SERVER"; // beacon message
    private static final int BROADCAST_INTERVAL_MS = 1000;   // broadcast every 1 second
    private static final int DISCOVERY_TIMEOUT_MS  = 5000;   // client waits up to 5 seconds

    // -------------------------------------------------------------------------
    // SERVER SIDE — call this when the server starts.
    // Spawns a background thread that broadcasts the server's presence every second.
    // -------------------------------------------------------------------------
    public static void startBroadcasting() {
        Thread broadcaster = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                byte[] data = DISCOVERY_MSG.getBytes();

                System.out.println("[Discovery] Broadcasting server presence on port " + DISCOVERY_PORT);

                while (!Thread.currentThread().isInterrupted()) {
                    // Send to the general broadcast address
                    try {
                        DatagramPacket packet = new DatagramPacket(
                                data, data.length,
                                InetAddress.getByName("255.255.255.255"),
                                DISCOVERY_PORT);
                        socket.send(packet);
                    } catch (Exception ignored) {}

                    // Also send to each network interface's broadcast address
                    // (some routers block 255.255.255.255)
                    try {
                        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                        while (interfaces.hasMoreElements()) {
                            NetworkInterface iface = interfaces.nextElement();
                            if (iface.isLoopback() || !iface.isUp()) continue;
                            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                                InetAddress broadcast = addr.getBroadcast();
                                if (broadcast == null) continue;
                                DatagramPacket packet = new DatagramPacket(
                                        data, data.length, broadcast, DISCOVERY_PORT);
                                socket.send(packet);
                            }
                        }
                    } catch (Exception ignored) {}

                    Thread.sleep(BROADCAST_INTERVAL_MS);
                }
            } catch (Exception e) {
                System.err.println("[Discovery] Broadcast error: " + e.getMessage());
            }
        }, "ServerDiscovery-Broadcaster");

        broadcaster.setDaemon(true);
        broadcaster.start();
    }

    // -------------------------------------------------------------------------
    // CLIENT SIDE — call this when client starts.
    // Listens for a broadcast from the server and returns the server's IP.
    // Returns null if no server found within DISCOVERY_TIMEOUT_MS.
    // -------------------------------------------------------------------------
    public static String discoverServer() {
        System.out.println("[Discovery] Looking for server on the network...");

        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setBroadcast(true);
            socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            // Keep listening until we get the right message or timeout
            while (true) {
                try {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();

                    if (DISCOVERY_MSG.equals(message)) {
                        String serverIP = packet.getAddress().getHostAddress();
                        System.out.println("[Discovery] Found server at " + serverIP);
                        return serverIP;
                    }
                } catch (java.net.SocketTimeoutException e) {
                    System.out.println("[Discovery] No server found within " + DISCOVERY_TIMEOUT_MS + "ms.");
                    return null;
                }
            }

        } catch (java.net.BindException e) {
            // Port already in use — likely the server is on this same machine
            // Fall back to localhost
            System.out.println("[Discovery] Port in use (server on same machine?) — trying localhost.");
            return "localhost";
        } catch (Exception e) {
            System.err.println("[Discovery] Listen error: " + e.getMessage());
            return null;
        }
    }
}