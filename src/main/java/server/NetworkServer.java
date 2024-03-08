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

            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void handleClient(Socket clientSocket) {
        // Handle Client Connectiosn
    }

    private void close() {
        try {
            server.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
