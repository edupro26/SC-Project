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

    protected ServerDomain(String domain) {
        String[] split = domain.split(",");
        this.name = split[0];
        this.owner = ServerStorage.searchUser(split[1]);
        this.canRead = new ArrayList<>();
        this.devices = new ArrayList<>();

        StringBuilder users = new StringBuilder(split[2]);
        users.deleteCharAt(split[2].indexOf("{"));
        users.deleteCharAt(split[2].indexOf("}") - 1);
        String[] temp = users.toString().split(";");
        for (String user: temp)
            this.canRead.add(ServerStorage.searchUser(user));

        if(!split[3].equals("{}")){
            StringBuilder devices = new StringBuilder(split[3]);
            devices.deleteCharAt(split[2].indexOf("{"));
            devices.deleteCharAt(split[2].indexOf("}") - 1);
            temp = devices.toString().split(";");
            for (String device : temp) {
                split = device.split(":");
                ServerConnection canRead = ServerStorage.searchDevice(split[0], Integer.parseInt(split[1]));
                if (canRead != null)
                    this.devices.add(canRead);
            }
        }
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

    protected String[] getDomainTemperatures() {
        String[] temperatures = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            temperatures[i] = devices.get(i) + "->" + devices.get(i).getLastTemperature();
        }
        return temperatures;
    }

    @Override
    public String toString() {
        StringBuilder users = new StringBuilder();
        users.append("{");
        for (User user : this.canRead) {
            users.append(user.getUsername()).append(";");
        }
        users.deleteCharAt(users.length() - 1);
        users.append("}");

        StringBuilder devices = new StringBuilder();
        if (!this.devices.isEmpty()) {
            devices.append("{");
            for (ServerConnection device : this.devices){
                devices.append(device.toString()).append(";");
            }
            devices.deleteCharAt(devices.length() - 1);
            devices.append("}");
            return name + "," + owner.getUsername() + "," + users + "," + devices;
        }
        else {
            return name + "," + owner.getUsername() + "," + users + "," + "{}";
        }

    }
}
