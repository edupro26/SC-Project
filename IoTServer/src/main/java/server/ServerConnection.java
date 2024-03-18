package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class ServerConnection {

    private final String clientIP;
    private final ObjectInputStream input;
    private final ObjectOutputStream output;
    private final ServerStorage srvStorage;

    private String userId;
    private int devId;
    private Boolean hasValidDevId;

    public ServerConnection(ObjectInputStream input, ObjectOutputStream output, String clientIP, ServerStorage srvStorage) {
        this.input = input;
        this.output = output;
        this.clientIP = clientIP;
        this.srvStorage = srvStorage;
        hasValidDevId = false;

        userAuthentication();
    }

    private void userAuthentication() {
        try {
            String in = (String) input.readObject();
            String[] logIn = in.split(",");
            userId = logIn[0];
            String password = logIn[1];

            User user = srvStorage.searchUser(userId);
            if (user == null) {
                srvStorage.saveUser(new User(userId, password));
                output.writeObject("OK-NEW-USER");
            }
            else {
                while (!password.equals(user.getPassword())) {
                    output.writeObject("WRONG-PWD");
                    password = ((String) input.readObject()).split(",")[1];
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
                    if (connection.getDevId() == Integer.parseInt(msg) && connection.getUserId().equals(this.userId)) {
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
            boolean tested = srvStorage.checkDeviceInfo(name, size);
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

                // TODO Handle the message

                output.writeObject("OK");
            }
        } catch (Exception e) {
            System.out.println("Client disconnected (" + this.clientIP + ")");
        }
    }

    protected String getUserId() {
        return this.userId;
    }

    protected int getDevId() {
        return this.devId;
    }
}
