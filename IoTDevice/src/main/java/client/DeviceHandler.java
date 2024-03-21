package client;

import java.io.*;
import java.net.Socket;

public class DeviceHandler {

    private static final String OK_RESPONSE = "OK";
    private static final String NODM = "NODM";
    private static final String NOUSER = "NOUSER";
    private static final String NOPERM = "NOPERM";
    private static final String ERROR_RESPONSE = "NOK";

    private final String address;
    private final int port;

    private ObjectOutputStream output;
    private ObjectInputStream input;

    // Socket connection to the server
    private Socket socket;

    public DeviceHandler(String address, int port) {
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
        switch (res) {
            case OK_RESPONSE -> System.out.println("Response: "
                    + OK_RESPONSE + " # Domain created successfully");
            case ERROR_RESPONSE -> System.out.println("Response: " + res
                    + " # Domain already exists");
            default -> System.out.println("Response: NOK # Error creating domain");
        }
    }

    public void sendReceiveADD(String[] args, String command) {
        if (args.length != 2) {
            System.out.println("Usage: ADD <user1> <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK_RESPONSE -> System.out.println("Response: "
                    + OK_RESPONSE + " # User added successfully");
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOUSER -> System.out.println("Response: " + res
                    + " # User does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error adding user");
        }
    }

    public void sendReceiveRD(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RD <dm>");
            return;
        }

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK_RESPONSE -> System.out.println("Response: "
                    + OK_RESPONSE + " # Device registered successfully");
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error registering device");
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
        String firstRes = this.sendReceive(msg);

        if (!firstRes.equals("Send image")) {
            System.out.println("Error sending image size");
            return;
        }

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
                Long fileSize = input.readLong();
                byte[] buffer = new byte[1024];
                int bytesRead;
                File file = new File("temperatures.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);
                long remainingBytes = fileSize;
                while (remainingBytes > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remainingBytes -= bytesRead;
                }

                fos.close();
                System.out.println("Resposta: " + OK_RESPONSE + ", " + fileSize + " (long), seguido de " + fileSize + " bytes de dados.");

            } catch (Exception e) {
                System.out.println(e.getMessage());
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

        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);

        if (res.equals(OK_RESPONSE)) {
            try {
                long imageSize = input.readLong();
                File file = new File("image.jpg");
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);

                long remainingBytes = imageSize;
                int bytesRead;

                byte[] buffer = new byte[1024];

                while (remainingBytes > 0 && (bytesRead = input.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remainingBytes -= bytesRead;
                }

                fos.flush();
                fos.close();

                System.out.println("Resposta: " + OK_RESPONSE + ", " + imageSize + " (long), seguido de " + imageSize + " Bytes de dados.");
            } catch (Exception e) {
                System.out.println("Error receiving image");
            }
        } else if (res.equals("NODATA")) {
            System.out.println("Resposta: " + res + " # esse device id não publicou dados");
        } else if (res.equals("NOID")) {
            System.out.println("Resposta: " + res + " # esse device id não existe");
        } else if (res.equals("NOPERM")) {
            System.out.println("Resposta: " + res + " # sem permissões de leitura");
        } else {
            System.out.println("Response: " + res + " # Error receiving image");
        }
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

            output.flush();
            fis.close();

            return (String) input.readObject();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;

    }
}
