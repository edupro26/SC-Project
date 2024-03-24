package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Domain {

    private final String name;
    private final User owner;
    private final List<User> users;
    private final List<Device> devices;

    protected Domain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.users = new ArrayList<>();
        this.devices = new ArrayList<>();
    }

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

    protected File getDomainTemperatures() {
        File file = new File(name + ".txt");
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

    protected void addUser(User user) {
        users.add(user);
    }

    protected void addDevice(Device device) {
        devices.add(device);
    }

    protected String getName() {
        return name;
    }

    protected User getOwner() {
        return owner;
    }

    protected List<User> getUsers() {
        return users;
    }

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
