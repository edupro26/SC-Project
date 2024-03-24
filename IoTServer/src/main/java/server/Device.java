package server;

import java.util.Objects;

/**
 * Represents a device in the server.
 *
 * @author Eduardo Proença - 57551, Manuel Barral - 52026, Tiago Oliveira - 54979
 */
public class Device {

    private final String user; // user of the device
    private int id; // id of the device
    private boolean isConnected; // connection status
    private Float lastTemp; // last temperature given by the device

    /**
     * Creates a new device with a user and an id.
     * @param user
     * @param id
     */
    protected Device(String user, int id) {
        this.user = user;
        this.id = id;
        isConnected = false;
        lastTemp = null;
    }

    /**
     * Creates a new device given the representative string of the device.
     * @param device the string representation of the device
     * @param temp the last temperature given by the device
     */
    protected Device(String device, String temp) {
        String[] deviceArgs = device.split(":");
        this.user = deviceArgs[0];
        this.id = Integer.parseInt(deviceArgs[1]);
        isConnected = false;
        if (!temp.equals("null"))
            lastTemp = Float.parseFloat(temp);
        else
            lastTemp = null;
    }

    /**
     * Returns the user of the device.
     * @return the user of the device
     */
    protected String getUser() {
        return user;
    }

    /**
     * Returns the id of the device.
     * @return the id of the device
     */
    protected int getId() {
        return id;
    }

    /**
     * Returns the connection status of the device.
     * @return the connection status of the device
     */
    protected boolean isConnected() {
        return isConnected;
    }

    /**
     * Returns the last temperature given by the device.
     * @return the last temperature given by the device
     */
    protected Float getLastTemp() {
        return lastTemp;
    }

    /**
     * Sets the connection status of the device.
     * @param connected if connected true, if not false
     */
    protected void setConnected(boolean connected) {
        isConnected = connected;
    }

    /**
     * Sets the id of the device.
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Sets the last temperature given by the device.
     * @param lastTemp
     */
    protected void setLastTemp(Float lastTemp) {
        this.lastTemp = lastTemp;
    }

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

    @Override
    public String toString() {
        return user + ":" + id;
    }

}
