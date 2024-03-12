package com.iot.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkDevice {

    private final String address;
    private final int port;

    private ObjectOutputStream output;
    private ObjectInputStream input;

    // Socket connection to the server
    private Socket socket;

    public NetworkDevice(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void connect() {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected to server: " + address + ":" + port);

            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public String sendReceive(String msg) {
        try {

            this.output.writeObject(msg);

            return (String) this.input.readObject();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
