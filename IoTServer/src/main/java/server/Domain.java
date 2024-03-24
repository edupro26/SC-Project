package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents a domain in the server with its users and devices.
 *
 * @author Eduardo Proença - 57551, Manuel Barral - 52026, Tiago Oliveira - 54979
 */
public class Domain {

    private final String name; // domain name
    private final User owner; // domain owner
    private final List<User> users; // list of users with read permissions
    private final List<Device> devices; // list of devices in the domain

    /**
     * Creates a new domain with a name and an owner.
     * @param name
     * @param owner
     */
    protected Domain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.users = new ArrayList<>();
        this.devices = new ArrayList<>();
    }

    /**
     * Creates a new domain given the representative string of the domain.
     * @param domain the string representation of the domain
     * @param srvStorage the storage of the server to get the users and devices
     */
    protected Domain(String domain, Storage srvStorage) {
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
     * Returns the file with the temperatures of the domain.
     * @return the file with the temperatures of the domain.
     */
    protected File getDomainTemperatures() {
        File file = new File( "temperatures/" + name + ".txt");
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file, false))) {
            StringBuilder content = new StringBuilder();
            for (Device device : devices) {
                if (device.getLastTemp() != null)
                    content.append(device).append(" -> ")
                        .append(device.getLastTemp()).append("\n");
            }
            out.write(content.toString());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return file.length() > 0 ? file : null;
    }

    /**
     * Adds a user to the domain.
     * @param user the user to be added
     */
    protected void addUser(User user) {
        users.add(user);
    }

    /**
     * Adds a device to the domain.
     * @param device the device to be added
     */
    protected void addDevice(Device device) {
        devices.add(device);
    }

    /**
     * Returns the name of the domain.
     * @return the name of the domain
     */
    protected String getName() {
        return name;
    }

    /**
     * Returns the owner of the domain.
     * @return the owner of the domain
     */
    protected User getOwner() {
        return owner;
    }

    /**
     * Returns the list of users with read permissions of the domain.
     * @return list of users
     */
    protected List<User> getUsers() {
        return users;
    }

    /**
     * Returns the list of devices in the domain.
     * @return list of devices
     */
    protected List<Device> getDevices() {
        return devices;
    }

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
