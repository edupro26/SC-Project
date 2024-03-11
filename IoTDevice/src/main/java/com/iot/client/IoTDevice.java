package com.iot.client;

import java.util.Scanner;

public class IoTDevice {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar IoTDevice.jar <serverAddress> <dev-id> <user-id>");
            return;
        }

        String serverAddress = args[0];
        String devId = args[1];
        String userId = args[2];

        String[] server = serverAddress.split(":");

        NetworkDevice client = new NetworkDevice(server[0], Integer.parseInt(server[1]));

        client.connect();
        handler(client);
    }

    private static void handler(NetworkDevice client) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // Ask user for input
            System.out.print("Enter a message: ");
            String msg = scanner.nextLine();

            // Send the message to the server
            String response = client.SendReceive(msg);
            System.out.println("Server response: " + response);
        }
    }
}
