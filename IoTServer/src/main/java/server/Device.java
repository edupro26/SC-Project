package server;

public class Device {

    private final String user;
    private final int id;
    private boolean isConnected;
    private Float lastTemp;

    protected Device(String user, int id) {
        this.user = user;
        this.id = id;
        isConnected = false;
        lastTemp = null;
    }

    protected Device(String device) {
        String[] deviceArgs = device.split(":");
        this.user = deviceArgs[0];
        this.id = Integer.parseInt(deviceArgs[1]);
        isConnected = false;
        lastTemp = null;
    }

    protected String getUser() {
        return user;
    }

    protected int getId() {
        return id;
    }

    protected boolean isConnected() {
        return isConnected;
    }

    protected Float getLastTemp() {
        return lastTemp;
    }

    protected void setConnected(boolean connected) {
        isConnected = connected;
    }

    protected void setLastTemp(Float lastTemp) {
        this.lastTemp = lastTemp;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO
        return false;
    }

    @Override
    public String toString() {
        return user + ":" + id;
    }
}
