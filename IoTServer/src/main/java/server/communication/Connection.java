package server.communication;

import server.components.User;
import server.components.Device;
import server.components.Domain;
import server.persistence.Storage;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Represents a {@code IoTDevice} connection to the {@code IoTServer}.
 * This class is responsible to handle communication between the client
 * program and the server program.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see Storage
 * @see Domain
 * @see Device
 * @see User
 */
public final class Connection {

    /**
     * Storage of the {@code IoTServer}
     */
    private final Storage srvStorage;

    /**
     * Communication channels
     */
    private final ObjectInputStream input;
    private final ObjectOutputStream output;

    /**
     * Connection attributes
     */
    private final String clientIP;  //The client IP
    private User devUser;           //The user of this connection
    private Device device;          //The device of this connection

    /**
     * Constructs a new {@code Connection}.
     *
     * @param input the {@link ObjectInputStream} for this connection
     * @param output the {@link ObjectOutputStream} for this connection
     * @param srvStorage the {@code Storage}
     * @param clientIP the client IP address
     */
    public Connection(ObjectInputStream input, ObjectOutputStream output, Storage srvStorage, String clientIP) {
        this.srvStorage = srvStorage;
        this.input = input;
        this.output = output;
        this.clientIP = clientIP;
        this.devUser = null;
        this.device = null;
        userAuthentication();
    }

    /**
     * Authenticates the {@code User} of this connection.
     */
    private void userAuthentication() {
        try {
            String in = (String) input.readObject();
            String[] userParts = in.split(",");
            User logIn = new User(userParts[0], userParts[1]);

            devUser = srvStorage.getUser(logIn.getName());
            if (devUser == null) {
                srvStorage.saveUser(logIn);
                devUser = logIn;
                output.writeObject(Codes.OKNEWUSER.toString());
            }
            else {
                while (!logIn.getPassword().equals(devUser.getPassword())) {
                    output.writeObject(Codes.WRONGPWD.toString());
                    String password = ((String) input.readObject()).split(",")[1];
                    logIn.setPassword(password);
                }
                output.writeObject(Codes.OKUSER.toString());
            }
            this.device = new Device(devUser.getName(), -1);
        } catch (Exception e) {
            System.out.println("Error receiving user password!");
        }
    }

    /**
     * Validates the {@code Device} id of this connection.
     *
     * @return true if validated, false otherwise
     */
    public boolean validateDevID() {
        try {
            while (device.getId() < 0) {
                String msg = (String) input.readObject();
                int id = Integer.parseInt(msg);
                if (id >= 0) {
                    device.setId(id);
                    Device exits = srvStorage.getDevice(device);
                    if (exits != null) {
                        if (!exits.isConnected()) {
                            device = exits;
                            device.setConnected(true);
                            output.writeObject(Codes.OKDEVID.toString());
                            System.out.println("Device ID validated!");
                            return true;
                        }
                    }
                    else {
                        device.setConnected(true);
                        srvStorage.saveDevice(device, new ArrayList<>());
                        output.writeObject(Codes.OKDEVID.toString());
                        System.out.println("Device ID validated!");
                        return true;
                    }
                }
                output.writeObject(Codes.NOKDEVID.toString());
                device.setId(-1);
            }
        } catch (Exception e) {
            System.out.println("Something went wrong!");
        }
        return false;
    }

    /**
     * Validates the client program.
     *
     * @return true if validated, false otherwise
     */
    public boolean validateConnection() {
        try {
            String[] in = ((String) input.readObject()).split(",");
            String name = in[0];
            String size = in[1];
            boolean tested = srvStorage.checkConnectionInfo(name, size);
            if (tested) {
                output.writeObject(Codes.OKTESTED.toString());
                System.out.println("Device info validated!");
                return true;
            }
            else {
                output.writeObject(Codes.NOKTESTED.toString());
                System.out.println("Device info not validated!");
            }
        } catch (Exception e) {
            System.out.println("Something went wrong!");
        }
        return false;
    }

