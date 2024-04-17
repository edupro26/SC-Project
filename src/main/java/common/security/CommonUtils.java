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
            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            md.update(nonceToByteArray(nonce));
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] hash = md.digest();
            inputStream.close();
            return hash;
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static boolean compareHashes(byte[] client, byte[] server) {
        return MessageDigest.isEqual(client, server);
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
