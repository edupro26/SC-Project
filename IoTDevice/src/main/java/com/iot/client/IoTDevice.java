package com.iot.client;

import java.io.File;
import java.util.Scanner;

public class IoTDevice {

    private static final String EXEC = "IoTDevice-1.0-SNAPSHOT.jar";

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

        deviceLogIn(client, userId, devId);
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
            System.out.print("Password: ");
            String pw = scanner.nextLine();
            String logIn = client.sendReceive(userId + "," + pw);
            System.out.println("Response: " + logIn);
            if (logIn.equals("OK-USER") || logIn.equals("OK-NEW-USER")){
                while (true) {
                    System.out.println("Sending device ID to the server...");
                    String id = client.sendReceive(devId);
                    System.out.println("Response: " + id);
                    if(id.equals("NOK-DEVID")) {
                        System.out.print("Enter new ID: ");
                        devId = scanner.nextLine();
                    }
                    if(id.equals("OK-DEVID")){
                        System.out.println("Sending application size to the server...");
                        File exec = new File(EXEC);
                        String res;
                        if (exec.exists() && exec.isFile()) {
                            res = client.sendReceive(exec.getName() + "," + exec.length());
                            System.out.println("Response: " + res + "\n");
                            if (res.equals("NOK-TESTED")) {
                                System.exit(1);
                            }
                            if (res.equals("OK-TESTED")) {
                                return;
                            }
                        }
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
