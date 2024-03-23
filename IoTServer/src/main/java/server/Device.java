package server;

import java.util.Objects;

public class Device {

    private final String user;
    private int id;
    private boolean isConnected;
    private Float lastTemp;

    protected Device(String user, int id) {
        this.user = user;
        this.id = id;
        isConnected = false;
        lastTemp = null;
    }

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

    public void setId(int id) {
        this.id = id;
    }

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
