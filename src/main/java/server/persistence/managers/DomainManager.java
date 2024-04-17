package server.persistence.managers;

import common.Codes;
import server.components.*;
import server.security.IntegrityVerifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that manages the domains of the {@code IoTServer}
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see Domain
 */
public class DomainManager {

    /**
     * The instance of {@code DomainManager}
     */
    private static DomainManager instance = null;

    /**
     * {@code Object} locks to control concurrency
     */
    private final Object domainsLock;
    private final Object tempsLock;

    /**
     * Data structures
     */
    private final String domainsFile;
    private final List<Domain> domains;

    /**
     * Constructs a new {@code DomainManager}
     *
     * @param filePath the path of the file to be managed
     */
    private DomainManager(String filePath) {
        domainsFile = filePath;
        domains = new ArrayList<>();
        domainsLock = new Object();
        tempsLock = new Object();
    }

    /**
     * Returns the instance of {@code DomainManager} or creates
     * it if the instance is still null
     *
     * @param filePath the path of the file to be managed
     * @return the instance of {@code DomainManager}
     */
    public static DomainManager getInstance(String filePath) {
        if (instance == null) {
            instance = new DomainManager(filePath);
        }
        return instance;
    }

    /**
     * Creates a new {@code Domain} with the {@code name} and
     * {@code owner} given and saves it in the list {@link #domains}
     * of the {@code DomainManager}. It also writes the domain to a
     * domains.txt file located in the server-files folder.
     *
     * @param name the name of the {@code Domain}
     * @param owner the owner of the {@code Domain}
     * @param fileVerifier the file {@code IntegrityVerifier}
     * @return "OK" if the method concluded with success, "NOK" otherwise.
     * @requires {@code name != null}
     * @see Codes
     */
    public String createDomain(String name, User owner, IntegrityVerifier fileVerifier) {
        if (owner == null) return Codes.NOK.toString();
        Domain domain = new Domain(name, owner);
        try {
            synchronized (domainsLock) {
                if (getDomain(name) != null) return Codes.NOK.toString();
                BufferedWriter writer = new BufferedWriter(new FileWriter(domainsFile, true));
                writer.write(domain + "\n");
                writer.close();
                domains.add(domain);
                String checksum = fileVerifier.calculateChecksum(new File(domainsFile));
                fileVerifier.updateChecksum(domainsFile, checksum);
            }
            return Codes.OK.toString();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return Codes.NOK.toString();
        }
    }

    /**
     * Updates the domains.txt file located in the server-files
     * folder, with the {@code Domain} given. If it already exists
     * in the file, replaces it.
     *
     * @param domain the {@code Domain} to write in file
     * @return true if the method concluded with success, false otherwise
     *
     * @requires {@code domain != null}
     */
    private boolean updateDomainInFile(Domain domain) {
        try (BufferedReader in = new BufferedReader(new FileReader(domainsFile))) {
            StringBuilder file = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp[0].equals(domain.getName()))
                    file.append(domain).append("\n");
                else
                    file.append(line).append("\n");
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(domainsFile, false));
            out.write(file.toString());
            out.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Adds a given {@code User} to a given {@code Domain} of the list {@link #domains}.
     * It also updates the content of the {@code Domain} in the domains.txt file located
     * in the server-files folder. Returns "NODM" if the {@code domain} does not exist,
     * "NOUSER" if the {@code userToAdd} does not exist, "NOPERM" if the {@code user}
     * does not have permission, "NOK" if there was an error writing to the file, "OK"
     * if the method concluded with success.
     *
     * @param user the {@code User} of the current {@code Device}
     * @param userToAdd the {@code User} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @param fileVerifier the file {@code IntegrityVerifier}
     * @return status code
     * @see Codes
     */
    public String addUserToDomain(User user, User userToAdd, Domain domain, IntegrityVerifier fileVerifier) {
        if (domain == null) return Codes.NODM.toString();
        if (userToAdd == null) return Codes.NOUSER.toString();
        if (!domain.getOwner().equals(user)) return Codes.NOPERM.toString();
        List<User> domainUsers = domain.getUsers();
        if (domainUsers.contains(userToAdd)) return Codes.NOK.toString();
        synchronized (domainsLock) {
            domainUsers.add(userToAdd);
            String res = updateDomainInFile(domain)
                    ? Codes.OK.toString() : Codes.NOK.toString();
            if (res.equals(Codes.NOK.toString())) {
                domainUsers.remove(userToAdd);
            }
            String checksum = fileVerifier.calculateChecksum(new File(domainsFile));
            fileVerifier.updateChecksum(domainsFile, checksum);
            return res;
        }
    }

    /**
     * Adds a given {@code Device} to a given {@code Domain} of the list {@link #domains}.
     * It also updates the content of the {@code Domain} in the domains.txt file located
     * in the server-files folder. Returns "NODM" if the {@code domain} does not exist,
     * "NOPERM" if the {@code user} does not have permission, "NOK" if the {@code device}
     * is already in the {@code domain} or there was an error writing to the file, "OK"
     * if the method concluded with success.
     *
     * @param user the {@code User} of the current {@code Device}
     * @param device the {@code Device} to add to the {@code Domain}
     * @param domain the {@code Domain}
     * @param fileVerifier the file {@code IntegrityVerifier}
     * @return status code
     * @see Codes
     */
    public String addDeviceToDomain(Domain domain, Device device, User user, IntegrityVerifier fileVerifier) {
        if(domain == null) return Codes.NODM.toString();
        if(domain.getDevices().contains(device)) return Codes.NOK.toString();
        String owner = domain.getOwner().getName();
        if (!owner.equals(user.getName())) {
            if(!domain.getUsers().contains(user)) return Codes.NOPERM.toString();
        }
        synchronized (domainsLock) {
            domain.getDevices().add(device);
            String res = updateDomainInFile(domain) ? Codes.OK.toString() : Codes.NOK.toString();
            if (res.equals(Codes.NOK.toString())) {
                domain.getDevices().remove(device);
            }
            String checksum = fileVerifier.calculateChecksum(new File(domainsFile));
            fileVerifier.updateChecksum(domainsFile, checksum);
            return res;
        }
    }

    /**
     * Returns the path of the file containing the temperatures sent
     * by the devices of the given domain. Creates if it does not
     * already exist or updates it with the most recent temperatures
     *
     * @param domain the {@code Domain}
     * @return the path of the file containing the temperatures, null
     *          if there is no data or in case of error
     * @requires {@code domain != null}
     */
    public String domainTemperaturesFile(Domain domain) {
        String path = "server-files/temperatures/" + domain.getName() + ".txt";
        try {
            String temperatures = domain.getDomainTemperatures();
            if (!temperatures.isEmpty()) {
                // FIXME Exception when executing RT D1 for example on
                //  client1, then sleep the thread and execute RT D1 on
                //  client2
                synchronized (tempsLock) {
                    File file = new File(path);
                    if (!file.exists()) file.createNewFile();
                    BufferedWriter out = new BufferedWriter(new FileWriter(file, false));
                    out.write(temperatures);
                    out.close();
                }
                return path;
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * Returns a {@code Domain} from the list {@link #domains}
     * of the {@code DomainManager}, that matches the name given.
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
     * Returns the list of {@code Domains} managed
     * by the {@code DomainsManager}
     *
     * @return the list of {@code Domains}
     */
    public List<Domain> getDomains() {
        return domains;
    }

}