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
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

public final class SecurityUtils {
    private static final String API_URL = "https://lmpinto.eu.pythonanywhere.com/2FA";
    private static HttpClient client = HttpClient.newHttpClient();

    // TODO: Make salt secure
    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };

    private static final int ITERATION_COUNT = 20;

    private static final File PARAMS_FILE = new File("params.txt");

    /**
     * Generates a symmetric key given a cipher-password.
     *
     * @param cipherPassword the password to be used to generate the key
     * @return the generated key
     */
    public static SecretKey generateKey(String cipherPassword) {


        PBEKeySpec keySpec = new PBEKeySpec(cipherPassword.toCharArray(), salt, ITERATION_COUNT);
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
            Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] inputBytes = data.getBytes();

            try (FileOutputStream fos = new FileOutputStream(file);
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

                cos.write(inputBytes);
            }

            saveParams(cipher.getParameters().getEncoded());


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String decryptDataFromFile(File file, SecretKey key) {
        try {
            AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
            p.init(readParams());
            Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
            cipher.init(Cipher.DECRYPT_MODE, key, p);

            try (FileInputStream fis = new FileInputStream(file);
                 CipherInputStream cis = new CipherInputStream(fis, cipher);
                 Scanner scanner = new Scanner(cis)) {
                // Read the encrypted data from the file and convert it to a String.
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {

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

    public static void saveParams(byte[] params) {
        try (FileOutputStream fos = new FileOutputStream(PARAMS_FILE)) {
            fos.write(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] readParams() {
        try (FileInputStream fis = new FileInputStream(PARAMS_FILE)) {
            byte[] params = new byte[(int) PARAMS_FILE.length()];
            fis.read(params);
            return params;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
