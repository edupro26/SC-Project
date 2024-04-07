package server.components;


/**
 * Represents a user with a name and a password.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see Device
 */
public final class User {

    /**
     * User attributes
     */
    private final String name;      // the name of this user
    private String password;        // the password of this user

    /**
     * Constructs a new {@code User} with a name and a password.
     *
     * @param name the name of this user
     * @param password the password of this user
     * @requires {@code name != null && password != null}
     */
    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }

    /**
     * Returns the name of this user.
     *
     * @return the name of this user
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the password of this user.
     *
     * @return the password of this user
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of this user.
     *
     * @param password the password to be set
     * @requires {@code password != null}
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Compares if this user is equal to the {@link Object} given.
     *
     * @param obj the object to compare
     * @return true if it is equal, false otherwise
     */
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

    /**
     * Returns a string representation of this user
     *
     * @return a string representation of this user
     */
    @Override
    public String toString() {
        return this.name + "," + this.password;
    }

}
