package com.iot.server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerStorage {

    private static final String USERS = "users.csv";
    private static final String SIZE = "client_size";
    private final List<ServerConnection> connections;

    public ServerStorage() {
        this.connections = new ArrayList<>();
    }

    protected void start() {
        File users = new File(USERS);
        try {
            if (!users.exists()) {
                if(users.createNewFile()){
                    System.out.println("Users text file created successfully");
                }
                else {
                    System.err.println("Unable to create user text file");
                }
            }
            else {
                System.out.println("Users text file loaded successfully");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected static String[] searchUser(String username) {
        File users = new File(USERS);
        try {
            BufferedReader in = new BufferedReader(new FileReader(users));
            String line;
            while ((line = in.readLine()) != null) {
                String[] data = line.split(",");
                if (data[0].equals(username)){
                    return new String[]{data[0], data[1]};
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    protected static void saveUser(String username, String password) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(USERS, true));
            writer.write(username + "," + password + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void addConnection(ServerConnection connection) {
        this.connections.add(connection);
    }

    protected void removeConnection(ServerConnection connection){
        this.connections.remove(connection);
    }

    protected List<ServerConnection> getConnections() {
        return this.connections;
    }
}
