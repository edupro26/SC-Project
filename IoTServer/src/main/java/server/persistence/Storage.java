package server.persistence;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import server.components.User;
import server.components.Device;
import server.components.Domain;
import server.communication.Codes;
import server.persistence.managers.DeviceManager;
import server.persistence.managers.DomainManager;

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
 * @see DomainManager
 * @see Device
 * @see User
 */
public final class Storage {

    /**
     * File paths
     */
    private static final String INFO = "device_info.txt";
    private static final String USERS = "server-files/users.txt";
    private static final String DOMAINS = "server-files/domains.txt";
    private static final String DEVICES = "server-files/devices.txt";

    /**
     * Data structures
     */
    private final List<User> users;

    /**
     * Storage managers
     */
    private static DomainManager domainManager;
    private static DeviceManager deviceManager;
    // TODO private static UserManager userManager;

    /**
     * SecretKey for encrypt data files
     */
    private final SecretKey secretKey;

    /**
     * Initiates a new Storage for the IoTServer
     *
     * @see FileLoader
     */
    public Storage(String passwordCypher) {
        domainManager = DomainManager.getInstance(DOMAINS);
        deviceManager = DeviceManager.getInstance(DEVICES);
        users = new ArrayList<>();
        secretKey = generateKey(passwordCypher);
        new FileLoader(this);
    }

    /**
     * Saves the given {@code User} to the list {@link #users} of this
     * storage. It also writes the user to a users.txt file located
     * in the server-files folder.
     *
     * @param user the {@code User} to be saved
     * @requires {@code user != null}
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
     * to a devices.txt file located in the server-files folder.
     *
     * @param device the {@code Device} to be saved
     * @param domains a list of {@code Domains} where the {@code Device} is registered
     * @requires {@code device != null && domains != null}
     */
    public synchronized void saveDevice(Device device, List<Domain> domains) {
        deviceManager.saveDevice(device, domains);
    }

    /**
     * Creates a new {@code Domain} with the {@code name} and {@code owner}
     * given and saves it in this storage. It also writes the domain to a
     * domains.txt file located in the server-files folder. Returns "OK" if
     * the method concluded with success, "NOK" otherwise.
     *
     * @param name the name of the {@code Domain}
     * @param owner the owner of the {@code Domain}
     * @requires {@code name != null}
     * @return status code
     *
     * @see Codes
     */
    public synchronized String createDomain(String name, User owner) {
        return domainManager.createDomain(name, owner);
    }

    /**
     * Saves the last temperature sent from the given {@code Device} and
     * updates the devices.txt file located in the server-files folder.
     * Returns "OK" if the method concluded with success, "NOK" otherwise
     *
     * @param device the {@code Device}
     * @param temperature the last temperature sent
     * @return status code
     * @see Codes
     * @requires {@code device != null && temperature != null}
     */
    public synchronized String updateLastTemp(Device device, Float temperature) {
        return deviceManager.updateLastTemp(device, temperature);
    }

    /**
     * Returns the path of the file containing the temperatures sent by
     * the devices of the given {@code Domain}. Creates if it does not
     * already exist or updates it with the most recent temperatures
     *
     * @param domain the {@code Domain}
     * @return the path of the file containing the temperatures, null
     *          if there is no data or in case of error
     * @requires {@code domain != null}
     */
    public synchronized String domainTemperaturesFile(Domain domain) {
        return domainManager.domainTemperaturesFile(domain);
    }

    /**
     * Adds a given {@code User} to a given {@code Domain} of this storage.
     * It also updates the content of the {@code Domain} in the domains.txt
     * file located in the server-files folder. Returns "NODM" if the
     * {@code domain} does not exist, "NOUSER" if the {@code userToAdd} does
     * not exist, "NOPERM" if the {@code user} does not have permission, "NOK"
     * if there was an error writing to the file, "OK" if the method concluded
     * with success.
     *
     * @param user the {@code User} of the current {@code Device}
     * @param userToAdd the {@code User} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @return status code
     * @see Codes
     */
    public synchronized String addUserToDomain(User user, User userToAdd, Domain domain) {
        return domainManager.addUserToDomain(user, userToAdd, domain);
    }

