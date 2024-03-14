package com.iot.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class ServerConnection {

    private String userId;
    private String devId;
    private Boolean hasValidDevId;

    private final ObjectInputStream input;
    private final ObjectOutputStream output;

    public ServerConnection(ObjectInputStream input, ObjectOutputStream output) {
        this.input = input;
        this.output = output;
        hasValidDevId = false;

        userAuthentication();
    }

    private void userAuthentication() {
        try {
            String in = (String) input.readObject();
            String[] logIn = in.split(",");
            userId = logIn[0];
            String password = logIn[1];

            String[] user = ServerStorage.searchUser(userId);
            if (user == null) {
                ServerStorage.saveUser(userId, password);
                output.writeObject("OK-NEW-USER");
            }
            else {
                while (!password.equals(user[1])) {
                    output.writeObject("WRONG-PWD");
                    password = ((String) input.readObject()).split(",")[1];
                }
                output.writeObject("OK-USER");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    protected void handleRequests() {
        try {
            while (true) {

                String msg = (String) input.readObject();
                System.out.println("Received: " + msg);

                // Handle the message

                output.writeObject("OK");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    protected void validateDevID(List<ServerConnection> connections) {
        try {
            while (!hasValidDevId) {
                String msg = (String) input.readObject();
                boolean validId = true;
                for (ServerConnection connection : connections) {
                    if (connection.getDevId().equals(msg) && connection.getUserId().equals(this.userId)) {
                        output.writeObject("NOK-DEVID");
                        validId = false;
                        break;
                    }
                }

                if (validId) {
                    this.devId = msg;
                    output.writeObject("OK-DEVID");
                    hasValidDevId = true;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    protected String getUserId() {
        return this.userId;
    }

    protected String getDevId() {
        return this.devId;
    }
}
