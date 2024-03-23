package server;

import java.io.*;
import java.util.*;

public class Storage {

    // TODO needs further research and testing
    private static final Object monitor = new Object();

    private static final String INFO = "device_info.csv";
    private static final String USERS = "users.csv";
    private static final String DOMAINS = "domains.csv";
    private static final String DEVICES = "devices.csv";

    private final List<User> users;
    private final List<Domain> domains;
    private final HashMap<Device, List<Domain>> devices;

    public Storage() {
        users = new ArrayList<>();
        domains = new ArrayList<>();
        devices = new HashMap<>();
        new FileLoader(this);
    }

    private boolean updateDomainInFile(Domain domain) {
        File domains = new File(DOMAINS);
        try (BufferedReader in = new BufferedReader(new FileReader(domains))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp[0].equals(domain.getName()))
                    content.append(domain).append("\n");
                else
                    content.append(line).append("\n");
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(domains, false));
            out.write(content.toString());
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    protected String addUserToDomain(User user, User userToAdd, Domain domain) {
        if (domain == null) return "NODM";
        if (userToAdd == null) return "NOUSER";
        if (!domain.getOwner().equals(user)) return "NOPERM";
        if (domain.getUsers().contains(userToAdd)) return "NOK";

        domain.addUser(userToAdd);
        return updateDomainInFile(domain) ? "OK" : "NOK";
    }

    protected String addDeviceToDomain(Domain domain, Device device, User user) {
        if(domain == null) return "NODM";
        if(domain.getDevices().contains(device)) return "NOK";
        User owner = domain.getOwner();
        if(!domain.getUsers().contains(user)) {
            if (!owner.getName().equals(user.getName()))
                return "NOPERM";
        }
        domain.addDevice(device);
        devices.get(device).add(domain);
        return updateDomainInFile(domain) ? "OK" : "NOK";
    }

    protected boolean checkConnectionInfo(String name, String size) {
        InputStream in = Storage.class.getClassLoader().getResourceAsStream(INFO);
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    if (name.equals(data[0]) && size.equals(data[1]))
                        return true;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return false;
    }

    protected void saveUser(User user) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(USERS, true));
            writer.write(user + "\n");
            writer.close();
            users.add(user);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void saveDevice(Device device, List<Domain> domains) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DEVICES, true));
            writer.write(device + ";" + device.getLastTemp() + "\n");
            writer.close();
            devices.put(device, domains);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected String createDomain(String name, User owner) {
        if (owner == null) return "NOK";
        if (getDomain(name) != null) return "NOK";
        try {
            synchronized (monitor) {
                Domain domain = new Domain(name, owner);
                domains.add(domain);
                BufferedWriter writer = new BufferedWriter(new FileWriter(DOMAINS, true));
                writer.write(domain + "\n");
                writer.close();
            }
            return "OK";
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return "NOK";
        }
    }

    protected User getUser(String username) {
        for (User user : users) {
            if (username.equals(user.getName()))
                return user;
        }
        return null;
    }

    protected Device getDevice(Device device) {
        for (Map.Entry<Device, List<Domain>> entry : devices.entrySet()) {
            if (entry.getKey().equals(device))
                return entry.getKey();
        }
        return null;
    }

    protected Domain getDomain(String name) {
        for (Domain domain : domains) {
            if (name.equals(domain.getName()))
                return domain;
        }
        return null;
    }

    protected HashMap<Device, List<Domain>> getDevices() {
        return devices;
    }


    private static class FileLoader {

        private FileLoader(Storage srvStorage) {
            File users = new File(USERS);
            File domains = new File(DOMAINS);
            File temps = new File(DEVICES);
            this.start(users, domains, temps, srvStorage);
        }

        private void start(File users, File domains, File temps, Storage srvStorage) {
            if (!createFile(users, "Users"))
                loadUsers(srvStorage);
            if (!createFile(domains, "Domains"))
                loadDomains(srvStorage);
            if (!createFile(temps, "Temperatures"))
                loadTemps(srvStorage);

            StringBuilder sb = new StringBuilder();
            for (Domain domain : srvStorage.domains)
                sb.append("Domain ").append(domain.getName()).append(" -> ")
                        .append(domain).append(" ").append("\n");
            System.out.println("Printing server domains...");
            System.out.println(sb);
        }

        private void loadUsers(Storage srvStorage) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(USERS));
                String line;
                while ((line = in.readLine()) != null) {
                    String[] data = line.split(",");
                    srvStorage.users.add(new User(data[0],data[1]));
                }
                in.close();
                System.out.println("Users text file loaded successfully");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Unable to load users text file");
            }
        }

        private void loadDomains(Storage srvStorage) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(DOMAINS));
                String line;
                while ((line = in.readLine()) != null) {
                    srvStorage.domains.add(new Domain(line, srvStorage));
                }
                in.close();
                for (Domain domain : srvStorage.domains){
                    for(Device device: domain.getDevices()) {
                        List<Domain> domains = srvStorage.devices.get(device);
                        domains.add(domain);
                        srvStorage.devices.put(device, domains);
                    }
                }
                System.out.println("Domains text file loaded successfully");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Unable to load domains text file");
            }
        }

        private void loadTemps(Storage srvStorage) {
            // TODO
        }

        private boolean createFile(File file, String type) {
            try {
                if (!file.exists()) {
                    if (file.createNewFile()){
                        System.out.println(type + " text file created successfully");
                        return true;
                    }
                    System.out.println("Unable to create " + type.toLowerCase() + " text file");
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            return false;
        }

    }

}