    /**
     * Handles the requests from the {@code IoTDevice}.
     *
     * @see Codes
     */
    public void handleRequests() {
        try {
            while (true) {
                String msg = (String) input.readObject();
                System.out.println("Received: " + msg + " from -> " + clientIP);
                String[] parsedMsg = msg.split(";");
                String command = parsedMsg[0];

                switch (command) {
                    case "CREATE" -> handleCREATE(parsedMsg[1]);
                    case "ADD" -> handleADD(parsedMsg[1], parsedMsg[2]);
                    case "RD" -> handleRD(parsedMsg[1]);
                    case "ET" -> handleET(parsedMsg[1]);
                    case "EI" -> handleEI(parsedMsg[1]);
                    case "RT" -> handleRT(parsedMsg[1]);
                    case "RI" -> {
                        String[] devParts = parsedMsg[1].split(":");
                        if (devParts.length != 2) {
                            output.writeObject(Codes.NOK.toString());
                            System.out.println("Error: Unable to send image!");
                            break;
                        }
                        try {
                            Integer.parseInt(devParts[1]);
                        } catch (NumberFormatException e) {
                            output.writeObject(Codes.NOK.toString());
                            System.out.println("Error: Unable to send image!");
                            break;
                        }
                        handleRI(devParts[0], Integer.parseInt(devParts[1]));
                    }
                    default -> output.writeObject(Codes.NOK.toString());
                }
            }
        } catch (Exception e) {
            this.device.setConnected(false);
            System.out.println("Client disconnected (" + this.clientIP + ")");
        }
    }

    /**
     * Handles the command CREATE
     * @param d the name of the {@code Domain} to create
     * @throws IOException if an error occurred when writing to the server
     *         files, or during the communication between client and server
     * @see Codes
     */
    private void handleCREATE(String d) throws IOException {
        String result = srvStorage.createDomain(d, devUser);
        output.writeObject(result);
        result = result.equals(Codes.OK.toString()) ?
                "Success: Domain created!" : "Error: Domain not created!";
        System.out.println(result);
    }

    /**
     * Handles the command ADD
     *
     * @param u the username of the {@code User} to add
     * @param d the name of the {@code Domain}
     * @throws IOException if an error occurred when writing to the server
     *         files, or during the communication between client and server
     * @see Codes
     */
    private void handleADD(String u, String d) throws IOException {
        User user = srvStorage.getUser(u);
        Domain domain = srvStorage.getDomain(d);
        String result = srvStorage.addUserToDomain(this.devUser, user, domain);
        output.writeObject(result);
        result = result.equals(Codes.OK.toString()) ?
                "Success: User added!" : "Error: Unable to add user!";
        System.out.println(result);
    }

    /**
     * Handles the command RD
     *
     * @param d the name of the {@code Domain}
     * @throws IOException if an error occurred when writing to the server
     *         files, or during the communication between client and server
     * @see Codes
     */
    private void handleRD(String d) throws IOException {
        Domain domain = srvStorage.getDomain(d);
        String result = srvStorage.addDeviceToDomain(domain, device, devUser);
        output.writeObject(result);
        result = result.equals(Codes.OK.toString()) ?
                "Success: Device registered!" : "Error: Unable to register device!";
        System.out.println(result);
    }

    /**
     * Handles the command ET
     *
     * @param t the temperature in string format
     * @throws IOException if an error occurred when writing to the server
     *         files, or during the communication between client and server
     * @see Codes
     */
    private void handleET(String t) throws IOException {
        try {
            device.setLastTemp(Float.parseFloat(t));
            String result = srvStorage.updateLastTemp(device);
            output.writeObject(result);
            result = result.equals(Codes.OK.toString()) ?
                    "Success: Temperature received!" : "Error: Unable to receive temperature!";
            System.out.println(result);
        } catch (Exception e) {
            output.writeObject(Codes.NOK.toString());
            System.out.println("Error: Unable to receive temperature!");
        }
    }

