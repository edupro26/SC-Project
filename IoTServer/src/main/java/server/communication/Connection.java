package server.communication;

import common.Message;
import server.components.Device;
import server.components.Domain;
import server.components.User;
import server.persistence.Storage;
import server.security.SecurityUtils;

import java.io.*;
import java.security.PublicKey;
import java.security.SecureRandom;
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
public class Connection {

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
    }

    /**
     * Authenticates the {@code User} of this connection.
     */
    public boolean userAuthentication() {
        try {
            String userId = (String) input.readObject();
            SecureRandom secureRandom = new SecureRandom();
            long nonce = secureRandom.nextLong();

            if (srvStorage.getUser(userId) == null) {

                String newUserRes = Codes.NEWUSER + ";" + nonce;

                output.writeObject(newUserRes);
                Message msg = (Message) input.readObject();


                long clientNonce = Long.parseLong((String) msg.getSignedObject().getObject());

                boolean verified = SecurityUtils.verifySignature(msg.getCertificate().getPublicKey(), msg.getSignedObject());

                if(clientNonce == nonce && verified) {
                    String userPublicKeyPath = "server-files/users_pub_keys/" + userId + ".cer";
                    SecurityUtils.savePublicKeyToFile(msg.getCertificate().getPublicKey(), new File(userPublicKeyPath));

                    // TODO: 2FA Auth

                    this.devUser = new User(userId, userPublicKeyPath);
                    srvStorage.saveUser(this.devUser);
                    output.writeObject(Codes.OKNEWUSER.toString());




                    return true;
                }
                else {
                    output.writeObject(Codes.NOK.toString());
                    return false;
                }


            }
            else {
                String foundUserRes = Codes.FOUNDUSER + ";" + nonce;

                output.writeObject(foundUserRes);
                Message msg = (Message) input.readObject();

                long clientNonce = Long.parseLong((String) msg.getSignedObject().getObject());

                PublicKey userPublicKey = SecurityUtils.readPublicKeyFromFile(new File("server-files/users_pub_keys/" + userId + ".cer"));

                boolean verified = SecurityUtils.verifySignature(userPublicKey, msg.getSignedObject());

                if(clientNonce == nonce && verified) { // && verified
                    output.writeObject(Codes.OKUSER.toString());
                    this.devUser = srvStorage.getUser(userId);
                    return true;
                } else {
                    output.writeObject(Codes.NOK.toString());
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println("Error on auth process!");
            return false;
        }
    }

    /**
     * Validates the {@code Device} id of this connection.
     *
     * @return true if validated, false otherwise
     */
    public boolean validateDevID() {
        try {
            //int devId = Integer.parseInt((String) input.readObject());
            String strDevId = (String) input.readObject();
            int devId = Integer.parseInt(strDevId);
            if (devId < 0) {
                output.writeObject(Codes.NOKDEVID.toString());
                return false;
            }
            this.device = new Device(devUser.getName(), devId);

            Device exists = srvStorage.getDevice(device);
            if (exists != null) {
                if (!exists.isConnected()) {
                    device = exists;
                } else {
                    output.writeObject(Codes.NOKDEVID.toString());
                    return false;
                }
            }
            else {
                srvStorage.saveDevice(device);
            }

            // TODO: Remote attestation

            device.setConnected(true);
            output.writeObject(Codes.OKDEVID.toString());
            System.out.println("Device ID validated!");
            return true;

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
                output.writeObject(Codes.NOUSER.toString());
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
                output.writeObject("NOK");
                return;
                /*
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
                */
            } else {
                output.writeObject("WAITING_KEY");
            }

            int size = input.readInt();
            File domainDir = new File("server-files/domain_keys/" + d);
            if (!domainDir.exists()) {
                domainDir.mkdirs();
            }
            String path = "server-files/domain_keys/" + d + "/" + u + ".key.cif";

            if (receiveFile(path, size)) {
                System.out.println("Success: Key received!");
                output.writeObject(Codes.OK.toString());
            } else {
                System.out.println("Error: Unable to receive key!");
                output.writeObject(Codes.NOK.toString());
                return;
            }

            input.readObject();

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
        try {
            // Check domains device is in
            List<Domain> domains = srvStorage.getDeviceDomains(device);
            if (domains.isEmpty()) {
                System.out.println("Error: Device not registered in any domain!");
                output.writeObject(Codes.NOK.toString());
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Domain domain : domains) {
                sb.append(domain.getName()).append(";");
            }

            // Send domains
            output.writeObject(sb.toString());

            // Receive confirmation of receiving the domains
            input.readObject();

            // Start receiving the images - one image per domain
            for (Domain domain : domains) {
                // Send the key
                String keyPath = "server-files/domain_keys/" + domain.getName() + "/" + devUser.getName() + ".key.cif";
                File keyFile = new File(keyPath);
                if (!keyFile.exists()) {
                    System.out.println("Error: Key not found!");
                    output.writeObject(Codes.NOK.toString());
                    return;
                }
                output.writeInt((int) keyFile.length()); // Send key size
                sendFile(keyPath, (int) keyFile.length()); // Send key

                int size = input.readInt(); // Receive image size
                String imagePath = "server-files/images/" + device.getUser() + "_" + device.getId() + "_" + domain.getName() + ".jpg.cif";

                receiveFile(imagePath, size); // Receive image

                output.writeObject("ONE_IMAGE_RECEIVED"); // Confirm image received

                int paramsSize = input.readInt(); // Receive params size
                String paramsPath = "server-files/images/" + device.getUser() + "_" + device.getId() + "_" + domain.getName() + ".params";
                receiveFile(paramsPath, paramsSize); // Receive params

                output.writeObject("ONE_PARAMS_RECEIVED"); // Confirm params received
            }

            String allImagesReceived = (String) input.readObject(); // Receive confirmation of all images were sent to the server

            if (allImagesReceived.equals("ALL_IMAGES_RECEIVED")) {
                System.out.println("Success: All images received!");
                output.writeObject(Codes.OK.toString());
            } else {
                System.out.println("Error: Unable to receive images!");
                output.writeObject(Codes.NOK.toString());
            }
        } catch (Exception e) {
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
        try {
            Device device = srvStorage.getDevice(new Device(user, id));
            if (device == null) {
                System.out.println("Error: Device id not found!");
                output.writeObject(Codes.NOID.toString());
            } else if (!srvStorage.hasPerm(devUser, device)) {
                System.out.println("Error: User does not have permissions!");
                output.writeObject(Codes.NOPERM.toString());
            } else {
                List<Domain> domainsDevice = srvStorage.getDeviceDomains(device);
                for (Domain d : domainsDevice) {
                    if (d.getUsers().contains(devUser) || d.getOwner().equals(devUser)) {
                        // Domain key
                        File domainKeyEnc = new File("server-files/domain_keys/" + d.getName() + "/" + devUser.getName() + ".key.cif");
                        if (!domainKeyEnc.exists()) continue;

                        // Image encrypted
                        File imageEnc = new File("server-files/images/" + device.getUser() + "_" + device.getId() + "_" + d.getName() + ".jpg.cif");
                        if (!imageEnc.exists()) continue;

                        // Image encryption params
                        File imageEncParams = new File("server-files/images/" + device.getUser() + "_" + device.getId() + "_" + d.getName() + ".params");
                        if (!imageEncParams.exists()) continue;

                        output.writeObject("SENDING_FILES"); // Warn client that server is going to send the files

                        //input.readObject(); // Client is ready to receive the key

                        output.writeObject(d.getName());

                        output.writeInt((int) domainKeyEnc.length());
                        sendFile(domainKeyEnc.getPath(), (int) domainKeyEnc.length());

                        input.readObject(); // Client is ready to receive the image
                        output.writeInt((int) imageEnc.length());
                        sendFile(imageEnc.getPath(), (int) imageEnc.length());

                        input.readObject(); // Client is ready to receive the image encryption params
                        output.writeInt((int) imageEncParams.length());
                        sendFile(imageEncParams.getPath(), (int) imageEncParams.length());

                        input.readObject();
                        output.writeObject("OK");

                        return;
                    }

                }

                output.writeObject(Codes.NODATA.toString());
            }
        } catch (Exception e) {
            output.writeObject(Codes.NOK);
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
