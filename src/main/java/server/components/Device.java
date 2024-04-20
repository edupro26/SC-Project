package server.components;


import java.util.Objects;

/**
 * Represents a {@code IoTDevice} on the side of the {@code IoTServer}.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see User
 */
public class Device {

    /**
     * Device attributes
     */
    private final String user;      // user of the device
    private final int id;           // id of the device
    private boolean isConnected;    // connection status

    /**
     * Constructs a new {@code Device} with a user and an id.
     *
     * @param user the user of this device
     * @param id the id of this device
     * @requires {@code user != null and id != null}
     */
    public Device(String user, int id) {
        this.user = user;
        this.id = id;
        isConnected = false;
    }

    /**
     * Returns the user of this device.
     *
     * @return the user of this device
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the id of this device.
     *
     * @return the id of this device
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the connection status of this device.
     *
     * @return true if this device is connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Sets the connection status of the device.
     *
     * @param connected true to connect, false to disconnect
     */
    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    /**
     * Compares if this device is equal to the {@link Object} given.
     *
     * @param obj the object to compare
     * @return true if it is equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Device device = (Device) obj;
        return Objects.equals(user, device.user)
                && Objects.equals(id, device.id);
    }

    /**
     * Returns a string representation of this device
     *
     * @return a string representation of this device
     */
    @Override
    public String toString() {
        return user + ":" + id;
    }

}
