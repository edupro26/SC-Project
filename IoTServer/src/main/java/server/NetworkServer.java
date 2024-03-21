package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkServer {

    private final int port;
    private static int counter;

    public NetworkServer(int port) {
        this.port = port;
        counter = 0;
        new ServerStorage();
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

    private static class ServerThread extends Thread {

        private final Socket cliSocket;

        public ServerThread (Socket cliSocket) {
            this.cliSocket = cliSocket;
        }

        public void run() {
            String clientAddress = cliSocket.getInetAddress().getHostAddress();
            System.out.println("Connection request received (" + clientAddress + ")");

            try {
                ObjectInputStream input = new ObjectInputStream(cliSocket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(cliSocket.getOutputStream());

                ServerConnection connection = new ServerConnection(input, output, clientAddress);

                System.out.print("Validating device ID... ");
                boolean validID = connection.validateDevID();

                // FIXME Enable when client app is finished and
                //  don't forget to update size in device_info.csv
//                System.out.print("Validating device info...");
//                boolean validInfo = connection.validateConnection();

                if (validID /*&& validInfo*/) {
                    System.out.println("Client connected (" + clientAddress + ")");
                    System.out.println("Active connections: " + ++counter);
                    connection.handleRequests();
                    System.out.println("Active connections: " + --counter);

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
