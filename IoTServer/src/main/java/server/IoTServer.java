package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main class of the {@code IoTServer}.This class represents a multithreaded server.
 * This class is responsible for running the main {@link Thread} of the server and
 * for creating a {@link Thread} for each connection from a {@code IoTDevice}.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see Connection
 * @see User
 * @see Device
 * @see Domain
 * @see Storage
 */
public class IoTServer {

    private static int counter;     // Connections counter

    /**
     * This class is not meant to be constructed
     */
    private IoTServer() {}

    /**
     * Main routine of this IoTServer
     *
     * @param args the arguments given when executed
     * @see Socket
     * @see ServerSocket
     * @see ServerThread
     */
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

    /**
     * Private class representing a {@link Thread} of this IoTServer.
     */
    private static class ServerThread extends Thread {

        /**
         * ServerThread attributes
         */
        private final Socket cliSocket;         // the socket of the client
        private final Storage srvStorage;       // the storage of this IoTServer

        /**
         * Initiates a new {@code ServerThread}.
         *
         * @param cliSocket the {@code Socket} of the client
         * @param srvStorage the {@code Storage} of this IoTServer
         * @requires {@code cliSocket != null && srvStorage != null}
         */
        public ServerThread (Socket cliSocket, Storage srvStorage) {
            this.cliSocket = cliSocket;
            this.srvStorage = srvStorage;
        }

        /**
         * Runs this ServerThread
         */
        public void run() {
            String clientAddress = cliSocket.getInetAddress().getHostAddress();
            System.out.println("Connection request received (" + clientAddress + ")");

            try {
                ObjectInputStream input = new ObjectInputStream(cliSocket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(cliSocket.getOutputStream());

                Connection connection = new Connection(input, output, srvStorage, clientAddress);

                System.out.print("Validating device ID... ");
                boolean validID = connection.validateDevID();

                System.out.print("Validating device info...");
                boolean validInfo = connection.validateConnection();

                if (validID && validInfo) {
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
