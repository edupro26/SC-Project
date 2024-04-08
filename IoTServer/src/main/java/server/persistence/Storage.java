package server.persistence;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import server.components.User;
import server.components.Device;
import server.components.Domain;
import server.communication.Codes;

import javax.crypto.SecretKey;

import static server.security.SecurityUtils.*;


/**
 * The storage of the {@code IoTServer}. This class is responsible for
 * saving data sent from the {@code IoTDevice}. It holds the data structures
 * where users, domains and devices are saved. It also handles interaction
 * with text files and images (.jpg).
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see Domain
 * @see Device
 * @see User
 */
public final class Storage {

    /**
     * File paths
     */
    private static final String INFO = "device_info.csv";
    private static final String USERS = "server-files/users.csv";
    private static final String DOMAINS = "server-files/domains.csv";
    private static final String DEVICES = "server-files/devices.csv";

    /**
     * Data structures
     */
    private final List<User> users;
    private final List<Domain> domains;
    private final HashMap<Device, List<Domain>> devices;

    /**
     * SecretKey for encrypt data files
     */
    private final SecretKey secretKey;

    /**
     * Initiates a new Storage for the IoTServer
     */
    public Storage(String passwordCypher) {
        users = new ArrayList<>();
        domains = new ArrayList<>();
        devices = new HashMap<>();
        secretKey = generateKey(passwordCypher);
        new FileLoader(this);
    }

    /**
     * Saves the given {@code User} to the list {@link #users} of this
     * storage. It also writes the user to a users.csv file located
     * in the server-files folder.
     *
     * @param user the {@code User} to be saved
     * @requires {@code user != null}
     * @see FileLoader
     */
    public synchronized void saveUser(User user) {
        File usersFile = new File(USERS);
        String currentUsersData = usersFile.exists() ? decryptDataFromFile(usersFile, this.secretKey) : "";
        currentUsersData += user + "\n";
        users.add(user);
        encryptDataIntoFile(currentUsersData, usersFile, this.secretKey);
    }

