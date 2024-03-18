package server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ServerStorage {

    private static final String USERS = "users.csv";
    private static final String DOMAINS = "domains.csv";
    private static final String INFO = "device_info.csv";

    private static List<User> users;
    private static List<ServerDomain> domains;
    private static List<ServerConnection> connections;

    public ServerStorage() {
        users = new ArrayList<>();
        connections = new ArrayList<>();
        domains = new ArrayList<>();

        this.start();
    }

    private void start() {
        try {
            File users = new File(USERS);
            File domains = new File(DOMAINS);
            String result;
            if (!users.exists()) {
                result = users.createNewFile() ?
                        "Users text file created successfully" : "Unable to create user text file";
                System.out.println(result);
            }
            else {
                result = loadUsers() ?
                        "Users text file loaded successfully" : "Unable to load users text file";
                System.out.println(result);
            }

            if (!domains.exists()) {
                result = domains.createNewFile() ?
                        "Domains text file created successfully" : "Unable to create domains text file";
                System.out.println(result);
            }
            else {
                result = loadDomains() ?
                        "Domains text file loaded successfully" : "Unable to load domains text file";
                System.out.println(result);
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

    private boolean loadDomains() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(DOMAINS));
            String line;
            while ((line = in.readLine()) != null) {
                domains.add(new ServerDomain(line));
            }
            System.out.println(domains.toString());
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    protected static void saveUser(User user) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(USERS, true));
            writer.write(user + "\n");
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

    protected static ServerDomain searchDomain(String name) {
        for (ServerDomain domain : domains) {
            if (name.equals(domain.getName())){
                return domain;
            }
        }
        return null;
    }

    protected static String createDomain(String name, User owner) {
        if (owner != null) {
            ServerDomain alreadyExists = searchDomain(name);
            if (alreadyExists == null) {
                ServerDomain domain = new ServerDomain(name, owner);
                domains.add(domain);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(DOMAINS, true));
                    writer.write(domain + "\n");
                    writer.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                return "OK";
            }
            return "NOK";
        }
        return "NOK";
    }

    protected static String addUserToDomain(User user, User userToAdd, ServerDomain domain) {
        if (domain == null) return "NODM";
        if (userToAdd == null) return "NOUSER";
        if (!domain.getOwner().equals(user)) return "NOPERM";

        domain.addUser(userToAdd);

        // TODO Save to file

        return "OK";
    }

    protected static void addDeviceToDomain(ServerDomain domain, ServerConnection device) {
        // TODO add a device to a domain and save in domains.csv
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

    protected static ServerConnection searchDevice(String user, int id) {
        for (ServerConnection device : connections) {
            if(user.equals(device.getDevUser().getUsername())
                    && id == device.getDevId()){
                return device;
            }
        }
        return null;
    }

    protected static void addConnection(ServerConnection connection) {
        connections.add(connection);
    }

    protected static void removeConnection(ServerConnection connection){
        connections.remove(connection);
    }

    protected static List<ServerConnection> getConnections() {
        return connections;
    }

}
