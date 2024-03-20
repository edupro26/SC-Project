package server;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ServerDomain {

    private final String name;
    private final User owner;
    private final List<User> users;
    private final List<ServerConnection> devices;

    protected ServerDomain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.users = new ArrayList<>();
        this.addUser(owner); //TODO decide whether to remove this or not
        this.devices = new ArrayList<>();
    }

    protected ServerDomain(String domain) {
        String[] domainParts = domain.split(",");
        this.name = domainParts[0];
        this.owner = ServerStorage.searchUser(domainParts[1]);
        this.users = new ArrayList<>();
        this.devices = new ArrayList<>();

        String[] users = domainParts[2].substring(1, domainParts[2].length() - 1).split(";");
        for (String user : users)
            this.users.add(ServerStorage.searchUser(user));

        if (!domainParts[3].equals("{}")) {
            String[] devices = domainParts[3].substring(1, domainParts[3].length() - 1).split(";");
            for (String device : devices) {
                String[] deviceParts = device.split(":");
                // FIXME searchDevice is not working here, because when
                //  the server restarts there are no ServerConnections active
                //  I think this is also causing an error with the temp_domains.csv
                ServerConnection domainDevice = ServerStorage.searchDevice(deviceParts[0],
                        Integer.parseInt(deviceParts[1]));
                if (domainDevice != null) {
                    this.devices.add(domainDevice);
                }
            }
        }
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

    protected List<ServerConnection> getDevices() {
        return devices;
    }

    protected void addUser(User user) {
        users.add(user);
    }

    protected void addDevice(ServerConnection device) {
        devices.add(device);
    }

    protected String[] getDomainTemperatures() {
        String[] temperatures = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            temperatures[i] = devices.get(i) + "->" + devices.get(i).getLastTemperature();
        }
        return temperatures;
    }

    @Override
    public String toString() {
        StringJoiner userJoiner = new StringJoiner(";");
        for (User user : this.users) {
            userJoiner.add(user.getUsername());
        }
        String user = "{" + userJoiner + "}";
        String devices;
        if (!this.devices.isEmpty()) {
            StringJoiner deviceJoiner = new StringJoiner(";");
            for (ServerConnection device : this.devices) {
                deviceJoiner.add(device.toString());
            }
            devices = "{" + deviceJoiner + "}";
        }
        else {
            devices = "{}";
        }
        return name + "," + owner.getUsername() + "," + user + "," + devices;
    }
}
