package server.persistence;

import common.Codes;
import server.components.*;
import server.persistence.managers.*;
import server.security.IntegrityVerifier;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static server.security.SecurityUtils.decryptDataFromFile;


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
public class Storage {

    /**
     * File paths
     */
    private static final String INFO = "classes/device_info.csv";
    private static final String USERS = "server-files/users.txt";
    private static final String DOMAINS = "server-files/domains.txt";
    private static final String DEVICES = "server-files/devices.txt";
    private static final String CHECKSUMS = "server-files/checksums.txt";

    /**
     * Storage managers
     */
    private final UserManager userManager;
    private final DomainManager domainManager;
    private final DeviceManager deviceManager;

    /**
     * Used for file integrity verification
     */
    private final IntegrityVerifier fileVerifier;

    /**
     * Initiates a new Storage for the IoTServer
     *
     * @param passwordCypher password used for encryption
     * @see FileLoader
     */
    public Storage(String passwordCypher) {
        userManager = UserManager.getInstance(USERS, passwordCypher);
        domainManager = DomainManager.getInstance(DOMAINS);
        deviceManager = DeviceManager.getInstance(DEVICES);
        new FileLoader(this);

        fileVerifier = new IntegrityVerifier(CHECKSUMS);
    }

    /**
     * Saves the given {@code User} to this storage. It also writes
     * the user to a users.txt file located in the server-files folder.
     *
     * @param user the {@code User} to be saved
     * @requires {@code user != null}
     */
    public void saveUser(User user) {
        userManager.saveUser(user);
    }

    /**
     * Saves a new {@code Device} to this storage. It also writes the device
     * to a devices.txt file located in the server-files folder.
     *
     * @param device the {@code Device} to be saved
     * @requires {@code device != null}
     */
    public void saveDevice(Device device) {
        deviceManager.saveDevice(device, new ArrayList<>(), fileVerifier);
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
    public String createDomain(String name, User owner) {
        return domainManager.createDomain(name, owner, fileVerifier);
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
    public String updateLastTemp(Device device, Float temperature) {
        return deviceManager.updateLastTemp(device, temperature, fileVerifier);
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
    public String domainTemperaturesFile(Domain domain) {
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
    public String addUserToDomain(User user, User userToAdd, Domain domain) {
        return domainManager.addUserToDomain(user, userToAdd, domain, fileVerifier);
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
    public String addDeviceToDomain(Domain domain, Device device, User user) {
        String res = domainManager.addDeviceToDomain(domain, device, user, fileVerifier);
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
     * Returns the information about the local client copy
     *
     * @return the information about the local client copy
     */
    public String[] getCopyInfo() {
        String[] info = null;
        try (BufferedReader br = new BufferedReader(new FileReader(INFO))) {
            String line;
            while ((line = br.readLine()) != null) {
                info = line.split(",");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return info;
    }

    /**
     * Returns a {@code User} from this storage
     * that matches the username given.
     *
     * @param username the username of the {@code User}
     * @return a {@code User}, if the username was found, null otherwise
     */
    public User getUser(String username) {
        return userManager.getUser(username);
    }

    /**
     * Returns a {@code Device} from this storage that matches
     * the {@code Device} given, used as a key.
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
     * Returns the map of devices of this storage.
     *
     * @return the map of devices of this storage.
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
         * @see #start(Storage, File, File)
         */
        private FileLoader(Storage srvStorage) {
            createFolders();
            File domains = new File(DOMAINS);
            File temps = new File(DEVICES);
            this.start(srvStorage, domains, temps);
        }

        /**
         * Creates all the files used by this storage to save data.
         * If they already exist, then loads their content to the
         * data structures of this storage.
         *
         * @param domains the domains.txt file
         * @param temps the devices.txt file
         * @param srvStorage this storage
         * @see #loadUsers(Storage)
         * @see #loadDomains(Storage)
         * @see #loadTemps(Storage)
         */
        private void start(Storage srvStorage, File domains, File temps) {
            loadUsers(srvStorage);
            if (!createFile(domains, "Domains"))
                loadDomains(srvStorage);
            if (!createFile(temps, "Temperatures"))
                loadTemps(srvStorage);

            StringBuilder sb = new StringBuilder();
            for (Domain domain : srvStorage.domainManager.getDomains())
                sb.append("Domain ").append(domain.getName()).append(" -> ")
                        .append(domain).append(" ").append("\n");
            System.out.println("Printing server domains...");
            System.out.println(sb);
        }

        /**
         * Loads the data from users.txt file to this storage
         *
         * @param srvStorage this storage
         */
        private void loadUsers(Storage srvStorage) {
            File usersFile = new File(USERS);
            if (usersFile.exists()) {
                SecretKey usersKey = srvStorage.userManager.getSecretKey();
                String usersData = decryptDataFromFile(usersFile, usersKey);
                if (usersData != null) {
                    String[] users = usersData.split("\n");
                    for (String user : users) {
                        String[] data = user.split(",");
                        User newUser = new User(data[0], data[1]);
                        srvStorage.userManager.getUsers().add(newUser);
                    }
                    System.out.println("Users text file loaded successfully");
                } else {
                    System.out.println("Users text file could not be loaded");
                }
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
                    srvStorage.domainManager.getDomains().add(new Domain(line, srvStorage));
                }
                for (Domain domain : srvStorage.domainManager.getDomains()){
                    for(Device device: domain.getDevices()) {
                        List<Domain> domains = srvStorage.deviceManager.getDevices().get(device);
                        domains.add(domain);
                        srvStorage.getDevices().put(device, domains);
                    }
                }
                System.out.println("Domains text file loaded successfully");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("Unable to load domains text file");
            }
        }

        /**
         * Loads the temperatures from devices.txt file to
         * the devices of this storage
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
                        List<Domain> domains = srvStorage.getDevices().get(exits);
                        srvStorage.getDevices().remove(exits);
                        for (Domain domain : domains) {
                            domain.getDevices().remove(exits);
                            domain.getDevices().add(device);
                        }
                        srvStorage.getDevices().put(device, domains);
                    }
                    else {
                        srvStorage.getDevices().put(device, new ArrayList<>());
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
