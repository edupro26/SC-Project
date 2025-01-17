package client;

import client.security.SecurityUtils;
import common.Codes;
import common.Message;
import common.security.CommonUtils;

import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.CodeSource;
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
    private static final String CLIENT = "client/";

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
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            System.out.println("Requesting authentication for " + userId);
            String res = sendReceive(userId);
            if (res == null || res.split(";").length != 2) {
                System.out.println("Error in the response from the server");
                System.exit(1);
            }

            long nonce = Long.parseLong(res.split(";")[1]);
            SignedObject signedObject = new SignedObject(Long.toString(nonce),
                    SecurityUtils.getPrivateKey(userId), Signature.getInstance("SHA256withRSA"));
            Message signedMessage = new Message(signedObject, SecurityUtils.getCertificate(userId));
            output.writeObject(signedMessage);

            res = (String) input.readObject();
            if (res.equals(Codes.OKUSER.toString()) || res.equals(Codes.OKNEWUSER.toString())) {
                output.writeObject(Codes.OK.toString()); // Signal server that client is waiting for 2FA
                String sent2FA = (String) input.readObject(); // Servers indicates if the 2FA code was sent with success
                if (!sent2FA.equals(Codes.OK.toString())) {
                    System.out.println("Error sending 2FA code.");
                    System.exit(1);
                }

                System.out.print("Enter 2FA Code: ");
                String code2FA = new Scanner(System.in).nextLine();
                output.writeObject(code2FA);
                String res2FA = (String) input.readObject();
                if (res2FA.equals(Codes.OK2FA.toString())) {
                    System.out.println(res + " # User authenticated!");
                    this.userId = userId;
                } else {
                    System.out.println("Authentication failed: Invalid 2FA code.");
                    System.exit(1);
                }
            } else {
                System.out.println("Authentication failed: Invalid user certificate.");
                System.exit(1);
            }
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
        try {
            if (Integer.parseInt(devId) < 0) {
                System.err.println("NOK-DEVID # Invalid device ID");
                System.exit(1);
            }
            String res = sendReceive(devId);
            if (res.equals(Codes.OKDEVID.toString())) {
                System.out.println(res + " # Device ID is valid");
                // Find the client executable
                CodeSource src = IoTDevice.class.getProtectionDomain().getCodeSource();
                String path = src.getLocation().toURI().getPath();
                File exec = new File(path);

                long nonce = (long) input.readObject();
                byte[] hash = CommonUtils.calculateHashWithNonce(exec, nonce);
                if (hash != null) {
                    output.writeObject(exec.getName());
                    output.writeObject(hash);
                    res = (String) input.readObject();
                    if (res.equals(Codes.OKTESTED.toString())) {
                        System.out.println(res + " # IoTDevice is valid!");
                    } else {
                        System.out.println(res + " # IoTDevice is not valid!");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Error calculating hash during device attestation");
                    System.exit(1);
                }
            } else {
                System.out.println(res + " # Invalid device ID");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error during device attestation");
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
        } else if (res.equals(Codes.CRR.toString())) {
            System.out.println("Response: CRR # Corrupted server files");
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
        PublicKey pk = SecurityUtils.findPublicKeyOnTrustStore(args[0]);
        if (pk == null) {
            System.out.println("Response: NOUSER # This user is unknown");
            return;
        }

        String msg = parseCommandToSend(command, args);
        try {
            String res = this.sendReceive(msg);
            if (res.equals(Codes.NODM.toString())) {
                System.out.println("Response: NODM # This domain does not exist");
            } else if (res.equals(Codes.NOUSER.toString())) {
                System.out.println("Response: NOUSER # This user never signed in");
            } else if (res.equals(Codes.NOPERM.toString())) {
                System.out.println("Response: NOPERM # No permission to add this user");
            }
            if (!res.equals(Codes.OK.toString())) return;

            String path = args[1] + "_" + args[0] + ".key.enc";
            SecurityUtils.encryptKeyWithRSA(SecurityUtils.generateKey(args[2]), pk, path);
            File tempFile = new File(path);
            output.writeInt((int) tempFile.length());
            sendFile(path, (int) tempFile.length());
            if (tempFile.exists()) tempFile.delete();

            res = (String) input.readObject();
            if (res.equals(Codes.OK.toString())) {
                System.out.println("Response: OK # User added successfully");
            } else if (res.equals(Codes.CRR.toString())) {
                System.out.println("Response: CRR # Corrupted server files");
            } else {
                System.out.println("Response: NOK # Error adding user");
            }
        } catch (IOException | ClassNotFoundException e) {
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
        } else if (res.equals(Codes.CRR.toString())) {
            System.out.println("Response: CRR # Corrupted server files");
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
        try {
            if (args.length != 1) {
                System.out.println("Usage: ET <float>");
                return;
            }
            args[0] = String.valueOf(Float.parseFloat(args[0]));
        } catch (Exception e) {
            System.out.println("Usage: ET <float>");
            return;
        }
        try {
            String msg = parseCommandToSend(command, args);
            String res = this.sendReceive(msg);
            if (res.equals(Codes.NRD.toString())) {
                System.out.println("Response: NRD # Device not registered");
                return;
            }

            String[] domains = res.split(";");
            output.writeObject(Codes.OK.toString());
            for(String domain : domains) {
                int size = input.readInt();
                String keyTempPath = domain + ".key.cif.temp";
                receiveFile(keyTempPath, size); // Receive the domain key

                File encryptedKey = new File(keyTempPath);
                SecretKey key = (SecretKey) SecurityUtils.decryptKeyWithRSA(
                        encryptedKey, SecurityUtils.getPrivateKey(userId));

                // Encrypt and send the temperature
                output.writeObject(SecurityUtils.encryptTemperature(args[0], key));
                encryptedKey.delete(); // Delete the temporary key file

                // Confirmation temperature has been received
                String response = (String) input.readObject();
                if(response.equals(Codes.NOK.toString())){
                    System.out.println("Response: NOK # Error sending temperature");
                    return;
                }
            }

            output.writeObject(Codes.OK.toString());
            String finalRes = (String) input.readObject();
            if (finalRes.equals(Codes.OK.toString())) {
                System.out.println("Response: OK # Temperature sent successfully");
            } else {
                System.out.println("Response: NOK # Error sending temperature");
            }
        } catch (Exception e) {
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
        if (!args[0].endsWith(".jpg")) { // Check if filename ends in .jpg
            System.out.println("File provided must be a JPG!");
            return;
        }
        File image = new File(args[0]); // Check if image exists
        if (!image.exists() || !image.isFile()) {
            System.out.println("The image provided doesn't exist!");
            return;
        }
        String msg = parseCommandToSend(command, args);
        try {
            String res = this.sendReceive(msg);
            if (res.equals(Codes.NRD.toString())) {
                System.out.println("Response: NRD # Device not registered");
                return;
            }
            // Don't use the entire image path
            args[0] = !args[0].contains("/") ? args[0]
                    : args[0].split("/")[args[0].split("/").length - 1];

            String[] domains = res.split(";");
            output.writeObject(Codes.OK.toString());
            for (String domain : domains) {
                String temp = domain + ".key.cif.temp";
                receiveFile(temp, input.readInt()); // Receive the domain key

                File encryptedKey = new File(temp);
                SecretKey key = (SecretKey) SecurityUtils.decryptKeyWithRSA(
                        encryptedKey, SecurityUtils.getPrivateKey(userId));

                File imageEnc = new File(args[0] + ".cif");
                SecurityUtils.encryptFile(image, imageEnc, key);

                int size = (int) imageEnc.length();
                output.writeInt(size);
                sendFile(imageEnc.getPath(), size); // Send the encrypted image

                input.readObject(); // Receive confirmation
                new File(temp).delete(); // Delete the temporary key file
                imageEnc.delete(); // Delete the encrypted image
            }
            output.writeObject(Codes.OK.toString());
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
        String outputPath = CLIENT + args[0] + ".txt";
        if (res.equals(Codes.OK.toString())) {
            try {
                int keySize = input.readInt();
                String keyTempPath = args[0] + ".key.cif.temp";
                receiveFile(keyTempPath, keySize); // Receive the domain key

                // Receive the file with encryted temperatures
                int fileSize = input.readInt();
                receiveFile(outputPath, fileSize);

                File encryptedKey = new File(keyTempPath);
                SecretKey key = (SecretKey) SecurityUtils.decryptKeyWithRSA(
                        encryptedKey, SecurityUtils.getPrivateKey(this.userId));

                encryptedKey.delete();// Delete temp key file
                File outputFile = new File(outputPath); // Decrypt the temperatures
                int received = SecurityUtils.decryptTemperatures(outputFile, key);
                if (received > 0) {
                    System.out.println("Response: OK, " + received + " (long), " +
                            "followed by " + outputFile.length() + " bytes of data");
                } else {
                    System.out.println("Response: NOK # Error getting temperatures");
                }
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
        try {
            Integer.parseInt(args[0].split(":")[1]);
        } catch (NumberFormatException e) {
            System.out.println("Usage: RI <user-id>:<dev_id>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        String[] temp = args[0].split(":");
        if (res.equals(Codes.OK.toString())) {
            try {
                String domain = (String) input.readObject();
                File domainKeyENc = new File(CLIENT + domain + ".key.enc");
                File imageEnc = new File(CLIENT + temp[0] + "_" + temp[1] + ".jpg.cif");

                // Receive the domain key
                int domainKeyEncSize = input.readInt();
                receiveFile(domainKeyENc.getPath(), domainKeyEncSize);
                output.writeObject(Codes.OK.toString());

                // Receive the encrypted image
                int imageEncSize = input.readInt();
                receiveFile(imageEnc.getPath(), imageEncSize);
                output.writeObject(Codes.OK.toString());

                String finalRes = (String) input.readObject();
                if (!finalRes.equals(Codes.OK.toString())) {
                    System.out.println("Response: NOK # Error getting image");
                    return;
                }

                // Decrypt the domain key and the image
                SecretKey key = (SecretKey) SecurityUtils.decryptKeyWithRSA(
                        domainKeyENc, SecurityUtils.getPrivateKey(userId));
                File image = new File(CLIENT + temp[0] + "_" + temp[1] + ".jpg");
                int received = SecurityUtils.decryptFile(imageEnc, image, key);
                domainKeyENc.delete(); // Delete the key
                imageEnc.delete(); // Delete the encrypted image

                if (received > 0) {
                    System.out.println("Response: OK, " + received + " (long), " +
                            "followed by " + image.length() + " bytes of data");
                } else {
                    System.out.println("Response: NOK # Error getting temperatures");
                }
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
     * @param size     the size of the file to receive
     * @requires {@code filePath != null}
     */
    private void receiveFile(String filePath, int size) {
        File outputFolder = new File(CLIENT);
        if (!outputFolder.isDirectory()) outputFolder.mkdir();
        try {
            FileOutputStream out = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            byte[] buffer = new byte[8192];
            int bytesLeft = size;
            while (bytesLeft > 0) {
                int bytes = input.read(buffer);
                bos.write(buffer, 0, bytes);
                bytesLeft -= bytes;
            }
            bos.flush();
            bos.close();
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
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
