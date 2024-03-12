package com.iot.client;

import java.util.Scanner;

public class IoTDevice {

    private IoTDevice() {}

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

        // TODO
        //deviceLogIn(client, userId, devId);
        printMenu();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            // Ask user for input
            System.out.print("Command: ");
            String msg = scanner.nextLine();

            // Send the message to the server
            String response = client.sendReceive(msg);
            System.out.println("Response: " + response);
        }
    }

    private static void deviceLogIn(NetworkDevice client, String userId, String devId) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Password: ");
            // TODO send user to server
            String pw = client.sendReceive(scanner.nextLine());
            System.out.println("Response: " + pw);
            if (pw.equals("OK-USER") || pw.equals("OK-NEW-USER")){
                while (true) {
                    System.out.println("Sending device ID to the server...");
                    String id = client.sendReceive(devId);
                    System.out.println("Response: " + id);
                    if(id.equals("NOK-DEVID")) {
                        System.out.println("Enter new ID: ");
                        devId = scanner.nextLine();
                    }
                    if(id.equals("OK-DEVID")){
                        return;
                    }
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("""
                CREATE <dm>
                ADD <user1> <dm>
                RD <dm>
                ET <float>
                ET <filename.jpg>
                RT <dm>
                RI <user-id>:<dev_id>
                \s""");
    }
}
