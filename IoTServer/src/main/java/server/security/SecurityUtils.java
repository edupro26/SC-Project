package server.security;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

public final class SecurityUtils {
    private static final String API_URL = "https://lmpinto.eu.pythonanywhere.com/2FA";
    private static HttpClient client = HttpClient.newHttpClient();

    /**
     * Generates a symmetric key given a cipher-password.
     *
     * @param cipherPassword the password to be used to generate the key
     * @return the generated key
     */
    public static SecretKey generateKey(String cipherPassword) {
        // TODO: Make salt secure
        byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };

        PBEKeySpec keySpec = new PBEKeySpec(cipherPassword.toCharArray(), salt, 20);
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
            return kf.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static void encryptDataIntoFile(String data, File file, SecretKey key) {
        try {
            // Create a Cipher instance and initialize it for encryption using the provided key.
            Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Convert the plaintext data to a byte array.
            byte[] inputBytes = data.getBytes();

            // Set up a FileOutputStream to write the encrypted data to a file.
            try (FileOutputStream fos = new FileOutputStream(file);
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                // Write the encrypted data to the file.
                cos.write(inputBytes);
            }
        } catch (Exception e) {
            // Handle exceptions such as NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException
            e.printStackTrace();
        }
    }

    public static String decryptDataFromFile(File file, SecretKey key) {
        try {
            // Create a Cipher instance and initialize it for decryption using the provided key.
            Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            cipher.init(Cipher.DECRYPT_MODE, key);

            // Set up a FileInputStream to read the encrypted data from the file.
            try (FileInputStream fis = new FileInputStream(file);
                 CipherInputStream cis = new CipherInputStream(fis, cipher);
                 Scanner scanner = new Scanner(cis)) {
                // Read the encrypted data from the file and convert it to a String.
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            // Handle exceptions such as NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends a 2FA code to the user's email.
     *
     * @param code the 2FA code to be sent
     * @param email the email to which the code will be sent
     * @param apiKey the API key to be used
     * @return true if the code was sent successfully, false otherwise
     */
    public static boolean send2FACode(String code, String email, String apiKey) {
        // Add parameters to the URL
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?e=" + email + "&c=" + code + "&a=" + apiKey))
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
