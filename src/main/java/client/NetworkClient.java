package client;

import java.net.Socket;

public class NetworkClient {

    private final String address;
    private final int port;

    // Socket connection to the server
    private Socket socket;

    public NetworkClient(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void connect() {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected to server: " + address + ":" + port);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
