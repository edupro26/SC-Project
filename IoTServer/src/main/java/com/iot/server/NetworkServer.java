package com.iot.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class NetworkServer {
    private final int port;

    private ArrayList<NetworkConnection> connections = new ArrayList<>();

    // Server socket
    private ServerSocket server;

    public NetworkServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            server = new ServerSocket(port);

            System.out.println("Server started on port " + port);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void handleClient(Socket clientSocket) {
        System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

        try {
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

            NetworkConnection connection = new NetworkConnection(input, output, connections);
            connection.handler();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }

    private void close() {
        try {
            server.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
