package com.iot.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class NetworkServer {

    private final int port;
    private List<DeviceConnection> connections;

    public NetworkServer(int port) {
        this.port = port;
        this.connections = new ArrayList<>();
    }

    public void start() {
        System.out.println("Server started on port " + port);
        System.out.println("Waiting for clients...");
        ServerSocket srvSocket = null;

        try {
            srvSocket = new ServerSocket(port);
            while (true) {
                Socket cliSocket = srvSocket.accept();
                new ServerThread(cliSocket).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        finally {
            if (srvSocket != null) {
                try {
                    srvSocket.close();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private class ServerThread extends Thread {
        private final Socket cliSocket;

        ServerThread (Socket cliSocket) {
            this.cliSocket = cliSocket;
        }

        public void run() {
            System.out.println("Client connected: " + cliSocket.getInetAddress().getHostAddress());

            try {
                ObjectInputStream input = new ObjectInputStream(cliSocket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(cliSocket.getOutputStream());

                DeviceConnection connection = new DeviceConnection(input, output, connections);
                connection.handler();

                // Remove the connection from the list after the client disconnects
                connections.remove(connection);
                cliSocket.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
