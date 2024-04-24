package server;

import java.util.logging.Logger;

/**
 * Simple logger utility class. Logs output
 * messages from the server to the terminal
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 */
public class ServerLogger {

    /**
     * Logger instance
     */
    private static final Logger logger = Logger.getLogger("Logger");

    /**
     * Utility class not meant to be constructed
     */
    private ServerLogger() {}

    /**
     * Log an info output message
     *
     * @param msg the message
     */
    public static void logInfo(String msg) {
        logger.info(msg);
    }

    /**
     * Log a warning output message
     *
     * @param msg the message
     */
    public static void logWarning(String msg) {
        logger.warning(msg);
    }

    /**
     * Log an error output message
     *
     * @param msg the message
     */
    public static void logError(String msg) {
        logger.severe(msg);
    }

}
