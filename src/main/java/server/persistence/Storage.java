package server.persistence;

import common.Codes;
import server.ServerLogger;
import server.components.*;
import server.persistence.managers.*;
import server.security.IntegrityVerifier;
import server.security.SecurityUtils;

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
    private static final String CLIENT_COPY = "classes/IoTServer/device_info.csv";
    private static final String USERS = "server/users.txt";
    private static final String DOMAINS = "server/domains.txt";
    private static final String HMACS = "server/hmacs.txt";

    /**
     * Storage managers
     */
    private final UserManager userManager;
    private final DomainManager domainManager;
    private final DeviceManager deviceManager;

    /**
     * Used for file integrity verification
     */
    private final IntegrityVerifier integrityVerifier;

    /**
     * Initiates a new Storage for the IoTServer
     *
     * @param passwordCypher password used for encryption
     * @see FileLoader
     */
    public Storage(String passwordCypher) {
        userManager = UserManager.getInstance(USERS, passwordCypher);
        domainManager = DomainManager.getInstance(DOMAINS);
        deviceManager = DeviceManager.getInstance();
        integrityVerifier = new IntegrityVerifier(HMACS, passwordCypher);
        new FileLoader(this);
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
        deviceManager.saveDevice(device, new ArrayList<>());
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
        if (!integrityVerifier.verify(DOMAINS))
            return Codes.CRR.toString();
        return domainManager.createDomain(name, owner, integrityVerifier);
    }

    /**
     * Saves the last temperature sent from the given {@code Device} and
     * updates the devices.txt file located in the server-files folder.
     * Returns "OK" if the method concluded with success, "NOK" otherwise
     *
     * @param device      the {@code Device}
     * @param temperature the last temperature sent
     * @param domain
     * @return status code
     * @requires {@code device != null && temperature != null}
     * @see Codes
     */
    public String saveTemperature(Device device, String temperature, Domain domain) {
        return domainManager.saveTemperature(device, temperature, domain);
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
    public String getDomainTemperatures(Domain domain) {
        return domainManager.getDomainTemperatures(domain);
    }

    /**
     * Adds a given {@code User} to a given {@code Domain} of this storage.
     * It also updates the content of the {@code Domain} in the domains.txt
     * file located in the server-files folder. Returns "NOK" or "CRR" if
     * there was an error writing to the file, "OK" if the method concluded
     * with success.
     *
     * @param userToAdd the {@code User} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @return status code
     * @see Codes
     */
    public String addUserToDomain(User userToAdd, Domain domain) {
        if (!integrityVerifier.verify(DOMAINS))
            return Codes.CRR.toString();
        return domainManager.addUserToDomain(userToAdd, domain, integrityVerifier);
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
        if (!integrityVerifier.verify(DOMAINS))
            return Codes.CRR.toString();
        String res = domainManager.addDeviceToDomain(domain, device, user, integrityVerifier);
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
        try (BufferedReader br = new BufferedReader(new FileReader(CLIENT_COPY))) {
            String line;
            while ((line = br.readLine()) != null) {
                info = line.split(",");
            }
        } catch (IOException e) {
            ServerLogger.logError("Client copy information not found");
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
        private static final String SERVER_FILES = "server";
        private static final String TEMPERATURES =  SERVER_FILES + "/temperatures";
        private static final String IMAGES = SERVER_FILES + "/images";
        private static final String USERS_PUB_KEYS_DIR =  SERVER_FILES + "/users_pub_keys";
        private static final String DOMAIN_KEYS_DIR =  SERVER_FILES + "/domain_keys";

        /**
         * Constructs a new {@code FileLoader}.
         *
         * @param srvStorage this storage
         * @see Storage
         */
        private FileLoader(Storage srvStorage) {
            this.start(srvStorage);
        }

        /**
         * Creates all the files used by this storage to save data.
         * If they already exist, then loads their content to the
         * data structures of this storage.
         *
         * @param srvStorage this storage
         * @see #loadUsers(Storage)
         * @see #loadDomains(Storage)
         */
        private void start(Storage srvStorage) {
            createFolders();
            loadUsers(srvStorage);

            IntegrityVerifier verifier = srvStorage.integrityVerifier;
            verifier.init();
            if (verifier.verifyAll()) {
                ServerLogger.logInfo("File integrity verified!");
            } else {
                ServerLogger.logErrorAndExit("Corrupted files found!" +
                        " Shutting down...");
            }

            File file = new File(DOMAINS);
            if (!file.exists()) {
                try {
                    if (file.createNewFile()){
                        ServerLogger.logInfo("Domains text file created successfully");
                    }
                } catch (IOException e) {
                    ServerLogger.logErrorAndExit("Unable to create domains text file");
                }
            } else {
                loadDomains(srvStorage);
            }
            System.out.println();
            StringBuilder sb = new StringBuilder();
            List<Domain> domains = srvStorage.domainManager.getDomains();
            if (!domains.isEmpty()) {
                for (Domain domain : domains)
                    sb.append("Domain ").append(domain.getName()).append(" -> ")
                            .append(domain).append(" ").append("\n");
                ServerLogger.logInfo("Printing server domains...");
                System.out.println(sb);
            }
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
                        if (SecurityUtils.getUserPubKey(new File(data[1])) == null){
                            ServerLogger.logErrorAndExit("Cipher password is incorrect!" +
                                    " Shutting down...");
                        }
                        srvStorage.userManager.getUsers().add(newUser);
                    }
                    ServerLogger.logInfo("Users text file loaded successfully");
                } else {
                    ServerLogger.logErrorAndExit("Users text file could not be loaded");
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
                    srvStorage.domainManager.getDomains()
                            .add(new Domain(line, srvStorage));
                }
                for (Domain domain : srvStorage.domainManager.getDomains()){
                    for(Device device: domain.getDevices()) {
                        List<Domain> domains = srvStorage.deviceManager
                                .getDevices().get(device);
                        domains.add(domain);
                        srvStorage.getDevices().put(device, domains);
                    }
                }
                ServerLogger.logInfo("Domains text file loaded successfully");
            } catch (IOException e) {
                ServerLogger.logErrorAndExit("Unable to load domains text file");
            }
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