    /**
     * Saves the {@code Device} as the key, and a list of domains as the value,
     * to the map {@link #devices} of this storage. It also writes the device
     * to a devices.csv file located in the server-files folder.
     *
     * @param device the {@code Device} to be saved
     * @param domains a list of {@code Domains} where the {@code Device} is registered
     * @requires {@code device != null && domains != null}
     * @see FileLoader
     */
    public synchronized void saveDevice(Device device, List<Domain> domains) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DEVICES, true))) {
            writer.write(device + "," + device.getLastTemp() + "\n");
            devices.put(device, domains);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Creates a new {@code Domain} with the {@code name} and {@code owner}
     * given and saves it the list {@link #domains} of this storage.
     * It also writes the domain to a domains.csv file located in the
     * server-files folder.
     *
     * @param name the name of the {@code Domain}
     * @param owner the owner of the {@code Domain}
     * @requires {@code name != null}
     * @return "OK" if the method concluded with success, "NOK" otherwise.
     * @see FileLoader
     * @see Codes
     */
    public synchronized String createDomain(String name, User owner) {
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

    /**
     * Updates the domains.csv file located in the server-files
     * folder in the {@code Domain} given.
     *
     * @param domain the {@code Domain} to write in file
     * @return true if the method concluded with success, false otherwise
     * @see FileLoader
     * @requires {@code domain != null}
     */
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

    /**
     * Updates the devices.csv file located in the server-files folder with
     * the last temperature sent from the {@code Device} given.
     *
     * @param device the {@code Device}
     * @return "OK" if the method concluded with success, "NOK" otherwise.
     * @see FileLoader
     * @see Codes
     * @requires {@code device != null}
     */
    public synchronized String updateLastTemp(Device device) {
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

    /**
     * Adds a given {@code User} to a given {@code Domain} of this storage.
     * It also updates the content of the {@code Domain} in the domains.csv
     * file located in the server-files folder.
     *
     * @param user the {@code User} of the current {@code Device}
     * @param userToAdd the {@code User} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @return "NODM" if the {@code domain} does not exist,
     *         "NOUSER" if the {@code userToAdd} does not exist,
     *         "NOPERM" if the {@code user} does not have permission,
     *         "NOK" if there was an error writing to the file,
     *         "OK" if the method concluded with success.
     * @see #updateDomainInFile(Domain)
     * @see Codes
     */
    public synchronized String addUserToDomain(User user, User userToAdd, Domain domain) {
        if (domain == null) return Codes.NODM.toString();
        if (userToAdd == null) return Codes.NOUSER.toString();
        if (!domain.getOwner().equals(user)) return Codes.NOPERM.toString();
        if (domain.getUsers().contains(userToAdd)) return Codes.NOK.toString();

        domain.addUser(userToAdd);
        return updateDomainInFile(domain) ? Codes.OK.toString() : Codes.NOK.toString();
    }

    /**
     * Adds a given {@code Device} to a given {@code Domain} of this storage.
     * It also updates the content of the {@code Domain} in the domains.csv
     * file located in the server-files folder.
     *
     * @param user the {@code User} of the current {@code Device}
     * @param device the {@code Device} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @return "NODM" if the {@code domain} does not exist,
     *         "NOPERM" if the {@code user} does not have permission,
     *         "NOK" if the {@code device} is already in the {@code domain},
     *              or there was an error writing to the file,
     *         "OK" if the method concluded with success.
     * @see #updateDomainInFile(Domain)
     * @see Codes
     */
    public synchronized String addDeviceToDomain(Domain domain, Device device, User user) {
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

    /**
     * Verifies if a {@code User} has permission to read data
     * sent from the {@code Device}.
     *
     * @param user the {@code User} to verify
     * @param device the {@code Device}
     * @return true, if the user has permission, false otherwise
     */
    public boolean hasPerm(User user, Device device) {
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

    /**
     * Validates the name and the size of the {@code IoTDevice} executable
     *
     * @param name the name {@code IoTDevice} executable
     * @param size the size {@code IoTDevice} executable
     * @return true, if validated, false otherwise
     */
    public boolean checkConnectionInfo(String name, String size) {
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

    /**
     * Returns a {@code User} from the list {@link #users}
     * of this storage that matches the username given.
     *
     * @param username the username of the {@code User}
     * @return a {@code User}, if the username was found, null otherwise
     */
    public User getUser(String username) {
        for (User user : users) {
            if (username.equals(user.getName()))
                return user;
        }
        return null;
    }

    /**
     * Returns a {@code Device} from the map {@link #devices}
     * of this storage that matches the {@code Device} given, used as a key.
     *
     * @param device the {@code Device} used as key for the search
     * @return a {@code Device}, if the key matched, null otherwise
     */
    public Device getDevice(Device device) {
        for (Map.Entry<Device, List<Domain>> entry : devices.entrySet()) {
            if (entry.getKey().equals(device))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Returns a {@code Domain} from the list {@link #domains}
     * of this storage that matches the name given.
     *
     * @param name the name of the {@code Domain}
     * @return a {@code Domain}, if the name matched, null otherwise
     */
    public Domain getDomain(String name) {
        for (Domain domain : domains) {
            if (name.equals(domain.getName()))
                return domain;
        }
        return null;
    }

    /**
     * Returns the map {@link #devices} of this storage.
     *
     * @return the map {@link #devices} of this storage.
     */
    public HashMap<Device, List<Domain>> getDevices() {
        return devices;
    }


    /**
     * Private class used when constructing a new {@code Storage}.
     * Responsible for creating/loading files used by this storage.
     */
    private static class FileLoader {

        /**
         * Folder names
         */
        private static final String SERVER_FILES = "server-files";
        private static final String TEMPERATURES = "temperatures";
        private static final String IMAGES = "images";

        /**
         * Constructs a new {@code FileLoader}.
         *
         * @param srvStorage this storage
         * @see Storage
         * @see #start(File, File, File, Storage)
         */
        private FileLoader(Storage srvStorage) {
            createFolders();
            File users = new File(USERS);
            File domains = new File(DOMAINS);
            File temps = new File(DEVICES);
            this.start(users, domains, temps, srvStorage);
        }

        /**
         * Creates all the files used by this storage to save data.
         * If they already exist, then loads their content to the
         * data structures of this storage.
         *
         * @param users the users.csv file
         * @param domains the domains.csv file
         * @param temps the devices.csv file
         * @param srvStorage this storage
         * @see #loadUsers(Storage)
         * @see #loadDomains(Storage)
         * @see #loadTemps(Storage)
         */
        private void start(File users, File domains, File temps, Storage srvStorage) {
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

        /**
         * Loads the data from users.csv file to the list
         * {@link #users} of this storage
         *
         * @param srvStorage this storage
         */
        private void loadUsers(Storage srvStorage) {
            File usersFile = new File(USERS);
            if (!usersFile.exists()) return;
            String usersData = decryptDataFromFile(usersFile, srvStorage.secretKey);
            String[] users = usersData.split("\n");
            for (String user : users) {
                String[] data = user.split(",");
                srvStorage.users.add(new User(data[0],data[1]));
            }
        }

        /**
         * Loads the data from domains.csv file to the list
         * {@link #domains} of this storage
         *
         * @param srvStorage this storage
         */
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

        /**
         * Loads the data from devices.csv file to the map
         * {@link #devices} of this storage
         *
         * @param srvStorage this storage
         */
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

        /**
         * Creates a new file
         *
         * @param file the file to create
         * @param type the type of the file (Users, Domains, Temperatures)
         * @return true, if file was created, false otherwise
         */
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

        /**
         * Creates the folders necessary to store the files of
         * this storage, if they do not already exist.
         */
        private void createFolders() {
            File server = new File(SERVER_FILES);
            if (!server.isDirectory()) server.mkdir();
            File temps = new File(TEMPERATURES);
            if (!temps.isDirectory()) temps.mkdir();
            File images = new File(IMAGES);
            if (!images.isDirectory()) images.mkdir();
        }

    }

}
