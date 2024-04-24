package server;

import java.util.logging.Logger;


public class ServerLogger {

    private static final Logger logger = Logger.getLogger("Logger");

    private ServerLogger() {}

    public static void logInfo(String msg) {
        logger.info(msg);
    }

    public static void logWarning(String msg) {
        logger.warning(msg);
    }

    public static void logError(String msg) {
        logger.severe(msg);
    }

}
