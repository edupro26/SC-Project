package server;

import java.io.*;
import java.util.*;

public class Storage {

    private static final String INFO = "device_info.csv";
    private static final String USERS = "server-files/users.csv";
    private static final String DOMAINS = "server-files/domains.csv";
    private static final String DEVICES = "server-files/devices.csv";

    private final List<User> users;
    private final List<Domain> domains;
    private final HashMap<Device, List<Domain>> devices;

    public Storage() {
        users = new ArrayList<>();
        domains = new ArrayList<>();
        devices = new HashMap<>();
        new FileLoader(this);
    }

    protected synchronized void saveUser(User user) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS, true))) {
            writer.write(user + "\n");
            users.add(user);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected synchronized void saveDevice(Device device, List<Domain> domains) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DEVICES, true))) {
            writer.write(device + "," + device.getLastTemp() + "\n");
            devices.put(device, domains);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected synchronized String createDomain(String name, User owner) {
        if (owner == null) return Codes.NOK.toString();
        if (getDomain(name) != null) return Codes.NOK.toString();
        try {
            Domain domain = new Domain(name, owner);
            domains.add(domain);
            BufferedWriter writer = new BufferedWriter(new FileWriter(DOMAINS, true));
            writer.write(domain + "\n");
            writer.close();
            return Codes.OK.toString();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return Codes.NOK.toString();
        }
    }

    private boolean updateDomainInFile(Domain domain) {
        try (BufferedReader in = new BufferedReader(new FileReader(DOMAINS))) {
            StringBuilder file = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp[0].equals(domain.getName()))
                    file.append(domain).append("\n");
                else
                    file.append(line).append("\n");
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(DOMAINS, false));
            out.write(file.toString());
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    protected synchronized String updateLastTemp(Device device) {
        try (BufferedReader in = new BufferedReader(new FileReader(DEVICES))) {
            StringBuilder file = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp[0].equals(device.toString())) {
                    file.append(device).append(",")
                            .append(device.getLastTemp()).append("\n");
                } else {
                    file.append(line).append("\n");
                }
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(DEVICES, false));
            out.write(file.toString());
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return Codes.NOK.toString();
        }
        return Codes.OK.toString();
    }

    protected synchronized String addUserToDomain(User user, User userToAdd, Domain domain) {
        if (domain == null) return Codes.NODM.toString();
        if (userToAdd == null) return Codes.NOUSER.toString();
        if (!domain.getOwner().equals(user)) return Codes.NOPERM.toString();
        if (domain.getUsers().contains(userToAdd)) return Codes.NOK.toString();

        domain.addUser(userToAdd);
        return updateDomainInFile(domain) ? Codes.OK.toString() : Codes.NOK.toString();
    }

    protected synchronized String addDeviceToDomain(Domain domain, Device device, User user) {
        if(domain == null) return Codes.NODM.toString();
        if(domain.getDevices().contains(device)) return Codes.NOK.toString();
        User owner = domain.getOwner();
        if(!domain.getUsers().contains(user)) {
            if (!owner.getName().equals(user.getName()))
                return Codes.NOPERM.toString();
        }
        domain.addDevice(device);
        devices.get(device).add(domain);
        return updateDomainInFile(domain) ? Codes.OK.toString() : Codes.NOK.toString();
    }

    protected boolean hasPerm(User user, Device device) {
        for (Map.Entry<Device, List<Domain>> entry : devices.entrySet()) {
            if (entry.getKey().equals(device)) {
                for (Domain domain : entry.getValue()) {
                    if (domain.getUsers().contains(user) || domain.getOwner().equals(user))
                        return true;
                }
            }
        }
        return false;
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
                in.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return false;
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

        private static final String SERVER_FILES = "server-files";
        private static final String TEMPERATURES = "temperatures";
        private static final String IMAGES = "images";

        private FileLoader(Storage srvStorage) {
            createFolders();
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
            try (BufferedReader in = new BufferedReader(new FileReader(USERS))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] data = line.split(",");
                    srvStorage.users.add(new User(data[0],data[1]));
                }
                System.out.println("Users text file loaded successfully");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Unable to load users text file");
            }
        }

        private void loadDomains(Storage srvStorage) {
            try (BufferedReader in = new BufferedReader(new FileReader(DOMAINS))) {
                String line;
                while ((line = in.readLine()) != null) {
                    srvStorage.domains.add(new Domain(line, srvStorage));
                }
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
            try (BufferedReader in = new BufferedReader(new FileReader(DEVICES))) {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] fileData = line.split(",");
                    Device device = new Device(fileData[0], fileData[1]);
                    Device exits = srvStorage.getDevice(device);
                    if (exits != null) {
                        List<Domain> domains = srvStorage.devices.get(exits);
                        srvStorage.devices.remove(exits);
                        for (Domain domain : domains) {
                            domain.getDevices().remove(exits);
                            domain.getDevices().add(device);
                        }
                        srvStorage.devices.put(device, domains);
                    }
                    else {
                        srvStorage.devices.put(device, new ArrayList<>());
                    }
                }
                System.out.println("Temperatures text file loaded successfully");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Unable to load temperatures text file");
            }
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

        protected void createFolders() {
            File server = new File(SERVER_FILES);
            if (!server.isDirectory()) server.mkdir();
            File temps = new File(TEMPERATURES);
            if (!temps.isDirectory()) temps.mkdir();
            File images = new File(IMAGES);
            if (!images.isDirectory()) images.mkdir();
        }

    }

}
