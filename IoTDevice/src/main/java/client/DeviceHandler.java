package client;

import java.io.*;
import java.net.Socket;

public class DeviceHandler {

    private static final String OK = "OK";
    private static final String NODM = "NODM";
    private static final String NOID = "NOID";
    private static final String NOUSER = "NOUSER";
    private static final String NOPERM = "NOPERM";
    private static final String NODATA = "NODATA";
    private static final String NOK = "NOK";

    private static final String SERVER_OUT = "server-output/";

    private final String address;
    private final int port;

    private ObjectOutputStream output;
    private ObjectInputStream input;

    // Socket connection to the server
    private Socket socket;

    protected DeviceHandler(String address, int port) {
        this.address = address;
        this.port = port;
    }

    protected void connect() {
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

    protected void disconnect() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    protected String sendReceive(String msg) {
        try {
            this.output.writeObject(msg);

            return (String) this.input.readObject();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    protected void sendReceiveCREATE(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: CREATE <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK -> System.out.println("Response: "
                    + OK + " # Domain created successfully");
            case NOK -> System.out.println("Response: " + res
                    + " # Domain already exists");
            default -> System.out.println("Response: NOK # Error creating domain");
        }
    }

    protected void sendReceiveADD(String[] args, String command) {
        if (args.length != 2) {
            System.out.println("Usage: ADD <user1> <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK -> System.out.println("Response: "
                    + OK + " # User added successfully");
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOUSER -> System.out.println("Response: " + res
                    + " # User does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error adding user");
        }
    }

    protected void sendReceiveRD(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RD <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        switch (res) {
            case OK -> System.out.println("Response: "
                    + OK + " # Device registered successfully");
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error registering device");
        }
    }

    protected void sendReceiveET(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: ET <float>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK)) {
            System.out.println("Response: " + OK +
                    " # Temperature sent successfully");
        } else {
            System.out.println("Response: " + res + " # Error sending temperature");
        }
    }

    protected void sendReceiveEI(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: EI <filename.jpg>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        if (res.equals(OK)) {
            String result = sendFile(args[0]) ? "Response: " + OK + " # Image " +
                    "sent successfully" : "Response: " + NOK + " # Error sending image";
            System.out.println(result);
        } else {
            System.out.println("Response: " + res + " # Error sending image");
        }
    }

    protected void sendReceiveRT(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RT <dm>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        String name = SERVER_OUT + args[0] + ".txt";
        switch (res) {
            case OK -> {
                int received = receiveFile(name);
                System.out.println("Response: " + res + ", " + received
                        + " (long), followed by " + received + " bytes of data");
            }
            case NODM -> System.out.println("Response: " + res
                    + " # Domain does not exist");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            case NODATA -> System.out.println("Response: " + res
                    + " # No data found in this domain");
            default -> System.out.println("Response: NOK # Error getting temperatures");
        }
    }

    protected void sendReceiveRI(String[] args, String command) {
        if (args.length != 1) {
            System.out.println("Usage: RI <user-id>:<dev_id>");
            return;
        }
        String msg = parseCommandToSend(command, args);
        String res = this.sendReceive(msg);
        String[] temp = args[0].split(":");
        String name = SERVER_OUT + temp[0] + "_" + temp[1] + ".jpg";
        switch (res) {
            case OK -> {
                int received = receiveFile(name);
                System.out.println("Response: " + res + ", " + received
                        + " (long), followed by " + received + " bytes of data");
            }
            case NODATA -> System.out.println("Response: " + res
                    + " # No image found for this device");
            case NOID -> System.out.println("Response: " + res
                    + " # No device found with this id");
            case NOPERM -> System.out.println("Response: " + res
                    + " # This user does not have permissions");
            default -> System.out.println("Response: NOK # Error getting image");
        }
    }

    private boolean sendFile(String filePath) {
        try {
            File image = new File(filePath);
            FileInputStream in = new FileInputStream(image);
            BufferedInputStream bis = new BufferedInputStream(in);
            byte[] buffer = new byte[8192];
            int bytesLeft = (int) image.length();
            while (bytesLeft > 0) {
                int bytesRead = bis.read(buffer);
                output.write(buffer, 0, bytesRead);
                bytesLeft -= bytesRead;
            }
            output.flush();
            bis.close();
            in.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    private int receiveFile(String name) {
        File outputFolder = new File(SERVER_OUT);
        if (!outputFolder.isDirectory()) outputFolder.mkdir();
        try {
            int size = input.readInt();
            FileOutputStream out = new FileOutputStream(name);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            byte[] buffer = new byte[8192];
            int bytesLeft = size;
            while (bytesLeft > 0) {
                int bytesRead = input.read(buffer);
                bos.write(buffer, 0, bytesRead);
                bytesLeft -= bytesRead;
            }
            bos.flush();
            bos.close();
            out.close();
            return size;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    private String parseCommandToSend(String command, String[] args) {
        StringBuilder sb = new StringBuilder(command);
        for (String arg : args) {
            sb.append(";").append(arg);
        }

        return sb.toString();
    }

}
