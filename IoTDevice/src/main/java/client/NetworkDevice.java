package client;

import java.io.*;
import java.net.Socket;

public class NetworkDevice {

    private static final String OK_RESPONSE = "OK";

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
            System.exit(1);
        }
    }

    public void disconnect() {
        try {
            output.close();
            input.close();
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

    public void sendReceiveCREATE(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: CREATE <dm>");
            return;
        }

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK_RESPONSE)) {
            System.out.println("Response: " + OK_RESPONSE
                    + " # Domain created successfully");
        } else {
            System.out.println("Error creating domain");
        }
    }

    public void sendReceiveADD(String[] args, String command) {
        if (args.length != 2) {
            System.out.println("Usage: ADD <user1> <dm>");
            return;
        }

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK_RESPONSE)) {
            System.out.println("Response: " + OK_RESPONSE +
                    " # User added successfully");
        } else {
            System.out.println("Error adding user");
        }
    }

    public void sendReceiveRD(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RD <dm>");
            return;
        }

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK_RESPONSE)) {
            System.out.println("Response: " + OK_RESPONSE +
                    " # Device registered successfully");
        } else {
            System.out.println("Error registering device");
        }
    }

    public void sendReceiveET(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: ET <float>");
            return;
        }

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK_RESPONSE)) {
            System.out.println("Response: " + OK_RESPONSE +
                    " # Temperature sent successfully");
        } else {
            System.out.println("Response: " + res +
                    " # Error sending temperature");
        }
    }

    public void sendReceiveEI(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: EI <filename.jpg>");
            return;
        }

        // Send command to server with the file size
        File file = new File(args[0]);
        String msg = parseCommandToSend(command, new String[]{String.valueOf(file.length())});

        // Send the command to the server to warn it will receive a file
        this.sendReceive(msg);

        // Send the file to the server
        String res = sendFileToServer(file);

        if (res != null && res.equals(OK_RESPONSE)) {
            System.out.println("Image sent successfully");
        } else {
            System.out.println("Error sending image");
        }
    }

    public void sendReceiveRT(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RT <dm>");
            return;
        }

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK_RESPONSE)) {
            try {
                Long fileSize = Long.parseLong((String) input.readObject());
                byte[] buffer = new byte[1024];
                int bytesRead;
                File file = new File("temperatures.txt");
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);

                while (fileSize > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }

                fos.close();
                // Resposta: OK, 45 (long), seguido de 45 bytes de dados.
                System.out.println("Resposta: " + OK_RESPONSE + ", " + fileSize + " (long), seguido de " + fileSize + " bytes de dados.");

            } catch (Exception e) {
                System.out.println("Error receiving temperatures");
            }
        } else {
            System.out.println("Response: " + res +
                    " # Error receiving temperatures");
        }

    }

    public void sendReceiveRI(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RI <user-id>:<dev_id>");
            return;
        }
        // TODO Receive the image from the server
    }

    private String parseCommandToSend(String command, String[] args) {
        StringBuilder sb = new StringBuilder(command);

        for (String arg : args) {
            sb.append(";").append(arg);
        }

        return sb.toString();
    }

    private String sendFileToServer(File file) {
        try {

            FileInputStream fis = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            fis.close();

            return (String) input.readObject();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;

    }
}
