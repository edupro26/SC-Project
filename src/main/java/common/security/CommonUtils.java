package common.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CommonUtils {

    private static final String HASH_ALGORITHM = "SHA-256";

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

    public static boolean compareHashes(byte[] client, byte[] server) {
        return MessageDigest.isEqual(client, server);
    }

    private static byte[] fileToByteArray(File file) {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            in.read(buffer);
        } catch (IOException e) {
            return null;
        }
        return buffer;
    }

    private static byte[] nonceToByteArray(long nonce) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (nonce & 0xFF);
            nonce >>= 8;
        }
        return array;
    }

}
