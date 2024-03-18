package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class ServerConnection {

    private final String clientIP;
    private final ObjectInputStream input;
    private final ObjectOutputStream output;

    private User devUser;
    private int devId;
    private Boolean hasValidDevId;

    private Float lastTemperature;

    public ServerConnection(ObjectInputStream input, ObjectOutputStream output, String clientIP) {
        this.input = input;
        this.output = output;
        this.clientIP = clientIP;
        //this.srvStorage = srvStorage;
        this.devUser = null;
        hasValidDevId = false;

        userAuthentication();
    }

    private void userAuthentication() {
        try {
            String in = (String) input.readObject();
            String[] logIn = in.split(",");
            User temp = new User(logIn[0], logIn[1]);

            this.devUser = ServerStorage.searchUser(temp.getUsername());
            if (this.devUser == null) {
                ServerStorage.saveUser(temp);
                output.writeObject("OK-NEW-USER");
                this.devUser = temp;
            }
            else {
                while (!temp.getPassword().equals(devUser.getPassword())) {
                    output.writeObject("WRONG-PWD");
                    String password = ((String) input.readObject()).split(",")[1];
                    temp.setPassword(password);
                }
                output.writeObject("OK-USER");
            }
        } catch (Exception e) {
            System.out.println("Error receiving user password!");
        }
    }

    protected boolean validateDevID(List<ServerConnection> connections) {
        try {
            while (!hasValidDevId) {
                String msg = (String) input.readObject();
                boolean validId = true;
                for (ServerConnection connection : connections) {
                    if (connection.getDevId() == Integer.parseInt(msg) &&
                            connection.devUser.getUsername().equals(devUser.getUsername())) {
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

    protected boolean validateDeviceInfo() {
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
                        if (result.equals("OK"))
                            System.out.println("Domain created!");
                    }
                    case "ADD" -> {
                        //TODO finish add command
                        result = ServerStorage.addUserToDomain(this.devUser, ServerStorage.searchUser(parsedMsg[1]),
                                ServerStorage.searchDomain(parsedMsg[2]));
                        output.writeObject(result);
                    }
                    //TODO finish commands
                    case "RD" -> output.writeObject("Not implemented");
                    case "ET" -> {
                        try {
                            lastTemperature = Float.parseFloat(parsedMsg[1]);
                            output.writeObject("OK");
                        } catch (Exception e) {
                            output.writeObject("NOK");
                        }
                    }
                    case "EI" -> output.writeObject("Not implemented");
                    case "RT" -> output.writeObject("Not implemented");
                    case "RI" -> output.writeObject("Not implemented");
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

    protected int getDevId() {
        return this.devId;
    }

    @Override
    public String toString() {
        return devUser.getUsername() + ":" + devId;
    }
}
