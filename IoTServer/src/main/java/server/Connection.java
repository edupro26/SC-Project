package server;

import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Connection {

    private final Storage srvStorage;
    private final ObjectInputStream input;
    private final ObjectOutputStream output;

    private final String clientIP;
    private User devUser;
    private Device device;

    public Connection(ObjectInputStream input, ObjectOutputStream output, Storage srvStorage, String clientIP) {
        this.srvStorage = srvStorage;
        this.input = input;
        this.output = output;
        this.clientIP = clientIP;
        this.devUser = null;
        this.device = null;
        userAuthentication();
    }

    private void userAuthentication() {
        try {
            String in = (String) input.readObject();
            String[] userParts = in.split(",");
            User logIn = new User(userParts[0], userParts[1]);

            devUser = srvStorage.getUser(logIn.getName());
            if (devUser == null) {
                srvStorage.saveUser(logIn);
                devUser = logIn;
                output.writeObject("OK-NEW-USER");
            }
            else {
                while (!logIn.getPassword().equals(devUser.getPassword())) {
                    output.writeObject("WRONG-PWD");
                    String password = ((String) input.readObject()).split(",")[1];
                    logIn.setPassword(password);
                }
                output.writeObject("OK-USER");
            }
            this.device = new Device(devUser.getName(), -1);
        } catch (Exception e) {
            System.out.println("Error receiving user password!");
        }
    }

    protected boolean validateDevID() {
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
                            output.writeObject("OK-DEVID");
                            System.out.println("Device ID validated!");
                            return true;
                        }
                    }
                    else {
                        device.setConnected(true);
                        srvStorage.saveDevice(device, new ArrayList<>());
                        output.writeObject("OK-DEVID");
                        System.out.println("Device ID validated!");
                        return true;
                    }
                }
                output.writeObject("NOK-DEVID");
                device.setId(-1);
            }
        } catch (Exception e) {
            System.out.println("Something went wrong!");
        }
        return false;
    }

    protected boolean validateConnection() {
        try {
            String[] in = ((String) input.readObject()).split(",");
            String name = in[0];
            String size = in[1];
            boolean tested = srvStorage.checkConnectionInfo(name, size);
            if (tested) {
                output.writeObject("OK-TESTED");
                System.out.println("Device info validated!");
                return true;
            }
            else {
                output.writeObject("NOK-TESTED");
                System.out.println("Device info not validated!");
            }
        } catch (Exception e) {
            System.out.println("Something went wrong!");
        }
        return false;
    }

    protected void handleRequests() {
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
                            output.writeObject("NOK");
                            System.out.println("Error: Unable to send image!");
                            break;
                        }
                        try {
                            Integer.parseInt(devParts[1]);
                        } catch (NumberFormatException e) {
                            output.writeObject("NOK");
                            System.out.println("Error: Unable to send image!");
                            break;
                        }
                        handleRI(devParts[0], Integer.parseInt(devParts[1]));
                    }
                    default -> output.writeObject("NOK");
                }
            }
        } catch (Exception e) {
            this.device.setConnected(false);
            System.out.println("Client disconnected (" + this.clientIP + ")");
        }
    }

    private void handleCREATE(String d) throws IOException {
        String result = srvStorage.createDomain(d, devUser);
        output.writeObject(result);
        result = result.equals("OK") ?
                "Success: Domain created!" : "Error: Domain not created!";
        System.out.println(result);
    }

    private void handleADD(String u, String d) throws IOException {
        User user = srvStorage.getUser(u);
        Domain domain = srvStorage.getDomain(d);
        String result = srvStorage.addUserToDomain(this.devUser, user, domain);
        output.writeObject(result);
        result = result.equals("OK") ?
                "Success: User added!" : "Error: Unable to add user!";
        System.out.println(result);
    }

    private void handleRD(String d) throws IOException {
        Domain domain = srvStorage.getDomain(d);
        String result = srvStorage.addDeviceToDomain(domain, device, devUser);
        output.writeObject(result);
        result = result.equals("OK") ?
                "Success: Device registered!" : "Error: Unable to register device!";
        System.out.println(result);
    }

    private void handleET(String t) throws IOException {
        try {
            device.setLastTemp(Float.parseFloat(t));
            String result = srvStorage.updateLastTemp(device);
            output.writeObject(result);
            result = result.equals("OK") ?
                    "Success: Temperature received!" : "Error: Unable to receive temperature!";
            System.out.println(result);
        } catch (Exception e) {
            output.writeObject("NOK");
            System.out.println("Error: Unable to receive temperature!");
        }
    }

    private void handleEI(String filePath) throws IOException {
        File image = new File(filePath);
        if (image.isFile() && image.exists()) {
            receiveImage(image);
            System.out.println("Success: Image received!");
        } else {
            System.out.println("Error: Unable to receive image!");
            output.writeObject("NOK");
        }
    }

    private void handleRT(String d) throws IOException {
        Domain domain = srvStorage.getDomain(d);
        if (domain == null) {
            output.writeObject("NODM");
            System.out.println("Error: Domain does not exist!");
        } else if (!domain.getUsers().contains(devUser) &&
                !domain.getOwner().equals(devUser)) {
            output.writeObject("NOPERM");
            System.out.println("Error: User does not have permissions!");
        } else {
            File file = domain.getDomainTemperatures();
            if (file == null) {
                output.writeObject("NODATA");
                System.out.println("Error: No data found for this device!");
            }
            else {
                sendFile(file);
            }
        }
    }

    private void handleRI(String user, int id) throws IOException {
        Device received = new Device(user, id);
        Device device = srvStorage.getDevice(received);
        if (device == null) {
            output.writeObject("NOID");
            System.out.println("Error: Device id not found!");
        } else if (!srvStorage.hasPerm(devUser, device)) {
            output.writeObject("NOPERM");
            System.out.println("Error: User does not have permissions!");
        } else {
            String name = device.getUser() + "_" + device.getId() + ".jpg";
            File image = new File(new File("images"), name);
            if (image.isFile() && image.exists()){
                sendFile(image);
            } else {
                output.writeObject("NODATA");
                System.out.println("Error: No data found for this device!");
            }
        }
    }

    private void receiveImage(File image) throws IOException {
        output.writeObject("OK");
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

    private void sendFile(File file) throws IOException {
        output.writeObject("OK");
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
