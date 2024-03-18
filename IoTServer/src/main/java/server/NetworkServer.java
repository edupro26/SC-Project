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

        public synchronized void run() {
            String clientAddress = cliSocket.getInetAddress().getHostAddress();
            System.out.println("Connection request received (" + clientAddress + ")");

            try {
                ObjectInputStream input = new ObjectInputStream(cliSocket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(cliSocket.getOutputStream());

                ServerConnection connection = new ServerConnection(input, output, clientAddress);

                System.out.print("Validating device ID... ");
                boolean validID = connection.validateDevID(srvStorage.getConnections());

                // FIXME Enable when client app is finished and
                //  don't forget to update size in device_info.csv
//                System.out.print("Validating device info...");
//                boolean validInfo = connection.validateDeviceInfo();

                if (validID /*&& validInfo*/) {
                    System.out.println("Client connected (" + clientAddress + ")");
                    srvStorage.addConnection(connection);
                    System.out.println("Active connections: " + srvStorage.getConnections().size());

                    connection.handleRequests();

                    // Remove the connection from the list after the client disconnects
                    srvStorage.removeConnection(connection);
                    System.out.println("Active connections: " + srvStorage.getConnections().size());
                }

                output.close();
                input.close();
                cliSocket.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

}
