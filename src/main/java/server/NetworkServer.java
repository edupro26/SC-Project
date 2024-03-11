package server;

import java.net.ServerSocket;
import java.net.Socket;

public class NetworkServer {
    private final int port;

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

        // Wait for the client to send a message and then send a response
        try {
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

            while (true) {
                String msg = (String) input.readObject();
                System.out.println("Received: " + msg);

                // Handle the message

                output.writeObject("OK");
            }

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
