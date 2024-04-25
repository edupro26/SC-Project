package common.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class with methods use for device validation
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 */
public class CommonUtils {

    /**
     * Hash algorithm
     */
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Calculates the hash of the concatenation between
     * a file and a long
     *
     * @param file the file
     * @param nonce the long
     * @return the hash in the form of a byte array
     */
    public static byte[] calculateHashWithNonce(File file, long nonce) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] fileArray = fileToByteArray(file);
            byte[] nonceArray = nonceToByteArray(nonce);
            md.update(fileArray);
            md.update(nonceArray);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Compares the hash sent from the client
     * with the hash of the server
     *
     * @param client the client hash
     * @param server the server hash
     * @return true if equal, false otherwise
     */
    public static boolean compareHashes(byte[] client, byte[] server) {
        return MessageDigest.isEqual(client, server);
    }

    /**
     * Converts a file to a byte array
     *
     * @param file the file to be converted
     * @return a byte array or null in case of error
     */
    private static byte[] fileToByteArray(File file) {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            in.read(buffer);
        } catch (IOException e) {
            return null;
        }
        return buffer;
    }

    /**
     * Converts a long to a byte array
     *
     * @param nonce the long to be converted
     * @return a byte array
     */
    private static byte[] nonceToByteArray(long nonce) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (nonce & 0xFF);
            nonce >>= 8;
        }
        return array;
    }

}
