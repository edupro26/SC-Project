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
public class User {

    /**
     * User attributes
     */
    private final String name;          // the name of this user
    private final String certificate;   // the certificate of this user

    /**
     * Constructs a new {@code User} with a name and a password.
     *
     * @param name the name of this user
     * @param certificate the certificate of this user
     * @requires {@code name != null && password != null}
     */
    public User(String name, String certificate) {
        this.name = name;
        this.certificate = certificate;
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
        return this.name + "," + this.certificate;
    }

}
