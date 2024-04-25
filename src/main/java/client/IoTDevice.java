package client;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Represents a {@code IoTDevice} capable of connecting to the {@code IoTServer}.
 * This class is responsible for presenting an interface, through which the user can
 * send data and requests from his {@code IoTDevice} to the {@code IoTServer}.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see DeviceHandler
 */
public class IoTDevice {

    private static final String EMAIL_REGEX = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

    /**
     * This class is not meant to be constructed
     */
    private IoTDevice() {}

    /**
     * Main routine of this IoTDevice.
     *
     * @param args the arguments given when executed
     */
    public static void main(String[] args) {
        String checkArgs = checkArgs(args);
        if (checkArgs != null) {
            System.out.println(checkArgs);
            System.exit(1);
        }

        String serverAddress = args[0];
        String truststore = URLDecoder.decode(args[1], StandardCharsets.UTF_8);
        String keystore = URLDecoder.decode(args[2], StandardCharsets.UTF_8);
        String passwordKeystore = args[3];
        String devId = args[4];
        String userId = args[5];
        String[] server = serverAddress.split(":");

        // Validate userId as an email address
        Pattern emailPattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
        if (!emailPattern.matcher(userId).matches()) {
            System.out.println("Error: <user-id> must be a valid email address");
            System.exit(1);
        }

        System.setProperty("javax.net.ssl.trustStore", truststore);
        System.setProperty("javax.net.ssl.trustStorePassword", passwordKeystore);
        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", passwordKeystore);

        DeviceHandler client = new DeviceHandler(server[0], Integer.parseInt(server[1]));
        try {
            client.connect(userId);
            client.deviceValidation(devId);
            printMenu();

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Command: ");

                String msg = scanner.nextLine();
                handleCommand(client, msg);
            }
        } catch (Exception e) {
            System.out.println("\nExited IoTDevice ");
            client.disconnect();
        }
    }

    /**
     * Validates the arguments given when executing this IoTDevice.
     *
     * @param args the arguments given when executing this IoTDevice
     * @return error message if something is wrong with the arguments,
     *         null otherwise
     */
    private static String checkArgs(String[] args) {
        if (args.length != 6) {
            return "Usage: java -jar IoTDevice.jar <serverAddress> <truststore> <keystore> <passwordkeystore> <dev-id> <user-id>";
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
                Integer.parseInt(args[4]);
            } catch (Exception e) {
                return "Error: <dev-id> must be an Integer";
            }

            try {
                Integer.parseInt(args[5]);
                return "Error: <user-id> can't be a Integer";
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            return "Error: <serverAddress> must be in the format <IP/hostname>[:Port]";
        }
    }

    /**
     * Prints the command interface.
     */
    private static void printMenu() {
        System.out.println("""
                \nCREATE <dm>
                ADD <user1> <dm> <password-domain>
                RD <dm>
                ET <float>
                EI <filename.jpg>
                RT <dm>
                RI <user-id>:<dev_id>
                \s""");
    }

    /**
     * Handles the command chosen by the user input.
     *
     * @param handler handler used for communication with the {@code IoTServer}
     * @param input the input given by the user of this IoTDevice
     */
    private static void handleCommand(DeviceHandler handler, String input) {
        String[] parsedCommand = input.split(" ");
        String command = parsedCommand[0];
        String[] args = Arrays.copyOfRange(parsedCommand, 1, parsedCommand.length);

        switch (command) {
            case "CREATE" -> handler.sendReceiveCREATE(args, command);
            case "ADD" -> handler.sendReceiveADD(args, command);
            case "RD" -> handler.sendReceiveRD(args, command);
            case "MYDOMAINS" -> handler.sendReceiveMYDOMAINS(args, command);
            case "ET" -> handler.sendReceiveET(args, command);
            case "EI" -> handler.sendReceiveEI(args, command);
            case "RT" -> handler.sendReceiveRT(args, command);
            case "RI" -> handler.sendReceiveRI(args, command);
            default -> System.out.println("Response: NOK # Invalid command");
        }
    }

}
