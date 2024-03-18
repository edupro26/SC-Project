package server;

import java.util.ArrayList;
import java.util.List;

public class ServerDomain {

    private final String name;
    private final User owner;
    private final List<User> canRead;
    private final List<ServerConnection> devices;

    protected ServerDomain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.canRead = new ArrayList<>();
        this.addUser(owner);
        this.devices = new ArrayList<>();
    }

    protected String getName() {
        return name;
    }

    protected User getOwner() {
        return owner;
    }

    protected List<User> getCanRead() {
        return canRead;
    }

    protected List<ServerConnection> getDevices() {
        return devices;
    }

    protected void addUser(User user) {
        canRead.add(user);
    }

    protected void addDevice(ServerConnection device) {
        devices.add(device);
    }

    @Override
    public String toString() {
        // TODO get usernames and implement a
        //  toString() for ServerConnection
        String users = null;
        String devices = null;
        return name + "," + owner.getUsername() + "," + users + "," + devices;
    }
}
