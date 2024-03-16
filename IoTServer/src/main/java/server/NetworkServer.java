package server;

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

        public ServerThread (Socket cliSocket) {
            this.cliSocket = cliSocket;
        }

        public void run() {
            System.out.println("Client connected: " + cliSocket.getInetAddress().getHostAddress());

            try {
                ObjectInputStream input = new ObjectInputStream(cliSocket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(cliSocket.getOutputStream());

                ServerConnection connection = new ServerConnection(input, output);
                System.out.println("Validating device ID...");
                connection.validateDevID(srvStorage.getConnections());
                System.out.println("Validating device info...");

                // TODO - Enable validation of device info after doing client side implementation

                //connection.validateDeviceInfo();
                //System.out.println("Device ID and info validated");

                srvStorage.addConnection(connection);
                System.out.println("Waiting for requests...");
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
