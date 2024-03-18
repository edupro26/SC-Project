package server;

import java.util.ArrayList;
import java.util.List;

public class ServerDomain {

    private final String name;
    private final User owner;
    private final List<User> canRead;
    private final List<ServerConnection> devices;

    public ServerDomain(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.canRead = new ArrayList<>();
        this.addUser(owner);
        this.devices = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public List<User> getCanRead() {
        return canRead;
    }

    public List<ServerConnection> getDevices() {
        return devices;
    }

    public void addUser(User user) {
        canRead.add(user);
    }

    public void addDevice(ServerConnection device) {
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
