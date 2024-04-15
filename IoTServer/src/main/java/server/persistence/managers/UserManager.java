package server.persistence.managers;

import server.components.User;

import javax.crypto.SecretKey;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static server.security.SecurityUtils.*;

/**
 * Singleton class that manages the users of the {@code IoTServer}
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see User
 */
public class UserManager {

    /**
     * The instance of {@code UserManager}
     */
    private static UserManager instance = null;

    /**
     * {@code Object} lock to control concurrency
     */
    private final Object usersLock;

    /**
     * Data structures
     */
    private final String usersFile;
    private final List<User> users;

    /**
     * SecretKey to encrypt the users file
     */
    private final SecretKey secretKey;

    /**
     * Constructs a new {@code UserManager}
     *
     * @param filePath the path of the file to be managed
     * @param passwordCypher password used for encryption
     */
    private UserManager(String filePath, String passwordCypher) {
        usersFile = filePath;
        secretKey = generateKey(passwordCypher);
        users = new ArrayList<>();
        usersLock = new Object();
    }

    /**
     * Returns the instance of {@code UserManager} or creates
     * it if the instance is still null
     *
     * @param filePath the path of the file to be managed
     * @param passwordCypher password used for encryption
     * @return the instance of {@code UserManager}
     */
    public static UserManager getInstance(String filePath, String passwordCypher) {
        if (instance == null) {
            instance = new UserManager(filePath, passwordCypher);
        }
        return instance;
    }

    /**
     * Saves the given {@code User} to the list {@link #users}.
     * It also writes the user to an encrypted users.txt file located
     * in the server-files folder.
     *
     * @param user the {@code User} to be saved
     * @requires {@code user != null}
     */
    public void saveUser(User user) {
        File file = new File(usersFile);
        String currentUsersData = file.exists() ? decryptDataFromFile(file, this.secretKey) : "";
        currentUsersData += user + "\n";
        synchronized (usersLock) {
            if (getUser(user.getName()) == null) {
                users.add(user);
                encryptDataIntoFile(currentUsersData, file, this.secretKey);
            }
        }
    }

    /**
     * Returns a {@code User} from the list {@link #users}
     * that matches the username given.
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
     * Returns the {@code SecretKey} used for encryption
     *
     * @return the {@code SecretKey} used for encryption
     */
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * Returns the list {@link #users}
     *
     * @return the list {@link #users}
     */
    public List<User> getUsers() {
        return users;
    }

}
