package com.iot.server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerStorage {

    private final List<ServerConnection> connections;
    private static FileHandler fileHandler;

    public ServerStorage() {
        this.connections = new ArrayList<>();
        fileHandler = new FileHandler();
        fileHandler.start();
    }

    protected static boolean checkDeviceInfo(String name, String size) {
        return fileHandler.checkDeviceInfo(name, size);
    }

    protected static String[] searchUser(String username) {
        return fileHandler.searchUser(username);
    }

    protected static void saveUser(String username, String password) {
        fileHandler.saveUser(username, password);
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

    private static class FileHandler {

        private static final String USERS = "users.csv";
        private static final String INFO = "device_info.csv";

        private void start() {
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

        private String[] searchUser(String username) {
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

        private void saveUser(String username, String password) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(USERS, true));
                writer.write(username + "," + password + "\n");
                writer.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private boolean checkDeviceInfo(String name, String size) {
            InputStream in = getClass().getClassLoader().getResourceAsStream(INFO);

            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(",");
                        if (name.equals(data[0]) && size.equals(data[1]))
                            return true;
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            return false;
        }

    }
}
