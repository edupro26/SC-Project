package client;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

public class IoTDevice {

    private static final String EXEC = "IoTDevice-grupo6.jar";

    private static final String OK_RESPONSE = "OK";

    private IoTDevice() {}

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar IoTDevice-grupo6.jar <serverAddress> <dev-id> <user-id>");
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

            handleCommand(client, msg);
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


    private static void handleCommand(NetworkDevice client, String input) {
        String[] parsedCommand = input.split(" ");
        String command = parsedCommand[0];

        String[] args = Arrays.copyOfRange(parsedCommand, 1, parsedCommand.length);

        switch (command) {
            case "CREATE" -> {
                if (args.length != 1) {
                    System.out.println("Usage: CREATE <dm>");
                    return;
                }

                String msg = parseCommandToSend(command, args);

                String res = client.sendReceive(msg);

                if (res.equals(OK_RESPONSE)) {
                    System.out.println("Domain created successfully");
                } else {
                    System.out.println("Error creating domain");
                }

            }
            case "ADD" -> {
                if (args.length != 2) {
                    System.out.println("Usage: ADD <user1> <dm>");
                    return;
                }

                String msg = parseCommandToSend(command, args);

                String res = client.sendReceive(msg);

                if (res.equals(OK_RESPONSE)) {
                    System.out.println("User added successfully");
                } else {
                    System.out.println("Error adding user");
                }
            }
            case "RD" -> {
                if (args.length != 1) {
                    System.out.println("Usage: RD <dm>");
                    return;
                }

                String msg = parseCommandToSend(command, args);

                String res = client.sendReceive(msg);

                if (res.equals(OK_RESPONSE)) {
                    System.out.println("Device registered successfully");
                } else {
                    System.out.println("Error registering device");
                }
            }
            case "ET" -> {
                if (args.length != 1) {
                    System.out.println("Usage: ET <float>");
                    return;
                }

                String msg = parseCommandToSend(command, args);

                String res = client.sendReceive(msg);

                if (res.equals(OK_RESPONSE)) {
                    System.out.println("Temperature sent successfully");
                } else {
                    System.out.println("Error sending temperature");
                }
            }
            case "EI" -> {
                if (args.length != 1) {
                    System.out.println("Usage: EI <filename.jpg>");
                    return;
                }

                // TODO Send the file to the server

                /*
                if (res.equals(OK_RESPONSE)) {
                    System.out.println("Image sent successfully");
                } else {
                    System.out.println("Error sending image");
                 */
            }
            case "RT" -> {
                if (args.length != 1) {
                    System.out.println("Usage: RT <dm>");
                    return;
                }
                String msg = parseCommandToSend(command, args);

                // TODO Print the temperature values received from the server
            }
            case "RI" -> {
                if (args.length != 1) {
                    System.out.println("Usage: RI <user-id>:<dev_id>");
                    return;
                }

                // TODO Receive the image from the server

            }
            default -> System.out.println("Invalid command");
        }
    }

    private static String parseCommandToSend(String command, String[] args) {
        StringBuilder sb = new StringBuilder(command);

        for (String arg : args) {
            sb.append(";").append(arg);
        }

        return sb.toString();
    }
}
