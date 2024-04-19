package client;

import common.*;
import client.security.SecurityUtils;

import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.Scanner;

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

    private String userId;

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
    protected void connect(String userId) {
        try {
            SocketFactory sf = SSLSocketFactory.getDefault();
            socket = (SSLSocket) sf.createSocket(address, port);
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());

            System.out.println("Sending user id:" + userId);
            String res = this.sendReceive(userId);
            if(res == null) {
                System.out.println("Error in the response");
                System.exit(1);
            }

            String[] resSplit = res.split(";");
            if (resSplit.length != 2) {
                System.out.println("Error in the response");
                System.exit(1);
            }

            String flag = resSplit[0];
            long nonce = Long.parseLong(resSplit[1]);

            // If the user is not registered
            if(flag.equals(Codes.NEWUSER.toString())) {
                SignedObject signedObject = new SignedObject(Long.toString(nonce), SecurityUtils.findPrivateKeyOnKeyStore(userId), Signature.getInstance("SHA256withRSA"));
                Message signedMessage = new Message(signedObject, SecurityUtils.getOwnCertificate(userId));
                output.writeObject(signedMessage);
            }
            else { // If the user is registered
                SignedObject signedObject = new SignedObject(Long.toString(nonce), SecurityUtils.findPrivateKeyOnKeyStore(userId), Signature.getInstance("SHA256withRSA"));
                Message signedMessage = new Message(signedObject, SecurityUtils.getOwnCertificate(userId));
                output.writeObject(signedMessage);
            }

            String authRes = (String) input.readObject();
            if (!authRes.equals(Codes.OKUSER.toString()) && !authRes.equals(Codes.OKNEWUSER.toString())) {
                System.out.println("Authentication failed. Certificate not valid.");
                System.exit(1);
            }

            // 2FA Process
            System.out.print("2FA Code: ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            output.writeObject(input);

            String finalAuthRes = (String) this.input.readObject();
            if (!finalAuthRes.equals(Codes.OK2FA.toString())) {
                System.out.println("Authentication failed. 2FA code not valid.");
                System.exit(1);
            }

            this.userId = userId;
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
     * Validates the id of this IoTDevice. It also tests this IoTDevice executable.
     *
     * @param devId id of this IoTDevice
     */
    protected void deviceValidation(String devId) {
        String res = sendReceive(devId);
        if (res.equals(Codes.OKDEVID.toString())) {
            //FIXME remote attestation is disabled
            /*try {
                // Find the client executable
                CodeSource src = IoTDevice.class.getProtectionDomain().getCodeSource();
                String path = src.getLocation().toURI().getPath();
                File exec = new File(path);

                long nonce = (long) input.readObject();
                byte[] hash = CommonUtils.calculateHashWithNonce(exec, nonce);
                if (hash != null) {
                    output.writeObject(exec.getName());
                    output.writeObject(hash);
                    String tested = (String) input.readObject();
                    if (tested.equals(Codes.OKTESTED.toString())) {
                        System.out.println("OK-TESTED # This IoTDevice is valid!");
                    } else {
                        System.out.println("NOK-TESTED # This IoTDevice is not valid!");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Error calculating hash during device validation");
                    System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("Error during device validation");
                System.exit(1);
            }*/
        } else {
            System.out.println("Response: NOK-DEVID # Invalid device id");
            System.exit(1);
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
        if (res.equals(Codes.OK.toString())){
            System.out.println("Response: OK # Domain created successfully");
        } else if (res.equals(Codes.NOK.toString())) {
            System.out.println("Response: NOK # Domain already exists");
        } else {
            System.out.println("Response: NOK # Error creating domain");
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
        if (args.length != 3) {
            System.out.println("Usage: ADD <user1> <dm> <password-domain>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        // TODO: Handle first response
        String res = this.sendReceive(msg);
        if (!res.equals(Codes.OK.toString())) {
            if (res.equals(Codes.NODM.toString())) {
                System.out.println("Response: NODM # Domain does not exist");
                return;
            } else if (res.equals(Codes.NOUSER.toString())) {
                System.out.println("Response: NOUSER # User does not exist");
                return;
            } else if (res.equals(Codes.NOPERM.toString())) {
                System.out.println("Response: NOPERM # User does not exist");
                return;
            } else {
                System.out.println("Response: NOK # Error adding user");
                return;
            }
        }

        PublicKey pk = SecurityUtils.findPublicKeyOnTrustStore(args[0]);
        try {
            if (pk == null) {
                String resNoPk = this.sendReceive("NO_PK");
                System.out.println("Response: NOK # Error adding user. Public key not found to encrypt the key.");
                /*
                output.writeObject("NO_PK");
                String findPkRes = (String) input.readObject();
                if (findPkRes.equals("NOK")) {
                    System.out.println("Response: NOK # Error adding user. Public key not found on the server.");
                    return;
                }
                output.writeObject("WAITING_PK");
                int size = input.readInt();
                receiveFile("server-output/" + args[0] + ".cer", size);
                Encryption.storePubKeyOnTrustStore(new File("server-output/" + args[0] + ".cer"), args[0]);
                */
            } else {
                String resFoundPk = this.sendReceive("SENDING_PK");
            }
        } catch (Exception e) {
            System.out.println("Response: NOK # Error adding user");
            return;
        }

        String keyEncFilename = args[1] + "_" + args[0] + ".key.enc";
        SecurityUtils.encryptKeyWithRSA(SecurityUtils.generateKey(args[2]), pk, keyEncFilename);
        File keyEncFile = new File(keyEncFilename);

        try {
            output.writeInt((int) keyEncFile.length());
            sendFile(keyEncFilename, (int) keyEncFile.length());
            String resKeySent = (String) input.readObject();

            if (keyEncFile.exists()) {
                keyEncFile.delete();
            }
            if (!resKeySent.equals(Codes.OK.toString())) {
                System.out.println("Response: NOK # Error adding user");
                return;
            }

            output.writeObject("WAITING_FINAL_RES");
            String finalRes = (String) input.readObject();
            if (finalRes.equals(Codes.OK.toString())) {
                System.out.println("Response: " + finalRes + " # User added successfully");
                return;
            }
            System.out.println("Response: NOK # Error adding user");
        } catch (Exception e) {
            System.out.println("Response: NOK # Error adding user");
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
        if (res.equals(Codes.OK.toString())) {
            System.out.println("Response: OK # Device registered successfully");
        } else if (res.equals(Codes.NODM.toString())) {
            System.out.println("Response: NODM # Domain does not exist");
        } else if (res.equals(Codes.NOPERM.toString())) {
            System.out.println("Response: NOPERM # This user does not have permissions");
        } else {
            System.out.println("Response: NOK # Error registering device");
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
        if (res.equals(Codes.OK.toString())) {
            System.out.println("Response: OK # Printing domains");
            try {
                System.out.println((String) input.readObject());
            } catch (Exception e) {
                System.out.println("Response: NOK # Error printing domains");
            }
        } else {
            System.out.println("Response: NOK # Device not registered");
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
        if (res.equals(Codes.OK.toString())) {
            System.out.println("Response: OK # Temperature sent successfully");
        } else if (res.equals(Codes.NRD.toString())) {
            System.out.println("Response: NRD # Device not registered");
        } else {
            System.out.println("Response: NOK # Error sending temperature");
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
            String res = this.sendReceive(msg);
            if (res.equals(Codes.NRD.toString())) {
                System.out.println("Response: NRD # Device not registered");
                return;
            }

            String[] domains = res.split(";");
            output.writeObject("RECEIVED_DOMAINS");
            for (String domain : domains) {
                // Receive the domain key
                int size = input.readInt();
                String keyTempPath = domain + ".key.cif.temp";
                receiveFile(keyTempPath, size);

                File encryptedKey = new File(keyTempPath);
                SecretKey key = (SecretKey) SecurityUtils.decryptKeyWithRSA(encryptedKey, SecurityUtils.findPrivateKeyOnKeyStore(this.userId));

                File imageEnc = new File(args[0] + ".cif");
                SecurityUtils.encryptFile(new File(args[0]), imageEnc, key);

                int imageEncSize = (int) imageEnc.length();

                // Send the encrypted image
                output.writeInt(imageEncSize);
                sendFile(imageEnc.getPath(), imageEncSize);

                input.readObject(); // Receive confirmation of the image received

                File imageEncParams = new File(args[0] + ".cif.params");
                output.writeInt((int) imageEncParams.length());
                sendFile(imageEncParams.getPath(), (int) imageEncParams.length());

                input.readObject(); // Receive confirmation of the image params received

                // Delete the temporary key file
                new File(keyTempPath).delete(); // Delete the temporary key file
                imageEnc.delete(); // Delete the encrypted image
                imageEncParams.delete(); // Delete the encrypted image params
            }
            output.writeObject("ALL_IMAGES_RECEIVED");

            String finalRes = (String) input.readObject();
            if (finalRes.equals(Codes.OK.toString())) {
                System.out.println("Response: OK # Image sent successfully");
            } else {
                System.out.println("Response: NOK # Error sending image");
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
        if (res.equals(Codes.OK.toString())) {
            try {
                int size = input.readInt();
                int received = receiveFile(name, size);
                String result = received == size
                        ? "Response: OK, " + received + " (long), followed by "
                        + received + " bytes of data"
                        : "Response: NOK # Error getting temperatures";
                System.out.println(result);
            } catch (IOException e) {
                System.out.println("Response: NOK # Error getting temperatures");
            }
        } else if (res.equals(Codes.NODM.toString())) {
            System.out.println("Response: NODM # Domain does not exist");
        } else if (res.equals(Codes.NOPERM.toString())) {
            System.out.println("Response: NOPERM # This user does not have permissions");
        } else if (res.equals(Codes.NODATA.toString())) {
            System.out.println("Response: NODATA # No data found in this domain");
        } else {
            System.out.println("Response: NOK # Error getting temperatures");
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
        if (args.length != 1 || !args[0].contains(":")) {
            System.out.println("Usage: RI <user-id>:<dev_id>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        String[] temp = args[0].split(":");
        if (res.equals("SENDING_FILES")) {
            try {
                String domain = (String) input.readObject();
                File domainKeyENc = new File(SERVER_OUT + domain + ".key.enc");
                File imageEnc = new File(SERVER_OUT + temp[0] + "_" + temp[1] + ".jpg.cif");
                File imageEncParams = new File(SERVER_OUT + temp[0] + "_" + temp[1] + ".jpg.cif.params");

                // Receive the domain key
                int domainKeyEncSize = input.readInt();
                receiveFile(domainKeyENc.getPath(), domainKeyEncSize);
                output.writeObject("RECEIVED_DOMAIN_KEY");

                // Receive the encrypted image
                int imageEncSize = input.readInt();
                receiveFile(imageEnc.getPath(), imageEncSize);
                output.writeObject("RECEIVED_IMAGE");

                // Receive the encrypted image params
                int imageEncParamsSize = input.readInt();
                receiveFile(imageEncParams.getPath(), imageEncParamsSize);
                output.writeObject("RECEIVED_IMAGE_PARAMS");

                String finalRes = (String) input.readObject();
                if (!finalRes.equals(Codes.OK.toString())) {
                    System.out.println("Response: NOK # Error getting image");
                    return;
                }

                // Decrypt the domain key
                SecretKey key = (SecretKey) SecurityUtils.decryptKeyWithRSA(domainKeyENc, SecurityUtils.findPrivateKeyOnKeyStore(this.userId));

                // Decrypt the image
                File image = new File(SERVER_OUT + temp[0] + "_" + temp[1] + ".jpg");
                SecurityUtils.decryptFile(imageEnc, image, key);

                // Delete the encrypted files
                domainKeyENc.delete();
                imageEnc.delete();
                imageEncParams.delete();
                System.out.println("Response: " + finalRes + " # Image received successfully");
                System.out.println("Image saved as: " + image.getPath());
                    /*
                    String result = received == size
                            ? "Response: " + res + ", " + received
                            + " (long), followed by " + received + " bytes of data"
                            : "Response: NOK # Error getting image";
                    System.out.println(result);
                    */
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Response: NOK # Error getting image");
            }
        } else if (res.equals(Codes.NODATA.toString())) {
            System.out.println("Response: NODATA # No image found for this device");
        } else if (res.equals(Codes.NOID.toString())) {
            System.out.println("Response: NOID # No device found with this id");
        } else if (res.equals(Codes.NOPERM.toString())) {
            System.out.println("Response: NOPERM # This user does not have permissions");
        } else {
            System.out.println("Response: NOK # Error getting image");
        }
    }

    /**
     * Sends a file to the {@code IoTServer}.
     *
     * @param filePath the path of the file to be sent
     * @param size the size of the file to send
     * @requires {@code filePath != null}
     */
    private void sendFile(String filePath, int size) {
        try {
            File file = new File(filePath);
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
            System.out.println(e.getMessage());
        }
    }

    /**
     * Receives a file from the {@code IoTServer} and saves it
     * in the output folder.
     *
     * @param filePath the path where the file will be saved
     * @param size the size of the file to receive
     * @return file size if the file was received, -1 otherwise
     * @requires {@code filePath != null}
     */
    private int receiveFile(String filePath, int size) {
        File outputFolder = new File(SERVER_OUT);
        if (!outputFolder.isDirectory()) outputFolder.mkdir();
        int bytesRead = 0;
        try {
            FileOutputStream out = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            byte[] buffer = new byte[8192];
            int bytesLeft = size;
            while (bytesLeft > 0) {
                int bytes = input.read(buffer);
                bytesRead += bytes;
                bos.write(buffer, 0, bytes);
                bytesLeft -= bytes;
            }
            bos.flush();
            bos.close();
            out.close();
        } catch (IOException e) {
            return -1;
        }
        return bytesRead;
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
