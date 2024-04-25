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
     * Logs an info output message
     *
     * @param msg the message
     */
    public static void logInfo(String msg) {
        logger.info(msg);
    }

    /**
     * Logs a warning output message
     *
     * @param msg the message
     */
    public static void logWarning(String msg) {
        logger.warning(msg);
    }

    /**
     * Logs a warning output message and exits the program
     *
     * @param msg the message
     */
    public static void logWarningAndExit(String msg) {
        logWarning(msg);
        System.exit(1);
    }

    /**
     * Logs an error output message
     *
     * @param msg the message
     */
    public static void logError(String msg) {
        logger.severe(msg);
    }

    /**
     * Logs an error output message and exits the program
     *
     * @param msg the message
     */
    public static void logErrorAndExit(String msg) {
        logError(msg);
        System.exit(1);
    }

}
