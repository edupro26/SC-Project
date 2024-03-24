package server;


/**
 * Represents a user with a name and a password.
 *
 * @author Eduardo Proença - 57551, Manuel Barral - 52026, Tiago Oliveira - 54979
 */
public class User {

    private final String name;
    private String password;

    /**
     * Creates a new user with a name and a password.
     * @param name
     * @param password
     */
    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }

    /**
     * Returns the name of the user.
     * @return the name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the password of the user.
     * @return the password of the user
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the user.
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof User user)) {
            return false;
        }
        return user.getName().equals(this.name);
    }

    @Override
    public String toString() {
        return this.name + "," + this.password;
    }

}
