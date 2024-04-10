package server.security;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

public final class SecurityUtils {
    private static final String API_URL = "https://lmpinto.eu.pythonanywhere.com/2FA";
    private static HttpClient client = HttpClient.newHttpClient();

    // TODO: Make salt secure
    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };

    private static final int ITERATION_COUNT = 20;

    private static final File PARAMS_FILE = new File("params.txt");

    private static final String SERVER_KEYPAIR_ALIAS = "ServerKeyPair";

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

    public static PublicKey getPublicKey(String alias) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(System.getProperty("javax.net.ssl.keyStore")), System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
            return ks.getCertificate(alias).getPublicKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PrivateKey getPrivateKey(String alias) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(System.getProperty("javax.net.ssl.keyStore")), System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
            return (PrivateKey) ks.getKey(alias, System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
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
            PrivateKey privateKey = getPrivateKey(SERVER_KEYPAIR_ALIAS);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            byte buffer[] = data.getBytes();
            signature.update(buffer);
            oos.writeObject(data);
            oos.writeObject(signature.sign());
            fos.close();
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
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
            PublicKey publicKey = getPublicKey(SERVER_KEYPAIR_ALIAS);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes());
            if (sig.verify(signature)) {
                return data;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifySignature(PublicKey publicKey, SignedObject signedObject) {
        try {
            String algorithm = signedObject.getAlgorithm();
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            return signedObject.verify(publicKey, signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void savePublicKeyToFile(PublicKey publicKey, File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PublicKey readPublicKeyFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (PublicKey) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
