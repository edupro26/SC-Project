package client;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

public class IoTDevice {

    private static final String EXEC = "IoTDevice-grupo6.jar";

    private IoTDevice() {}

    public static void main(String[] args) {
        String checkArgs = checkArgs(args);
        if (checkArgs != null) {
            System.out.println(checkArgs);
            System.exit(1);
        }

        String serverAddress = args[0];
        String devId = args[1];
        String userId = args[2];
        String[] server = serverAddress.split(":");

        NetworkDevice client = new NetworkDevice(server[0], Integer.parseInt(server[1]));
        try {
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
        } catch (Exception e) {
            System.out.println("\nExited IoTDevice ");
            client.disconnect();
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
                        try {
                            System.out.print("Enter new ID: ");
                            devId = scanner.nextLine();
                            Integer.parseInt(devId);
                        } catch (Exception e) {
                            System.out.println("Error: <dev-id> must be an Integer");
                            System.exit(1);
                        }
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

    private static String checkArgs(String[] args) {
        if (args.length < 3) {
            return "Usage: java -jar IoTDevice-grupo6.jar <serverAddress> <dev-id> <user-id>";
        }

        try {
            String[] address = args[0].split(":");
            if (address.length == 2) {
                try {
                    Integer.parseInt(address[1]);
                } catch (Exception e) {
                    return "Error: <IP/hostname>[:Port] Port must be an Integer";
                }
            }
            else {
                return "Error: <serverAddress> must be in the format <IP/hostname>[:Port]";
            }

            try {
                Integer.parseInt(args[1]);
            } catch (Exception e) {
                return "Error: <dev-id> must be an Integer";
            }

            try {
                Integer.parseInt(args[2]);
                return "Error: <user-id> can't be a Integer";
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            return "Error: <serverAddress> must be in the format <IP/hostname>[:Port]";
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
            case "CREATE" -> client.sendReceiveCREATE(args, command);
            case "ADD" -> client.sendReceiveADD(args, command);
            case "RD" -> client.sendReceiveRD(args, command);
            case "ET" -> client.sendReceiveET(args, command);
            case "EI" -> client.sendReceiveEI(args, command);
            case "RT" -> client.sendReceiveRT(args, command);
            case "RI" -> client.sendReceiveRI(args, command);
            default -> System.out.println("Invalid command");
        }
    }
}
