package server;

import java.util.ArrayList;
import java.util.List;

public class ServerDomain {

    private final User owner;
    private final List<User> canRead;
    private final List<ServerConnection> devices;

    public ServerDomain(User owner) {
        this.owner = owner;
        this.canRead = new ArrayList<>();
        this.devices = new ArrayList<>();
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

    // TODO finish implementation
    //  Create a file in ServerStorage to save domains like the file
    //  created for users (não tenho a certeza se é isto que é pedido)

}
