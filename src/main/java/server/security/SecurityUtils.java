package server.security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AlgorithmParameters;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

/**
 * Utility class with security
 * methods used by the server
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 */
public class SecurityUtils {

    /**
     * The API url
     */
    private static final String API_URL = "https://lmpinto.eu.pythonanywhere.com/2FA";

    /**
     * Algorithms
     */
    private static final String ENC_ALGORITHM = "PBEWithHmacSHA256AndAES_128";
    private static final String SIG_ALGORITHM = "SHA256withRSA";

    /**
     * Parameters for {@code SecretKey} generation
     */
    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78,
                                        (byte) 0x99, (byte) 0x52, (byte) 0x3e,
                                        (byte) 0xea, (byte) 0xf2 };
    private static final int ITERATION_COUNT = 20;

    /**
     * File to store encryption parameters
     */
    private static final File PARAMS_FILE = new File("server/params.txt");

    /**
     * A {@code HttpClient}
     */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Utility class should not be constructed
     */
    private SecurityUtils() {}

    /**
     * Generates a symmetric key given a cipher-password.
     *
     * @param cipherPassword the password to be used to generate the key
     * @return the generated key
     */
    public static SecretKey generateKey(String cipherPassword) {
        PBEKeySpec keySpec = new PBEKeySpec(
                cipherPassword.toCharArray(), salt, ITERATION_COUNT);
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance(ENC_ALGORITHM);
            return kf.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Encrypts a {@code String} of data and writes it to a file
     *
     * @param data the data to be encrypted
     * @param file the file to write to
     * @param key the {@code SecretKey}
     */
    public static void encryptDataIntoFile(String data, File file, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] inputBytes = data.getBytes();
            try (FileOutputStream fos = new FileOutputStream(file);
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

                cos.write(inputBytes);
            }
            saveParams(cipher.getParameters().getEncoded());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Decrypts data from a encryted file
     *
     * @param file the encrypted file
     * @param key the {@code SecretKey}
     * @return a string with the decryted data
     */
    public static String decryptDataFromFile(File file, SecretKey key) {
        try {
            AlgorithmParameters p = AlgorithmParameters.getInstance(ENC_ALGORITHM);
            p.init(readParams());
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, p);

            try (FileInputStream fis = new FileInputStream(file);
                 CipherInputStream cis = new CipherInputStream(fis, cipher);
                 Scanner scanner = new Scanner(cis)) {
                // Read the encrypted data from the file and convert it to a String.
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Saves the {@code Cipher} parameters to the file params.txt
     *
     * @param params the parameters
     */
    private static void saveParams(byte[] params) {
        try (FileOutputStream fos = new FileOutputStream(PARAMS_FILE)) {
            fos.write(params);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Reads the {@code Cipher} parameters from the file params.txt
     *
     * @return the parameters or null in case of error
     */
    private static byte[] readParams() {
        try (FileInputStream fis = new FileInputStream(PARAMS_FILE)) {
            byte[] params = new byte[(int) PARAMS_FILE.length()];
            fis.read(params);
            return params;
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
        String params = "?e=" + email + "&c=" + code + "&a=" + apiKey;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + params))
                .build();
        try {
            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() >= 200 && res.statusCode() < 300;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Gets the server {@code PublicKey}
     *
     * @return the server {@code PublicKey} or null in case of error
     */
    public static PublicKey getPublicKey() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    System.getProperty("javax.net.ssl.keyStore")),
                    System.getProperty("javax.net.ssl.keyStorePassword")
                            .toCharArray());

            return ks.getCertificate("ServerKeyPair").getPublicKey();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Gets the server {@code PrivateKey}
     *
     * @return the server {@code PrivateKey} or null in case of error
     */
    public static PrivateKey getPrivateKey() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    System.getProperty("javax.net.ssl.keyStore")),
                    System.getProperty("javax.net.ssl.keyStorePassword")
                            .toCharArray());

            return (PrivateKey) ks.getKey("ServerKeyPair",
                    System.getProperty("javax.net.ssl.keyStorePassword")
                            .toCharArray());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Signs a file with the server's private key.
     *
     * @param file the file to be signed
     * @param data the data to be signed
     */
    public static void signFile(File file, String data) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            PrivateKey privateKey = getPrivateKey();
            Signature signature = Signature.getInstance(SIG_ALGORITHM);
            signature.initSign(privateKey);
            byte[] buffer = data.getBytes();
            signature.update(buffer);
            oos.writeObject(data);
            oos.writeObject(signature.sign());
            fos.close();
            oos.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Verifies the signature of a file.
     *
     * @param file the file to be verified
     * @return the data if the signature is valid, null otherwise
     */
    public static String verifySignature(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            String data = (String) ois.readObject();
            byte[] signature = (byte[]) ois.readObject();
            PublicKey publicKey = getPublicKey();
            Signature sig = Signature.getInstance(SIG_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data.getBytes());
            if (sig.verify(signature)) {
                return data;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifies the signature of a {@code SignedObject}
     * @param publicKey the public key used for verification
     * @param signedObject the signed object
     * @return true if verified, false otherwise
     */
    public static boolean verifySignature(PublicKey publicKey, SignedObject signedObject) {
        try {
            String algorithm = signedObject.getAlgorithm();
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            return signedObject.verify(publicKey, signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes a {@code PublicKey} to a file
     *
     * @param publicKey the public key
     * @param file the file
     */
    public static void savePublicKeyToFile(PublicKey publicKey, File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(publicKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Gets a user's {@code PublicKey}
     * @param file the file holding the public key
     * @return the public key or null in case of error
     */
    public static PublicKey getUserPubKey(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (PublicKey) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

}
