package server.communication;

import common.Codes;
import common.Message;
import common.security.CommonUtils;
import server.components.Device;
import server.components.Domain;
import server.components.User;
import server.persistence.Storage;
import server.security.SecurityUtils;

import java.io.*;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

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


    private static final String EMAIL_REGEX = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

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
            String email = (String) input.readObject();
            Pattern emailPattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
            if (!emailPattern.matcher(email).matches()) {   // Validate email address
                output.writeObject(Codes.NOK.toString());
                System.out.println("Received invalid email address!");
                return false;
            }

            User user = srvStorage.getUser(email);
            String res = user == null ?
                    Codes.NEWUSER.toString() : Codes.FOUNDUSER.toString();
            long generated = new SecureRandom().nextLong();
            output.writeObject(res + ";" + generated);

            Message msg = (Message) input.readObject();
            long received = Long.parseLong((String) msg.getSignedObject().getObject());
            // TODO better exception handling here in case of FileNotFound
            PublicKey pubKey = user == null ?
                    msg.getCertificate().getPublicKey()
                    : SecurityUtils.getUserPubKey(new File(user.certificate()));

            boolean verified = SecurityUtils.verifySignature(pubKey, msg.getSignedObject());
            if (generated == received && verified) {
                if (user == null) {
                    String keyPath = "server-files/users_pub_keys/" + email + ".cer";
                    File pubKeyFile = new File(keyPath);
                    SecurityUtils.savePublicKeyToFile(msg.getCertificate().getPublicKey(), pubKeyFile);
                    output.writeObject(Codes.OKNEWUSER.toString());
                    if (!authentication2FA(apiKey, email)) return false;
                    devUser = new User(email, keyPath);
                    srvStorage.saveUser(this.devUser);
                } else {
                    output.writeObject(Codes.OKUSER.toString());
                    if (!authentication2FA(apiKey, email)) return false;
                    devUser = srvStorage.getUser(email);
                }
                return true;
            } else {
                output.writeObject(Codes.NOK.toString());
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error on authentication process!");
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
            System.err.println("Error during device validation!");
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
        result = result.equals(Codes.OK.toString()) ?
                "Success: Domain " + d + " created!"
                : "Error: Domain " + d + " not created!";
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
                    "Success: Added " + user.name() + " to domain " + d
                    : "Error: Unable to add " + user.name() + " to domain " + d;
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
                "Success: Registered " + device + " to domain " + d
                : "Error: Unable to register " + device + " to domain " + d;
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
     * @throws IOException if an error occurred when writing to the server
     *         files, or during the communication between client and server
     * @see Codes
     */
    private void handleET() throws IOException {
        try {
            List<Domain> domains = srvStorage.getDeviceDomains(device);
            if (domains.isEmpty()) {
                System.out.println("Error: Device not registered!");
                output.writeObject(Codes.NRD.toString());
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
            for (Domain domain : domains) {
                String keyPath = "server-files/domain_keys/" + domain.getName()
                        + "/" + devUser.name() + ".key.cif";

                File keyFile = new File(keyPath);
                if (!keyFile.exists()) {
                    System.out.println("Error: Key not found!");
                    output.writeObject(Codes.NOK.toString());
                    return;
                }

                // Send the key
                output.writeInt((int) keyFile.length()); // Send key size
                sendFile(keyPath, (int) keyFile.length()); // Send key

                // Receiving encrypted temperature
                String encTemp = (String) this.input.readObject();

                // Save temperature
                String res = srvStorage.saveTemperature(device, encTemp, domain);
                if(res.equals(Codes.OK.toString())) {
                    output.writeObject(Codes.OK.toString());
                } else {
                    output.writeObject(Codes.NOK.toString());
                }
            }

            // Receive confirmation of all temperatures were sent to the server
            String allTempsReceived = (String) input.readObject();
            if (allTempsReceived.equals(Codes.OK.toString())) {
                System.out.println("Success: Temperature received!");
                output.writeObject(Codes.OK.toString());
            } else {
                System.out.println("Error: Unable to receive temperature!");
                output.writeObject(Codes.NOK.toString());
            }
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
                System.out.println("Error: Device not registered!");
                output.writeObject(Codes.NRD.toString());
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
                String keyPath = "server-files/domain_keys/" + domain.getName() + "/" + devUser.name() + ".key.cif";
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
            }

            String allImagesReceived = (String) input.readObject(); // Receive confirmation of all images were sent to the server

            if (allImagesReceived.equals("ALL_IMAGES_SENT")) {
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
        } else if (!domain.getUsers().contains(devUser)) {
            System.out.println("Error: User does not have permissions!");
            output.writeObject(Codes.NOPERM.toString());
        } else {
            String path = srvStorage.getDomainTemperatures(domain);
            if (path != null) {
                String keyPath = "server-files/domain_keys/" + domain.getName()
                        + "/" + devUser.name() + ".key.cif";

                // Find domain key
                File keyFile = new File(keyPath);
                if (!keyFile.exists()) {
                    System.out.println("Error: Key not found!");
                    output.writeObject(Codes.NOK.toString());
                    return;
                }
                output.writeObject(Codes.OK.toString());

                // Send the key
                output.writeInt((int) keyFile.length());
                sendFile(keyPath, (int) keyFile.length());

                // Send the temperatures file
                int size = (int) new File(path).length();
                output.writeInt(size);
                String result = sendFile(path, size) ?
                        "Success: Temperatures from domain " + d + " sent successfully"
                        : "Error: Failed to send temperatures from domain " + d;
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
                    if (d.getUsers().contains(devUser)) {
                        // Domain key
                        File domainKeyEnc = new File("server-files/domain_keys/" 
                                + d.getName() + "/" + devUser.name() + ".key.cif");
                        if (!domainKeyEnc.exists()) continue;

                        // Image encrypted
                        File imageEnc = new File("server-files/images/" 
                                + device.getUser() + "_" + device.getId() + "_" + d.getName() + ".jpg.cif");
                        if (!imageEnc.exists()) continue;

                        output.writeObject("SENDING_FILES"); // Warn client that server is going to send the files

                        //input.readObject(); // Client is ready to receive the key

                        output.writeObject(d.getName());

                        output.writeInt((int) domainKeyEnc.length());
                        sendFile(domainKeyEnc.getPath(), (int) domainKeyEnc.length());

                        input.readObject(); // Client is ready to receive the image
                        output.writeInt((int) imageEnc.length());
                        sendFile(imageEnc.getPath(), (int) imageEnc.length());

                        input.readObject(); // Client is ready to receive the image encryption params

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

    /**
     * Returns the {@code Device} of this {@code Connection}
     *
     * @return the {@code Device} of this {@code Connection}
     */
    public Device getDevice() {
        return device;
    }

}
