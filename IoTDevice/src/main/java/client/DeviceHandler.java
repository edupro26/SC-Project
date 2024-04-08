package client;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * A handler used by the {@link IoTDevice} when communicating
 * with the {@code IoTServer}.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see IoTDevice
 */
public class DeviceHandler {

    /**
     * Response codes received by the {@code IoTServer}
     */
    private static final String OK = "OK";
    private static final String NODM = "NODM";
    private static final String NOID = "NOID";
    private static final String NOUSER = "NOUSER";
    private static final String NOPERM = "NOPERM";
    private static final String NODATA = "NODATA";
    private static final String NOK = "NOK";

    /**
     * Folder used as an output for files sent by the {@code IoTServer}
     */
    private static final String SERVER_OUT = "server-output/";

    /**
     * Communication channels
     */
    private ObjectOutputStream output;
    private ObjectInputStream input;

    /**
     * DeviceHandler attributes
     */
    private final String address;       // the ip address of the client
    private final int port;             // the server port
    private SSLSocket socket;              // the client socket

    /**
     * Constructs a new {@code DeviceHandler}.
     *
     * @param address the ip address of the {@code IoTDevice}
     * @param port the port of the {@code IoTServer}
     * @requires {@code address != null && port != null}
     */
    protected DeviceHandler(String address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * Connects opens a {@link Socket} to the {@code IoTServer}
     * and its input and output streams
     */
    protected void connect() {
        try {
            SocketFactory sf = SSLSocketFactory.getDefault();
            socket = (SSLSocket) sf.createSocket(address, port);
            System.out.println("Connected to server: " + address + ":" + port);

            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Closes the {@link Socket} connection to the IoTServer and its input and output streams
     */
    protected void disconnect() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Send a request to the {@code IoTServer} and returns the corresponding response.
     *
     * @param msg the request to the {@code IoTServer}
     * @return server response or null if an error occurred
     * @requires {@code msg != null}
     */
    protected String sendReceive(String msg) {
        try {
            this.output.writeObject(msg);

            return (String) this.input.readObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Sends a CREATE request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args name of the domain to create
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveCREATE(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: CREATE <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK -> System.out.println("Response: "
                    + OK + " # Domain created successfully");
            case NOK -> System.out.println("Response: " + res
                    + " # Domain already exists");
            default -> System.out.println("Response: NOK # Error creating domain");
        }
    }

    /**
     * Sends an ADD request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args user to be added and the domain
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveADD(String[] args, String command) {
        if (args.length != 2) {
            System.out.println("Usage: ADD <user1> <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK -> System.out.println("Response: "
                    + OK + " # User added successfully");
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOUSER -> System.out.println("Response: " + res
                    + " # User does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error adding user");
        }
    }

    /**
     * Sends an RD request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args domain in which the device will be registered
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveRD(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RD <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK -> System.out.println("Response: "
                    + OK + " # Device registered successfully");
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error registering device");
        }
    }

    /**
     * Sends a MYDOMAINS request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args the args of the command. For this command
     *              {@code args} is supposed to be null
     * @param command the command in a string format
     */
    protected void sendReceiveMYDOMAINS(String[] args, String command) {
        if (args.length != 0) {
            System.out.println("Usage: MYDOMAINS");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK)) {
            System.out.println("Response: " + OK + " # Printing domains");
            try {
                System.out.println((String) input.readObject());
            } catch (Exception e) {
                System.out.println("Response: NOK # Error printing domains");
            }
        } else {
            System.out.println("Response: " + NOK + " # Device not registered");
        }
    }

    /**
     * Sends an ET request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args the temperature to send
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveET(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: ET <float>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK)) {
            System.out.println("Response: " + OK +
                    " # Temperature sent successfully");
        } else {
            System.out.println("Response: " + res + " # Error sending temperature");
        }
    }

    /**
     * Sends an EI request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args the path for the image to send
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveEI(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: EI <filename.jpg>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        try {
            Path imagePath = Paths.get(args[0]);
            if (Files.exists(imagePath)) {
                output.writeObject(msg);
                int size = (int) new File(args[0]).length();
                output.writeInt(size);
                sendImage(args[0], size);
                String res = (String) input.readObject();
                if (res.equals(OK)) {
                    System.out.println("Response: " + OK + " # Image sent successfully");
                } else {
                    System.out.println("Response: " + res + " # Error sending image");
                }
            } else {
                System.out.println("Response: NOK # Image does not exist");
            }
        } catch (Exception e) {
            System.out.println("Response: NOK # Error sending image");
        }
    }

    /**
     * Sends an RT request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args the domain to receive data from
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveRT(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RT <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        String name = SERVER_OUT + args[0] + ".txt";
        switch (res) {
            case OK -> {
                int received = receiveFile(name);
                System.out.println("Response: " + res + ", " + received
                        + " (long), followed by " + received + " bytes of data");
            }
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            case NODATA -> System.out.println("Response: " + res
                    + " # No data found in this domain");
            default -> System.out.println("Response: NOK # Error getting temperatures");
        }
    }

    /**
     * Sends an RI request to the {@code IoTServer}
     * and handles the response.
     *
     * @param args the device to receive data from
     * @param command the command in a string format
     * @requires {@code args != null && command != null}
     */
    protected void sendReceiveRI(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RI <user-id>:<dev_id>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        String[] temp = args[0].split(":");
        String name = SERVER_OUT + temp[0] + "_" + temp[1] + ".jpg";
        switch (res) {
            case OK -> {
                int received = receiveFile(name);
                System.out.println("Response: " + res + ", " + received
                        + " (long), followed by " + received + " bytes of data");
            }
            case NODATA -> System.out.println("Response: " + res
                    + " # No image found for this device");
            case NOID -> System.out.println("Response: " + res
                    + " # No device found with this id");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error getting image");
        }
    }

    /**
     * Sends a file to the {@code IoTServer}.
     *
     * @param filePath the path of the file to be sent
     * @requires {@code filePath != null}
     */
    private void sendImage(String filePath, int size) {
        try {
            File image = new File(filePath);
            FileInputStream in = new FileInputStream(image);
            BufferedInputStream bis = new BufferedInputStream(in);
            byte[] buffer = new byte[8192];
            int bytesLeft = size;
            while (bytesLeft > 0) {
                int bytesRead = bis.read(buffer);
                output.write(buffer, 0, bytesRead);
                bytesLeft -= bytesRead;
            }
            output.flush();
            bis.close();
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Receives a file from the {@code IoTServer} and saves it
     * in the output folder.
     *
     * @param filePath the path where the file will be saved
     * @return file size if the file was received, -1 otherwise
     * @requires {@code filePath != null}
     */
    private int receiveFile(String filePath) {
        File outputFolder = new File(SERVER_OUT);
        if (!outputFolder.isDirectory()) outputFolder.mkdir();
        try {
            int size = input.readInt();
            FileOutputStream out = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            byte[] buffer = new byte[8192];
            int bytesLeft = size;
            while (bytesLeft > 0) {
                int bytesRead = input.read(buffer);
                bos.write(buffer, 0, bytesRead);
                bytesLeft -= bytesRead;
            }
            bos.flush();
            bos.close();
            out.close();
            return size;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    /**
     * Formats the command chosen by the user input, so it can
     * be sent to the {@code IoTServer}.
     *
     * @param command the command
     * @param args the command arguments
     * @return a string ready to be sent to the {@code IoTServer}
     */
    private String parseCommandToSend(String command, String[] args) {
        StringBuilder sb = new StringBuilder(command);
        for (String arg : args) {
            sb.append(";").append(arg);
        }

        return sb.toString();
    }

}
