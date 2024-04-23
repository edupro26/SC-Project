package server.components;


/**
 * Represents a user with a name and a password.
 *
 * @param name        User attributes
 *                    the name of this user
 * @param certificate the certificate of this user
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 * @see Device
 */
public record User(String name, String certificate) {

    /**
     * Constructs a new {@code User} with a name and a password.
     *
     * @param name        the name of this user
     * @param certificate the certificate of this user
     * @requires {@code name != null && password != null}
     */
    public User {
    }

    /**
     * Returns the name of this user.
     *
     * @return the name of this user
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the certificate of this user.
     *
     * @return the certificate of this user
     */
    @Override
    public String certificate() {
        return certificate;
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
        return user.name().equals(this.name);
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
