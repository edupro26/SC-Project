package com.iot.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkServer {

    private final int port;
    private final ServerStorage srvStorage;

    public NetworkServer(int port) {
        this.port = port;
        this.srvStorage = new ServerStorage();
    }

    public void start() {
        System.out.println("Server started on port " + port);
        this.srvStorage.start();

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

                ServerConnection connection = new ServerConnection(input, output);

                connection.validateDevID(srvStorage.getConnections());
                srvStorage.addConnection(connection);
                connection.handleRequests();

                // Remove the connection from the list after the client disconnects
                srvStorage.removeConnection(connection);

                output.close();
                input.close();
                cliSocket.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