    /**
     * Adds a given {@code Device} to a given {@code Domain} of this storage.
     * It also updates the content of the {@code Domain} in the domains.txt
     * file located in the server-files folder. Returns "NODM" if the
     * {@code domain} does not exist, "NOPERM" if the {@code user} does not
     * have permission, "NOK" if the {@code device} is already in the
     * {@code domain} or there was an error writing to the file, "OK" if the
     * method concluded with success.
     *
     * @param user the {@code User} of the current {@code Device}
     * @param device the {@code Device} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @return status code
     * @see Codes
     */
    public synchronized String addDeviceToDomain(Domain domain, Device device, User user) {
        String res = domainManager.addDeviceToDomain(domain, device, user);
        if (res.equals(Codes.OK.toString())) {
            deviceManager.addDomainToDevice(device, domain);
        }
        return res;
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
        return deviceManager.hasPerm(user, device);
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
        return deviceManager.getDevice(device);
    }

    /**
     * Returns a list of {@code Domains} containing all the
     * domains where the given {@code Device} is registered
     *
     * @param device the {@code Device}
     * @return a list of {@code Domains}
     */
    public List<Domain> getDeviceDomains(Device device) {
        return deviceManager.getDeviceDomains(device);
    }

    /**
     * Returns a {@code Domain} from this storage, that matches the name given.
     *
     * @param name the name of the {@code Domain}
     * @return a {@code Domain}, if the name matched, null otherwise
     */
    public Domain getDomain(String name) {
        return domainManager.getDomain(name);
    }

    /**
     * Returns the map {@link #devices} of this storage.
     *
     * @return the map {@link #devices} of this storage.
     */
    public HashMap<Device, List<Domain>> getDevices() {
        return deviceManager.getDevices();
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
        private static final String TEMPERATURES = "server-files/temperatures";
        private static final String IMAGES = "server-files/images";
        private static final String USERS_PUB_KEYS_DIR = "server-files/users_pub_keys";
        private static final String DOMAIN_KEYS_DIR = "server-files/domain_keys";

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
         * @param users the users.txt file
         * @param domains the domains.txt file
         * @param temps the devices.txt file
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
            for (Domain domain : Storage.domainManager.getDomains())
                sb.append("Domain ").append(domain.getName()).append(" -> ")
                        .append(domain).append(" ").append("\n");
            System.out.println("Printing server domains...");
            System.out.println(sb);
        }

        /**
         * Loads the data from users.txt file to the list
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
         * Loads the data from domains.txt file to this storage
         *
         * @param srvStorage this storage
         */
        private void loadDomains(Storage srvStorage) {
            try (BufferedReader in = new BufferedReader(new FileReader(DOMAINS))) {
                String line;
                while ((line = in.readLine()) != null) {
                    Storage.domainManager.getDomains().add(new Domain(line, srvStorage));
                }
                for (Domain domain : Storage.domainManager.getDomains()){
                    for(Device device: domain.getDevices()) {
                        List<Domain> domains = Storage.deviceManager.getDevices().get(device);
                        domains.add(domain);
                        Storage.deviceManager.getDevices().put(device, domains);
                    }
                }
                System.out.println("Domains text file loaded successfully");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Unable to load domains text file");
            }
        }

        /**
         * Loads the data from devices.txt file to the map
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
                        List<Domain> domains = Storage.deviceManager.getDevices().get(exits);
                        Storage.deviceManager.getDevices().remove(exits);
                        for (Domain domain : domains) {
                            domain.getDevices().remove(exits);
                            domain.getDevices().add(device);
                        }
                        Storage.deviceManager.getDevices().put(device, domains);
                    }
                    else {
                        Storage.deviceManager.getDevices().put(device, new ArrayList<>());
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
            File usersPubKeys = new File(USERS_PUB_KEYS_DIR);
            if (!usersPubKeys.isDirectory()) usersPubKeys.mkdir();
            File domainKeys = new File(DOMAIN_KEYS_DIR);
            if (!domainKeys.isDirectory()) domainKeys.mkdir();
        }

    }

}
