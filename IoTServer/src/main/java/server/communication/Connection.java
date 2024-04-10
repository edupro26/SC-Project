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
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

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

            client.Message in = (client.Message) input.readObject();
            SecureRandom secureRandom = new SecureRandom();
            long nonce = secureRandom.nextLong();

            if (srvStorage.getUser(in.getUserId()) == null) {
                //TODO adicionar os users nos ficheiros

                client.Message mes = sendReceiveMessage(new client.Message(nonce, false));

                assert mes != null;
                SignedObject signedNonce = mes.getSignedObject();
                long clientNonce = mes.getNonce();
                PublicKey clientPK = mes.getCertificate().getPublicKey();

                String algorithm = signedNonce.getAlgorithm();

                Signature signature = Signature.getInstance(algorithm);

                signature.initVerify(clientPK);

                boolean verified = signedNonce.verify(clientPK, signature);

                if(clientNonce == nonce && verified){ // && verified
                    output.writeObject(new client.Message("OK"));
                    System.out.println("Sucess");
                }
                else {
                    output.writeObject(new client.Message("ERROR"));
                }
                //decripta o nonce com a chave publica recebida na mensagem

            }
            else {
                //mandar só o nonce
                client.Message mes = sendReceiveMessage(new client.Message(nonce, false));
                //TODO ir ao ficheiro buscar a chave publica e
            }
        } catch (Exception e) {
            System.out.println("Error receiving user password!");
        }
    }
    protected client.Message sendReceiveMessage(client.Message msg) {
        try {
            this.output.writeObject(msg);

            return (client.Message) this.input.readObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
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
                    case "MYDOMAINS" -> handleMYDOMAINS();
                    case "ET" -> handleET(parsedMsg[1]);
                    case "EI" -> handleEI();
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
        try {
            User user = srvStorage.getUser(u);
            if (user == null) {
                output.writeObject(Codes.NOK.toString());
                return;
            }
            Domain domain = srvStorage.getDomain(d);
            if (domain == null) {
                output.writeObject(Codes.NODM.toString());
                return;
            }
            if (!domain.getOwner().equals(devUser)) {
                output.writeObject(Codes.NOPERM.toString());
                return;
            }

            output.writeObject(Codes.OK.toString());

            String hasPK = (String) input.readObject();
            if (hasPK.equals("NO_PK")) {
                String path = "server-files/users_pub_keys/" + u + ".cer";
                File file = new File(path);
                if (file.isFile() && file.exists()) {
                    output.writeObject("SENDING_KEY");
                    input.readObject();
                    output.writeObject(Codes.OK.toString());
                    output.writeInt((int) file.length());
                    sendFile(path, (int) file.length());
                    System.out.println("Success: File send successfully!");
                } else {
                    output.writeObject("NOK");
                    return;
                }
            } else {
                output.writeObject("WAITING_KEY");
            }

            int size = input.readInt();
            String path = "server-files/domain_keys" + d + "." + u + ".key.cif";

            if (receiveFile(path, size)) {
                System.out.println("Success: Key received!");
                output.writeObject(Codes.OK.toString());
            } else {
                System.out.println("Error: Unable to receive key!");
                output.writeObject(Codes.NOK.toString());
                return;
            }

            String result = srvStorage.addUserToDomain(this.devUser, user, domain);
            output.writeObject(result);
            result = result.equals(Codes.OK.toString()) ?
                    "Success: User added!" : "Error: Unable to add user!";
            System.out.println(result);
        } catch (Exception e) {
            output.writeObject(Codes.NOK.toString());
        }
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
     * Handles the command MYDOMAINS
     *
     * @throws IOException if an error occurred during the
     *          communication between client and server
     */
    private void handleMYDOMAINS() throws IOException {
        List<Domain> domains = srvStorage.getDeviceDomains(this.device);
        if (!domains.isEmpty()) {
            output.writeObject(Codes.OK.toString());
            StringBuilder sb = new StringBuilder("Domains:\n");
            for (Domain domain : domains)
                sb.append(domain.toString()).append("\n");
            output.writeObject(sb.toString());
            System.out.println("Success: Domains sent!");
        } else {
            output.writeObject(Codes.NOK.toString());
            System.out.println("Error: Device not registered!");
        }
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
            Float temperature = Float.parseFloat(t);
            String res = srvStorage.updateLastTemp(device, temperature);
            String result = res.equals(Codes.OK.toString()) ?
                    "Success: Temperature received!" : "Error: Unable to receive temperature!";
            System.out.println(result);
            output.writeObject(res);
        } catch (Exception e) {
            System.out.println("Error: Unable to receive temperature!");
            output.writeObject(Codes.NOK.toString());
        }
    }

    /**
     * Handles the command EI
     *
     * @throws IOException if an error occurred when receiving the image,
     *         or during the communication between client and server
     * @see #receiveFile(String, int)
     * @see Codes
     */
    private void handleEI() throws IOException {
        int size = input.readInt();
        String name = device.getUser() + "_" + device.getId() + ".jpg";
        String path = "server-files/images/" + name;
        if (receiveFile(path, size)) {
            System.out.println("Success: Image received!");
            output.writeObject(Codes.OK.toString());
        }
        else {
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
     * @see #sendFile(String, int)
     * @see Codes
     */
    private void handleRT(String d) throws IOException {
        Domain domain = srvStorage.getDomain(d);
        if (domain == null) {
            System.out.println("Error: Domain does not exist!");
            output.writeObject(Codes.NODM.toString());
        } else if (!domain.getUsers().contains(devUser) &&
                !domain.getOwner().equals(devUser)) {
            System.out.println("Error: User does not have permissions!");
            output.writeObject(Codes.NOPERM.toString());
        } else {
            String path = srvStorage.domainTemperaturesFile(domain);
            if (path != null) {
                output.writeObject(Codes.OK.toString());
                int size = (int) new File(path).length();
                output.writeInt(size);
                String result = sendFile(path, size) ?
                        "Success: Temperatures sent successfully!"
                        : "Error: Failed to send temperatures!";
                System.out.println(result);
            }
            else {
                System.out.println("Error: No data found in this domain!");
                output.writeObject(Codes.NODATA.toString());
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
     * @see #sendFile(String, int)
     * @see Codes
     */
    private void handleRI(String user, int id) throws IOException {
        Device device = srvStorage.getDevice(new Device(user, id));
        if (device == null) {
            System.out.println("Error: Device id not found!");
            output.writeObject(Codes.NOID.toString());
        } else if (!srvStorage.hasPerm(devUser, device)) {
            System.out.println("Error: User does not have permissions!");
            output.writeObject(Codes.NOPERM.toString());
        } else {
            String name = device.getUser() + "_" + device.getId() + ".jpg";
            String path = "server-files/images/" + name;
            File image = new File(path);
            if (image.isFile() && image.exists()){
                output.writeObject(Codes.OK.toString());
                int size = (int) image.length();
                output.writeInt(size);
                String result = sendFile(path, size) ?
                        "Success: Image sent successfully!" : "Error: Failed to send image!";
                System.out.println(result);
            } else {
                System.out.println("Error: No data found for this device!");
                output.writeObject(Codes.NODATA.toString());
            }
        }
    }

    /**
     * Receives a file and stores it with the given name and path.
     *
     * @param size the size in bytes of the file to receive
     * @param path the path to store the file
     *
     */
    private boolean receiveFile(String path, int size) {
        File file = new File(path);
        try {
            FileOutputStream out = new FileOutputStream(file);
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
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Sends a file to the {@code IoTDevice}.
     *
     * @param path the path of the file to send
     * @param size the size in bytes of the file to send
     */
    private boolean sendFile(String path, int size) {
        File file = new File(path);
        try {
            FileInputStream in = new FileInputStream(file);
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
            return false;
        }
        return true;
    }

}
