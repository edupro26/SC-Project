package com.iot.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.List;

public class NetworkConnection {
    private String userId;
    private String devId;
    private Boolean isAuth = false;

    private User user;

    private final List<NetworkConnection> connections;

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


            this.user = findUser(userId);
            if (this.user == null) {
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

    public User findUser(String userId) {
        return User.findUser(userId);

    }

    public void registerUser(String password) {
       this.user = new User(userId, password);
    }

    public Boolean authenticate(String password) {
       return this.user.getPassword().equals(password);
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
                    if (connection.getDevId().equals(msg) && connection.getAuth()) {
                        output.writeObject("NOK-DEVID");
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    this.devId = msg;
                    output.writeObject("OK-DEVID");
                    isAuth = true;
                    connections.add(this);
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
