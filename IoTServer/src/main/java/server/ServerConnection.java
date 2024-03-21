package server;

import java.io.*;
import java.util.List;

public class ServerConnection {

    private final ObjectInputStream input;
    private final ObjectOutputStream output;

    private final String clientIP;
    private User devUser;
    private Device device;

    // TODO delete when Device is implemented in the project
    private int devId;
    private Boolean hasValidDevId;
    private Float lastTemperature;

    public ServerConnection(ObjectInputStream input, ObjectOutputStream output, String clientIP) {
        this.input = input;
        this.output = output;
        this.clientIP = clientIP;
        this.devUser = null;
        hasValidDevId = false;

        userAuthentication();
    }

    private void userAuthentication() {
        try {
            String in = (String) input.readObject();
            String[] userParts = in.split(",");
            User logIn = new User(userParts[0], userParts[1]);

            devUser = ServerStorage.searchUser(logIn.getName());
            if (devUser == null) {
                ServerStorage.createUser(logIn);
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
        } catch (Exception e) {
            System.out.println("Error receiving user password!");
        }
    }

    protected boolean validateDevID(List<ServerConnection> connections) {
        // TODO remake to use Device. The method will need to receive
        //  the HashMap with Devices and use it to validate the id
        //  In the end the Device should be set to connected
        try {
            while (!hasValidDevId) {
                String msg = (String) input.readObject();
                boolean validId = true;
                for (ServerConnection connection : connections) {
                    if (connection.getDevId() == Integer.parseInt(msg) &&
                            connection.devUser.getName().equals(devUser.getName())) {
                        output.writeObject("NOK-DEVID");
                        validId = false;
                        break;
                    }
                }
                if (Integer.parseInt(msg) < 0) {
                    output.writeObject("NOK-DEVID");
                    validId = false;
                }

                if (validId) {
                    this.devId = Integer.parseInt(msg);
                    output.writeObject("OK-DEVID");
                    hasValidDevId = true;
                    System.out.println("Device ID validated!");
                    return true;
                }
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
            boolean tested = ServerStorage.checkDeviceInfo(name, size);
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
                        result = ServerStorage.createDomain(parsedMsg[1], devUser);
                        output.writeObject(result);
                        result = result.equals("OK") ?
                                "Success: Domain created!" : "Error: Domain not created!";
                        System.out.println(result);
                    }
                    case "ADD" -> {
                        User user = ServerStorage.searchUser(parsedMsg[1]);
                        ServerDomain domain = ServerStorage.searchDomain(parsedMsg[2]);
                        result = ServerStorage.addUserToDomain(this.devUser, user, domain);
                        output.writeObject(result);
                        result = result.equals("OK") ?
                                "Success: User added!" : "Error: Unable to add user!";
                        System.out.println(result);
                    }
                    case "RD" -> {
                        ServerDomain domain = ServerStorage.searchDomain(parsedMsg[1]);
                        // TODO this method will receive a Device
                        result =  ServerStorage.addDeviceToDomain(domain,this);
                        output.writeObject(result);
                        result = result.equals("OK") ?
                                "Success: Device registered!" : "Error: Unable to register device!";
                        System.out.println(result);
                    }
                    case "ET" -> {
                        try {
                            lastTemperature = Float.parseFloat(parsedMsg[1]);
                            output.writeObject("OK");
                        } catch (Exception e) {
                            output.writeObject("NOK");
                        }
                    }
                    // TODO these commands need to be looked at with more
                    //  detail after Device implementation
                    case "EI" -> {
                        long imageSize = Long.parseLong(parsedMsg[1]);

                        output.writeObject("Send image");

                        System.out.println("Receiving image from " + devUser.getName() + ":" + devId + " with size " + imageSize + " bytes.");

                        String imageName = "images/" + devUser.getName() + "_" + devId + ".jpg";

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
                        ServerDomain domain = ServerStorage.searchDomain(parsedMsg[1]);
                        if (domain == null) {
                            output.writeObject("NODM");
                        } else if (!domain.getUsers().contains(devUser) && !domain.getOwner().equals(devUser)) {
                            output.writeObject("NOPERM");
                        }
                        else {
                            String[] temperatures = domain.getDomainTemperatures();
                            if (temperatures.length == 0) {
                                output.writeObject("NODATA");
                            }
                            else {
                                // Form a txt file with the temperatures, send the data size and then the file
                                output.writeObject("OK");
                                StringBuilder data = new StringBuilder();
                                for (String temperature : temperatures) {
                                    data.append(temperature).append("\n");
                                }

                                byte[] dataBytes = data.toString().getBytes();
                                output.writeLong(dataBytes.length);
                                output.write(dataBytes);


                                output.flush();

                            }
                        }
                    }
                    // TODO implement command
                    case "RI" -> {
                        String userDevice = parsedMsg[1];
                        User devUser = ServerStorage.searchUser(userDevice.split(":")[0]);
                        int devId = Integer.parseInt(userDevice.split(":")[1]);

                        // TODO Verify if device exists

                        // TODO Verify permission

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
            System.out.println("Client disconnected (" + this.clientIP + ")");
        }
    }

    protected User getDevUser() {
        return devUser;
    }

    protected Float getLastTemperature() {
        return this.lastTemperature;
    }

    protected int getDevId() {
        return this.devId;
    }

    @Override
    public String toString() {
        return devUser.getName() + ":" + devId;
    }

}
