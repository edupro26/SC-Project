package server;

public class IoTServer {

  public static void main(String[] args) {

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
        new NetworkServer(port).start();

    }

}
