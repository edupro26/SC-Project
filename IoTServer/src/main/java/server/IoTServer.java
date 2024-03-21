package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class IoTServer {

    private static int counter;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
        System.out.println("Server started on port " + port);

        ServerSocket srvSocket = null;
        try {
            srvSocket = new ServerSocket(port);
            Storage srvStorage = new Storage();
            System.out.println("Waiting for clients...");
            while (true) {
                Socket cliSocket = srvSocket.accept();
                new ServerThread(cliSocket, srvStorage).start();
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
        private final Storage srvStorage;

        public ServerThread (Socket cliSocket, Storage srvStorage) {
            this.cliSocket = cliSocket;
            this.srvStorage = srvStorage;
        }

        public void run() {
            String clientAddress = cliSocket.getInetAddress().getHostAddress();
            System.out.println("Connection request received (" + clientAddress + ")");

            try {
                ObjectInputStream input = new ObjectInputStream(cliSocket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(cliSocket.getOutputStream());

                Connection connection = new Connection(input, output, srvStorage, clientAddress);

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
