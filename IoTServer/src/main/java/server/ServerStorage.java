package server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerStorage {

    private static final String USERS = "users.csv";
    private static final String INFO = "device_info.csv";

    private static List<User> users;
    private final List<ServerConnection> connections;
    private List<ServerDomain> domains;

    public ServerStorage() {
        users = new ArrayList<>();
        connections = new ArrayList<>();

        this.start();
    }

    private void start() {
        File users = new File(USERS);
        try {
            // TODO create domain file
            // TODO load domain file
            if (!users.exists()) {
                if(users.createNewFile()){
                    System.out.println("Users text file created successfully");
                }
                else {
                    System.err.println("Unable to create user text file");
                }
            }
            else {
                if (loadUsers()) {
                    System.out.println("Users text file loaded successfully");
                }
                else {
                    System.out.println("Unable to load users text file");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean loadUsers() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(USERS));
            String line;
            while ((line = in.readLine()) != null) {
                String[] data = line.split(",");
                users.add(new User(data[0],data[1]));
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    protected static void saveUser(User user) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(USERS, true));
            writer.write(user.toString() + "\n");
            writer.close();
            users.add(user);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected static User searchUser(String username) {
        for (User user : users) {
            if (username.equals(user.getUsername())){
                return user;
            }
        }
        return null;
    }

    protected static void createDomain(String username) {
        // TODO
    }

    protected static boolean checkDeviceInfo(String name, String size) {
        InputStream in = ServerStorage.class.getClassLoader().getResourceAsStream(INFO);

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
