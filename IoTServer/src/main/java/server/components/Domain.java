package server.components;

import java.util.List;
import java.util.ArrayList;
import java.util.StringJoiner;

import server.persistence.Storage;

/**
 * Represents a domain in the {@code IoTServer} with its users and devices.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see User
 * @see Device
 */
public final class Domain {

    /**
     * Domain attributes
     */
    private final String name;              // the name of the domain
    private final User owner;               // the user who created the domain
    private final List<User> users;         // list of users with read permissions
    private final List<Device> devices;     // list of devices in the domain

    /**
     * Constructs a new {@code Domain} with a name and an owner.
     *
     * @param name the name of this domain
     * @param owner the owner of this domain
     * @requires {@code name != null && owner != null}
     */
    public Domain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.users = new ArrayList<>();
        this.devices = new ArrayList<>();
    }

    /**
     * Constructs a new {@code Domain} with the string representation given.
     *
     * @param domain the string representation of the domain
     * @param srvStorage the storage of the server to get the users and devices
     *
     * @requires {@code domain != null && srvStorage != null}
     */
    public Domain(String domain, Storage srvStorage) {
        String[] domainParts = domain.split(",");
        this.name = domainParts[0];
        this.owner = srvStorage.getUser(domainParts[1]);
        this.users = new ArrayList<>();
        this.devices = new ArrayList<>();

        if (!domainParts[2].equals("[]")) {
            String[] users = domainParts[2].substring(1, domainParts[2].length() - 1).split(";");
            for (String user : users)
                this.users.add(srvStorage.getUser(user));
        }

        if (!domainParts[3].equals("[]")) {
            String[] devices = domainParts[3].substring(1, domainParts[3].length() - 1).split(";");
            for (String device : devices) {
                String[] deviceParts = device.split(":");
                Device newDev = new Device(deviceParts[0], Integer.parseInt(deviceParts[1]));
                srvStorage.getDevices().put(newDev, new ArrayList<>());
                this.devices.add(newDev);
            }
        }
    }

    /**
     * Returns a string containing the temperatures
     * sent by the devices of this {@code Domain}.
     * This string can be empty.
     *
     * @return Returns a string containing the temperatures
     *          sent by the devices of this {@code Domain}
     */
    public String getDomainTemperatures() {
        StringBuilder temps = new StringBuilder();
        for (Device device : devices) {
            if(device.getLastTemp() != null)
                temps.append(device)
                        .append(",")
                        .append(device.getLastTemp())
                        .append("\n");
        }
        return temps.toString();
    }

    /**
     * Adds a {@code User} to this domain.
     *
     * @param user the {@code User} to be added
     * @requires {@code user != null}
     */
    public void addUser(User user) {
        users.add(user);
    }

    /**
     * Adds a {@code Device} to this domain.
     *
     * @param device the {@code Device} to be added
     * @requires {@code device != null}
     */
    public void addDevice(Device device) {
        devices.add(device);
    }

    /**
     * Returns the name of this domain.
     *
     * @return the name of this domain
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the owner of this domain.
     *
     * @return the owner of this domain
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Returns the list of users in this domain.
     *
     * @return the list of users in this domain
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Returns the list of devices in the domain.
     *
     * @return the list of devices in the domain
     */
    public List<Device> getDevices() {
        return devices;
    }

    /**
     * Returns a string representation of this domain.
     *
     * @return a string representation of this domain
     */
    @Override
    public String toString() {
        StringJoiner userJoiner = new StringJoiner(";");
        for (User user : this.users) {
            userJoiner.add(user.getName());
        }
        String user = "[" + userJoiner + "]";
        String devices;
        if (!this.devices.isEmpty()) {
            StringJoiner deviceJoiner = new StringJoiner(";");
            for (Device device : this.devices) {
                deviceJoiner.add(device.toString());
            }
            devices = "[" + deviceJoiner + "]";
        }
        else {
            devices = "[]";
        }
        return name + "," + owner.getName() + "," + user + "," + devices;
    }
}
