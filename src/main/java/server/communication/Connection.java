package server.communication;

import common.Codes;
import common.Message;
import common.security.CommonUtils;
import server.ServerLogger;
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
    private User devUser;           //The user of this connection
    private Device device;          //The device of this connection

    /**
     * Constructs a new {@code Connection}.
     *
     * @param input the {@link ObjectInputStream} for this connection
     * @param output the {@link ObjectOutputStream} for this connection
     * @param srvStorage the {@code Storage}
     */
    public Connection(ObjectInputStream input, ObjectOutputStream output, Storage srvStorage) {
        this.srvStorage = srvStorage;
        this.input = input;
        this.output = output;
        this.devUser = null;
        this.device = null;
    }

    /**
     * Authenticates the {@code User} of this connection.
     */
    public boolean userAuthentication(String apiKey) {
        try {
            String userId = (String) input.readObject();
            User user = srvStorage.getUser(userId);
            String res = user == null ?
                    Codes.NEWUSER.toString() : Codes.FOUNDUSER.toString();
            long generated = new SecureRandom().nextLong();
            output.writeObject(res + ";" + generated);

            Message msg = (Message) input.readObject();
            long received = Long.parseLong((String) msg.getSignedObject().getObject());
            PublicKey pubKey = user == null ?
                    msg.getCertificate().getPublicKey()
                    : SecurityUtils.getUserPubKey(new File(user.certificate()));

            boolean verified = SecurityUtils.verifySignature(pubKey, msg.getSignedObject());
            if (generated == received && verified) {
                if (user == null) {
                    output.writeObject(Codes.OKNEWUSER.toString());
                    if (!authentication2FA(apiKey, userId)) return false;
                    String keyPath = "server-files/users_pub_keys/" + userId + ".cer";
                    File pubKeyFile = new File(keyPath);
                    SecurityUtils.savePublicKeyToFile(msg.getCertificate().getPublicKey(), pubKeyFile);
                    devUser = new User(userId, keyPath);
                    srvStorage.saveUser(this.devUser);
                } else {
                    output.writeObject(Codes.OKUSER.toString());
                    if (!authentication2FA(apiKey, userId)) return false;
                    devUser = srvStorage.getUser(userId);
                }
                return true;
            } else {
                output.writeObject(Codes.NOK.toString());
                return false;
            }
        } catch (Exception e) {
            ServerLogger.logError("Error on authentication process");
            return false;
        }
    }

    /**
     * Handles the second mechanism of the 2FA authentication. In this step the
     * server sends a random code to the client by email, which the client then
     * has to insert in order to be authenticated.
     *
     * @param apiKey the apiKey 
     * @param userId the user id
     *               
     * @return true if this step was successful, false otherwise
     * @throws IOException Any of the usual Input/Output related exceptions.
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     */
    private boolean authentication2FA(String apiKey, String userId)
            throws IOException, ClassNotFoundException {
        SecureRandom secureRandom = new SecureRandom();
        long fiveDigitCode = secureRandom.nextInt(90000) + 10000;
        SecurityUtils.send2FACode(String.valueOf(fiveDigitCode), userId, apiKey);
        String codeStr = (String) input.readObject();
        try {
            int code = Integer.parseInt(codeStr);
            if (code != fiveDigitCode) {
                output.writeObject(Codes.NOK.toString());
                return false;
            }
        } catch (NumberFormatException e) {
            output.writeObject(Codes.NOK.toString());
            return false;
        }
        output.writeObject(Codes.OK2FA.toString());
        return true;
    }

    /**
     * Validates the {@code Device} id of this connection,
     * and tests this client program to see if it is valid.
     *
     * @return true if validated, false otherwise
     */
    public boolean validateDevice() {
        try {
            int devId = Integer.parseInt((String) input.readObject());
            this.device = new Device(devUser.name(), devId);
            Device exists = srvStorage.getDevice(this.device);
            if (exists != null && exists.isConnected()) {
                output.writeObject(Codes.NOKDEVID.toString());
                return false;
            } else if (exists == null) {
                srvStorage.saveDevice(this.device);
            } else {
                this.device = exists;
            }
            output.writeObject(Codes.OKDEVID.toString());

            // Remote attestation
            long nonce = new SecureRandom().nextLong();
            output.writeObject(nonce);
            String[] copyInfo = srvStorage.getCopyInfo();
            byte[] server = CommonUtils.calculateHashWithNonce(new File(copyInfo[1]), nonce);
            String name = (String) input.readObject();
            byte[] client = (byte[]) input.readObject();
            if(name.equals(copyInfo[0]) && CommonUtils.compareHashes(client, server)) {
                this.device.setConnected(true);
                output.writeObject(Codes.OKTESTED.toString());
                return true;
            } else {
                output.writeObject(Codes.NOKTESTED.toString());
            }
        } catch (Exception e) {
            ServerLogger.logError("Error during device validation");
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
                String[] parsedMsg = msg.split(";");

                String command = parsedMsg[0];
                System.out.println("Received " + command + " request from " + device);
                switch (command) {
                    case "CREATE" -> handleCREATE(parsedMsg[1]);
                    case "ADD" -> handleADD(parsedMsg[1], parsedMsg[2]);
                    case "RD" -> handleRD(parsedMsg[1]);
                    case "MYDOMAINS" -> handleMYDOMAINS();
                    case "ET" -> handleET();
                    case "EI" -> handleEI();
                    case "RT" -> handleRT(parsedMsg[1]);
                    case "RI" -> handleRI(parsedMsg[1]);
                    default -> output.writeObject(Codes.NOK.toString());
                }
            }
        } catch (Exception e) {
            // Terminate this connection
            this.device.setConnected(false);
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
        if (result.equals(Codes.OK.toString())) {
            ServerLogger.logInfo("Domain " + d + " created");
        } else {
            ServerLogger.logWarning("Domain " + d + " not created");
        }
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
            Domain domain = srvStorage.getDomain(d);
            String res = Codes.OK.toString();
            if (domain == null) res = Codes.NODM.toString();
            else if (user == null) res = Codes.NOUSER.toString();
            else if (!domain.getOwner().equals(devUser)) {
                res = Codes.NOPERM.toString();
            }
            output.writeObject(res);

            if (res.equals(Codes.OK.toString())) {
                int size = input.readInt();
                String parent = "server-files/domain_keys/" + d;
                File domainDir = new File(parent);
                if (!domainDir.exists()) domainDir.mkdirs();
                String path = parent + "/" + u + ".key.cif";
                if (receiveFile(path, size)) {
                    ServerLogger.logInfo("User key received");
                    res = srvStorage.addUserToDomain(user, domain);
                    output.writeObject(res);
                } else {
                    ServerLogger.logWarning("Unable to receive user key");
                    output.writeObject(Codes.NOK.toString());
                }
            }

            if (res.equals(Codes.OK.toString())) {
                ServerLogger.logInfo( "Added " + u + " to domain " + d);
            } else {
                ServerLogger.logWarning("Unable to add " + u + " to domain " + d);
            }
        } catch (Exception e) {
            System.out.println("Error when trying to add user");
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
        if (result.equals(Codes.OK.toString())) {
            ServerLogger.logInfo("Registered " + device + " to domain " + d);
        } else {
            ServerLogger.logWarning("Unable to register " + device + " to domain " + d);
        }
    }

    /**
     * Handles the command MYDOMAINS
     *
     * @throws IOException if an error occurred during the
     *          communication between client and server
     */
    private void handleMYDOMAINS() throws IOException {
        List<Domain> domains = srvStorage.getDeviceDomains(device);
        if (!domains.isEmpty()) {
            output.writeObject(Codes.OK.toString());
            StringBuilder sb = new StringBuilder("Domains:\n");
            for (Domain domain : domains)
                sb.append(domain.toString()).append("\n");
            output.writeObject(sb.toString());
            ServerLogger.logInfo("Sent domains from " + device);
        } else {
            output.writeObject(Codes.NOK.toString());
            ServerLogger.logWarning("Device " + device + " not registered");
        }
    }

    /**
     * Handles the command ET
     *
     * @throws IOException if an error occurred when writing to the server
     *         files, or during the communication between client and server
     * @see Codes
     */
    private void handleET() throws IOException {
        try {
            List<Domain> domains = srvStorage.getDeviceDomains(device);
            if (domains.isEmpty()) {
                ServerLogger.logWarning("Device " + device + " not registered");
                output.writeObject(Codes.NRD.toString());
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Domain d : domains) sb.append(d.getName()).append(";");
            output.writeObject(sb.toString()); // Send domains
            input.readObject(); // Receive confirmation of receiving the domains

            for (Domain d : domains) {
                String keyPath = "server-files/domain_keys/" + d.getName()
                        + "/" + devUser.name() + ".key.cif";
                File keyFile = new File(keyPath);
                if (!keyFile.exists()) {
                    ServerLogger.logError("Key not found");
                    output.writeObject(Codes.NOK.toString());
                    return;
                }
                output.writeInt((int) keyFile.length()); // Send key size
                sendFile(keyPath, (int) keyFile.length()); // Send key

                // Receive and save encrypted temperature
                String encTemp = (String) this.input.readObject();
                String res = srvStorage.saveTemperature(device, encTemp, d);
                if(res.equals(Codes.OK.toString())) {
                    output.writeObject(Codes.OK.toString());
                } else {
                    output.writeObject(Codes.NOK.toString());
                }
            }
            // Receive final confirmation
            String res = (String) input.readObject();
            if (res.equals(Codes.OK.toString())) {
                ServerLogger.logInfo("Temperature received");
                output.writeObject(Codes.OK.toString());
            } else {
                ServerLogger.logWarning("Unable to receive temperature");
                output.writeObject(Codes.NOK.toString());
            }
        } catch (Exception e) {
            ServerLogger.logError("Error when trying to receive temperature");
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
                ServerLogger.logWarning("Device " + device + " not registered");
                output.writeObject(Codes.NRD.toString());
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Domain d : domains) sb.append(d.getName()).append(";");
            output.writeObject(sb.toString()); // Send domains
            input.readObject(); // Receive confirmation of receiving the domains

            for (Domain d : domains) { // Receive one image per domain
                String keyPath = "server-files/domain_keys/" + d.getName()
                        + "/" + devUser.name() + ".key.cif";
                File keyFile = new File(keyPath);
                if (!keyFile.exists()) {
                    ServerLogger.logError("Key not found");
                    output.writeObject(Codes.NOK.toString());
                    return;
                }
                output.writeInt((int) keyFile.length()); // Send key size
                sendFile(keyPath, (int) keyFile.length()); // Send key

                int size = input.readInt(); // Receive image size
                String imagePath = "server-files/images/" + device.getUser()
                        + "_" + device.getId() + "_" + d.getName() + ".jpg.cif";
                receiveFile(imagePath, size); // Receive image
                output.writeObject(Codes.OK.toString()); // Send confirmation
            }
            // Receive final confirmation
            String res = (String) input.readObject();
            if (res.equals(Codes.OK.toString())) {
                ServerLogger.logInfo("Image received");
                output.writeObject(Codes.OK.toString());
            } else {
                ServerLogger.logWarning("Unable to receive image");
                output.writeObject(Codes.NOK.toString());
            }
        } catch (Exception e) {
            ServerLogger.logError("Error when trying to receive image");
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
            ServerLogger.logWarning("Domain " + d + " does not exist");
            output.writeObject(Codes.NODM.toString());
        } else if (!domain.getUsers().contains(devUser)) {
            ServerLogger.logWarning("User does not have permission");
            output.writeObject(Codes.NOPERM.toString());
        } else {
            String path = srvStorage.getDomainTemperatures(domain);
            if (path != null) {
                String keyPath = "server-files/domain_keys/" + domain.getName()
                        + "/" + devUser.name() + ".key.cif";
                File keyFile = new File(keyPath);
                if (!keyFile.exists()) { // Find domain key
                    ServerLogger.logError("Key not found");
                    output.writeObject(Codes.NOK.toString());
                    return;
                }
                output.writeObject(Codes.OK.toString());

                output.writeInt((int) keyFile.length()); // Send the key size
                sendFile(keyPath, (int) keyFile.length()); // Send the key

                int size = (int) new File(path).length();
                output.writeInt(size);
                if (sendFile(path, size)) { // Send the temperatures file
                    ServerLogger.logInfo("Temperatures from domain " + d + " sent successfully");
                } else {
                    ServerLogger.logWarning("Failed to send temperatures from domain " + d);
                }
            }
            else {
                ServerLogger.logWarning("No data found in domain " + d);
                output.writeObject(Codes.NODATA.toString());
            }
        }
    }

    /**
     * Handles the command RI
     *
     * @param dev the {@code Device}
     * @throws IOException if an error occurred when sending the image,
     *         or during the communication between client and server
     * @see #sendFile(String, int)
     * @see Codes
     */
    private void handleRI(String dev) throws IOException {
        try {
            String user = dev.split(":")[0];
            int id = Integer.parseInt(dev.split(":")[1]);
            Device device = srvStorage.getDevice(new Device(user, id));
            if (device == null) {
                ServerLogger.logWarning("Device " + user + id + " not found");
                output.writeObject(Codes.NOID.toString());
            } else if (!srvStorage.hasPerm(devUser, device)) {
                ServerLogger.logWarning("User does not have permission");
                output.writeObject(Codes.NOPERM.toString());
            } else {
                List<Domain> domains = srvStorage.getDeviceDomains(device);
                for (Domain d : domains) {
                    if (d.getUsers().contains(devUser)) {
                        File domainKeyEnc = new File("server-files/domain_keys/" 
                                + d.getName() + "/" + devUser.name() + ".key.cif");
                        if (domainKeyEnc.exists()) { // Domain key
                            File imageEnc = new File("server-files/images/"
                                    + device.getUser() + "_" + device.getId()
                                    + "_" + d.getName() + ".jpg.cif");
                            if (imageEnc.exists()) { // Image encrypted
                                output.writeObject(Codes.OK.toString());

                                output.writeObject(d.getName());
                                output.writeInt((int) domainKeyEnc.length());
                                sendFile(domainKeyEnc.getPath(), (int) domainKeyEnc.length());

                                input.readObject(); // Receive confirmation
                                output.writeInt((int) imageEnc.length());
                                sendFile(imageEnc.getPath(), (int) imageEnc.length());

                                input.readObject(); // Receive confirmation
                                output.writeObject(Codes.OK.toString());
                                ServerLogger.logInfo("Image from " + device + " sent successfully");
                                return;
                            }
                        }
                    }
                }
                ServerLogger.logWarning("No data found for " + device);
                output.writeObject(Codes.NODATA.toString());
            }
        } catch (Exception e) {
            ServerLogger.logError("Error when trying to send image");
            output.writeObject(Codes.NOK.toString());
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

    /**
     * Returns the {@code Device} of this {@code Connection}
     *
     * @return the {@code Device} of this {@code Connection}
     */
    public Device getDevice() {
        return device;
    }

}
