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

                String result;
                switch (command) {
                    case "CREATE" -> {
                        result = srvStorage.createDomain(parsedMsg[1], devUser);
                        output.writeObject(result);
                        result = result.equals("OK") ?
                                "Success: Domain created!" : "Error: Domain not created!";
                        System.out.println(result);
                    }
                    case "ADD" -> {
                        User user = srvStorage.getUser(parsedMsg[1]);
                        Domain domain = srvStorage.getDomain(parsedMsg[2]);
                        result = srvStorage.addUserToDomain(this.devUser, user, domain);
                        output.writeObject(result);
                        result = result.equals("OK") ?
                                "Success: User added!" : "Error: Unable to add user!";
                        System.out.println(result);
                    }
                    case "RD" -> {
                        Domain domain = srvStorage.getDomain(parsedMsg[1]);
                        result = srvStorage.addDeviceToDomain(domain, device, devUser);
                        output.writeObject(result);
                        result = result.equals("OK") ?
                                "Success: Device registered!" : "Error: Unable to register device!";
                        System.out.println(result);
                    }
                    case "ET" -> {
                        try {
                            device.setLastTemp(Float.parseFloat(parsedMsg[1]));
                            result = srvStorage.updateLastTemp(device);
                            output.writeObject(result);
                            result = result.equals("OK") ?
                                    "Success: Temperature received!" : "Error: Unable to receive temperature!";
                            System.out.println(result);
                        } catch (Exception e) {
                            output.writeObject("NOK");
                            System.out.println("Error: Unable to receive temperature!");
                        }
                    }
                    case "EI" -> { // TODO Finish EI command
                        long imageSize = Long.parseLong(parsedMsg[1]);

                        output.writeObject("Send image");

                        System.out.println("Receiving image from " + devUser.getName() + ":" + device.getId() + " with size " + imageSize + " bytes.");

                        String imageName = "images/" + devUser.getName() + "_" + device.getId() + ".jpg";

                        File imageFile = new File(imageName);

                        File parentDir = imageFile.getParentFile();
                        if (!parentDir.exists()) {
                            parentDir.mkdirs();
                        }

                        if (!imageFile.exists()) {
                            imageFile.createNewFile();
                        }

                        FileOutputStream imageOutput = new FileOutputStream(imageFile);

                        long remainingBytes = imageSize;
                        int bytesRead;

                        byte[] buffer = new byte[1024]; // Use a fixed-size buffer
                        while (remainingBytes > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) != -1){
                            imageOutput.write(buffer, 0, bytesRead);
                            remainingBytes -= bytesRead;
                        }

                        imageOutput.flush();
                        imageOutput.close();
                        output.writeObject("OK");

                    }
                    case "RT" -> {
                        Domain domain = srvStorage.getDomain(parsedMsg[1]);
                        if (domain == null) {
                            output.writeObject("NODM");
                        } else if (!domain.getUsers().contains(devUser) &&
                                !domain.getOwner().equals(devUser)) {
                            output.writeObject("NOPERM");
                        } else {
                            File file = domain.getDomainTemperatures();
                            if (file == null) {
                                output.writeObject("NODATA");
                            }
                            else {
                                output.writeObject("OK");
                                byte[] buffer = new byte[(int) file.length()];
                                FileInputStream in = new FileInputStream(file);
                                BufferedInputStream bis = new BufferedInputStream(in);
                                bis.read(buffer, 0, buffer.length);
                                output.write(buffer, 0, buffer.length);
                                output.flush();
                                bis.close();
                            }
                        }
                    }
                    case "RI" -> { // TODO Finish RI command
                        String userDevice = parsedMsg[1];
                        String username = userDevice.split(":")[0];
                        int devId = Integer.parseInt(userDevice.split(":")[1]);

                        Device reqDevice = srvStorage.getDevice(username, devId);
                        if (reqDevice == null) {
                            output.writeObject("NOID");
                            break;
                        }

                        User reqDevUser = srvStorage.getUser(username);

                        if (!srvStorage.hasPerm(reqDevUser, reqDevice)) {
                            output.writeObject("NOPERM");
                            break;
                        }

                        // FInd if image exists
                        String imageName = "images/" + devUser.getName() + "_" + devId + ".jpg";
                        File imageFile = new File(imageName);
                        if (!imageFile.exists()) {
                            output.writeObject("NODATA");
                        } else {
                            output.writeObject("OK");
                            output.writeLong(imageFile.length());


                            FileInputStream fis = new FileInputStream(imageFile);
                            byte[] buffer = new byte[1024];

                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }

                            output.flush();
                            fis.close();
                        }


                    }
                    default -> output.writeObject("NOK");
                }
            }
        } catch (Exception e) {
            this.device.setConnected(false);
            System.out.println("Client disconnected (" + this.clientIP + ")");
        }
    }

}
