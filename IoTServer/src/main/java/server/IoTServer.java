package server;

import server.communication.Connection;
import server.persistence.Storage;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
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
        int port = 12345;
        String passwordCipher = null;
        String keystore = null;
        String passwordKeystore = null;
        String apiKey = null;

        /*
        Check if the arguments are valid
        If 5 arguments are given, the first one is the port and the rest are the remaining arguments
        If 4 arguments are given, a default port is used. All 4 arguments are the required arguments
        If the arguments are invalid, the program exits with an error message
        */
        if (args.length == 5) {
            port = Integer.parseInt(args[0]);
            passwordCipher = args[1];
            keystore = args[2];
            passwordKeystore = args[3];
            apiKey = args[4];
        } else if (args.length == 4) {
            passwordCipher = args[0];
            keystore = args[1];
            passwordKeystore = args[2];
            apiKey = args[3];
        } else {
            System.out.println("Usage: IoTServer <port> <password-cifra> <keystore> <password-keystore> <2FA-APIKey>");
            System.exit(1);
        }

        if (passwordCipher == null || keystore == null || passwordKeystore == null || apiKey == null) {
            System.out.println("Usage: IoTServer <port> <password-cifra> <keystore> <password-keystore> <2FA-APIKey>");
            System.exit(1);
        }

        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", passwordKeystore);

        System.out.println("Server started on port " + port);
        SSLServerSocket srvSocket = null;

        try {
            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            srvSocket = (SSLServerSocket) ssf.createServerSocket(port);
            Storage srvStorage = new Storage(passwordCipher);
            System.out.println("Waiting for clients...");
            while (true) {
                new ServerThread(srvSocket.accept(), srvStorage, apiKey).start();
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
        private final String apiKey;            // the API key for the 2FA

        /**
         * Initiates a new {@code ServerThread}.
         *
         * @param cliSocket the {@code Socket} of the client
         * @param srvStorage the {@code Storage} of this IoTServer
         * @param apiKey the API key for the 2FA
         * @requires {@code cliSocket != null && srvStorage != null && apiKey != null}
         */
        private ServerThread (Socket cliSocket, Storage srvStorage, String apiKey) {
            this.cliSocket = cliSocket;
            this.srvStorage = srvStorage;
            this.apiKey = apiKey;
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
                boolean auth = connection.userAuthentication(this.apiKey);

                if (!auth) {
                    System.out.println("Client not authenticated (" + clientAddress + ")");
                    output.close();
                    input.close();
                    cliSocket.close();
                    return;
                }

                System.out.println("Validating device ID... ");
                boolean validID = connection.validateDevID();


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
