package com.iot.server;

public class IoTServer {
  public static void main(String[] args) {

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;

        NetworkServer server = new NetworkServer(port);

        server.start();


    }
}