    /**
     * Handles the command EI
     *
     * @param filePath the path of the image
     * @throws IOException if an error occurred when receiving the image,
     *         or during the communication between client and server
     * @see #receiveImage(File)
     * @see Codes
     */
    private void handleEI(String filePath) throws IOException {
        File image = new File(filePath);
        if (image.isFile() && image.exists()) {
            receiveImage(image);
            System.out.println("Success: Image received!");
        } else {
            System.out.println("Error: Unable to receive image!");
            output.writeObject(Codes.NOK.toString());
        }
    }

    /**
     * Handles the command RT
     *
     * @param d the name of the {@code Domain}
     * @throws IOException if an error occurred when sending the file,
     *         or during the communication between client and server
     * @see #sendFile(File)
     * @see Codes
     */
    private void handleRT(String d) throws IOException {
        Domain domain = srvStorage.getDomain(d);
        if (domain == null) {
            output.writeObject(Codes.NODM.toString());
            System.out.println("Error: Domain does not exist!");
        } else if (!domain.getUsers().contains(devUser) &&
                !domain.getOwner().equals(devUser)) {
            output.writeObject(Codes.NOPERM.toString());
            System.out.println("Error: User does not have permissions!");
        } else {
            File file = domain.getDomainTemperatures();
            if (file == null) {
                output.writeObject(Codes.NODATA.toString());
                System.out.println("Error: No data found for this device!");
            }
            else {
                sendFile(file);
            }
        }
    }

    /**
     * Handles the command RI
     *
     * @param user the user of the {@code Device}
     * @param id the id of the {@code Device}
     * @throws IOException if an error occurred when sending the image,
     *         or during the communication between client and server
     * @see #sendFile(File)
     * @see Codes
     */
    private void handleRI(String user, int id) throws IOException {
        Device received = new Device(user, id);
        Device device = srvStorage.getDevice(received);
        if (device == null) {
            output.writeObject(Codes.NOID.toString());
            System.out.println("Error: Device id not found!");
        } else if (!srvStorage.hasPerm(devUser, device)) {
            output.writeObject(Codes.NOPERM.toString());
            System.out.println("Error: User does not have permissions!");
        } else {
            String name = device.getUser() + "_" + device.getId() + ".jpg";
            File image = new File(new File("images"), name);
            if (image.isFile() && image.exists()){
                sendFile(image);
            } else {
                output.writeObject(Codes.NODATA.toString());
                System.out.println("Error: No data found for this device!");
            }
        }
    }

    /**
     * Receives an image sent from the {@code IoTDevice}.
     *
     * @param image the image file
     * @throws IOException if an error occurred when receiving the image,
     *         or during the communication between client and server
     * @see Codes
     */
    private void receiveImage(File image) throws IOException {
        output.writeObject(Codes.OK.toString());
        String imageName = device.getUser() + "_" + device.getId() + ".jpg";
        File file = new File(new File("images"), imageName);
        FileOutputStream out = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(out);
        byte[] buffer = new byte[8192];
        int bytesLeft = (int) image.length();
        while (bytesLeft > 0) {
            int bytesRead = input.read(buffer);
            bos.write(buffer, 0, bytesRead);
            bytesLeft -= bytesRead;
        }
        bos.flush();
        bos.close();
        out.close();
    }

    /**
     * Sends a file to the {@code IoTDevice}.
     *
     * @param file the file to send
     * @throws IOException if an error occurred when sending the file,
     *         or during the communication between client and server
     * @see Codes
     */
    private void sendFile(File file) throws IOException {
        output.writeObject(Codes.OK.toString());
        output.writeInt((int) file.length());
        System.out.println("Success: File send successfully!");
        FileInputStream in = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(in);
        byte[] buffer = new byte[8192];
        int bytesLeft = (int) file.length();
        while (bytesLeft > 0) {
            int bytesRead = bis.read(buffer);
            output.write(buffer, 0, bytesRead);
            bytesLeft -= bytesRead;
        }
        output.flush();
        bis.close();
        in.close();
    }

}