package com.iot.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.List;

public class NetworkConnection {
    private String userId;
    private String devId;
    private Boolean isAuth = false;

    private List<NetworkConnection> connections;

    ObjectInputStream input;
    ObjectOutputStream output;

    public NetworkConnection(ObjectInputStream input, ObjectOutputStream output, List<NetworkConnection> connections) {
        this.input = input;
        this.output = output;
        this.connections = connections;

        try {
            String userPassword = (String) input.readObject();
            String[] parts = userPassword.split(":");
            this.userId = parts[0];
            String password = parts[1];



            if (!findUser(userId)) {
                registerUser(password);
                output.writeObject("OK-NEW-USER");


            } else {
                while(!authenticate(password)) {
                    output.writeObject("WRONG-PWD");
                    password = (String) input.readObject();


                }

                output.writeObject("OK-USER");

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public Boolean findUser(String userId) {
        // TODO: Implement findUser

        return true;
    }

    public void registerUser(String password) {
        // TODO: Implement registerUser
    }

    public Boolean authenticate(String password) {
        // TODO: Implement authenticate

        return true;
    }

    public void handler() {
        try {
            handleDeviceAuth();

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

    public void handleDeviceAuth() {
        try {
            while (!isAuth) {
                String msg = (String) input.readObject();
                boolean found = false;
                for (NetworkConnection connection : connections) {
                    if (connection.getDevId().equals(msg)) {
                        output.writeObject("NOK-DEVID");
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    this.devId = msg;
                    output.writeObject("OK-DEVID");
                    isAuth = true;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public String getUserId() {
        return userId;
    }

    public String getDevId() {
        return devId;
    }

    public Boolean getAuth() {
        return isAuth;
    }

}
