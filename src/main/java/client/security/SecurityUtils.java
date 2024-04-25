package client.security;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utility class with security
 * methods used by the client
 */
public class SecurityUtils {

    /**
     * Encryption algorithm
     */
    private static final String ENC_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Parameters for SecretKey generation
     */
    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78,
                                        (byte) 0x99, (byte) 0x52, (byte) 0x3e,
                                        (byte) 0xea, (byte) 0xf2 };

    private static final int ITERATION_COUNT = 20;

    private static final int KEY_LENGTH = 128;

    /**
     * Generates a symmetric key given a cipher-password.
     *
     * @param cipherPassword the password to be used to generate the key
     * @return the generated key
     */
    public static SecretKey generateKey(String cipherPassword) {
        KeySpec keySpec = new PBEKeySpec(
                cipherPassword.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance(ENC_ALGORITHM);
            byte[] key = kf.generateSecret(keySpec).getEncoded();
            return new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Encrypts a {@code SecretKey} with the given {@code PublicKey}
     * and saves it to a file
     *
     * @param key the secret key to be encrypted
     * @param pubKey the public key used for encryption
     * @param filenameToSave the file path
     */
    public static void encryptKeyWithRSA(SecretKey key, PublicKey pubKey, String filenameToSave) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.WRAP_MODE, pubKey);

            byte[] wrappedKey = cipher.wrap(key);

            try (FileOutputStream fos = new FileOutputStream(filenameToSave)) {
                fos.write(wrappedKey);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Decrypts a key stored in a file using a {@code PrivateKey}
     *
     * @param keyFile the file holding the encrypted key
     * @param privateKey the private key used for decryption
     * @return a key if it was successfulley decrypted, null otherwise
     */
    public static Key decryptKeyWithRSA(File keyFile, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.UNWRAP_MODE, privateKey);

            byte[] wrappedKey = new byte[(int) keyFile.length()];
            try (FileInputStream fis = new FileInputStream(keyFile)) {
                fis.read(wrappedKey);
            }

            return cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the {@code Certificate} of a given alias
     *
     * @param alias the alias used to search
     * @return the certificate if found, null otherwise
     */
    public static Certificate getCertificate(String alias) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    System.getProperty("javax.net.ssl.keyStore")),
                    System.getProperty("javax.net.ssl.keyStorePassword")
                            .toCharArray());

            return ks.getCertificate(alias);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the {@code PublicKey} of a given alias in a truststore
     *
     * @param alias the alias used to search
     * @return the public key if found, null otherwise
     */
    public static PublicKey findPublicKeyOnTrustStore(String alias) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    System.getProperty("javax.net.ssl.trustStore")),
                    System.getProperty("javax.net.ssl.trustStorePassword")
                            .toCharArray());

            return ks.getCertificate(alias).getPublicKey();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the {@code PrivateKey} of a given alias
     *
     * @param alias the alias used to search
     * @return the private key if found, null otherwise
     */
    public static PrivateKey getPrivateKey(String alias) {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(
                    System.getProperty("javax.net.ssl.keyStore")),
                    System.getProperty("javax.net.ssl.keyStorePassword")
                            .toCharArray());

            return (PrivateKey) ks.getKey(alias,
                    System.getProperty("javax.net.ssl.keyStorePassword")
                            .toCharArray());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Encrypts the content of a file to another file
     *
     * @param fileToEncrypt the file with the content to encrypt
     * @param encryptedFile the file to save the encrypted content
     * @param key the secret key used for encryption
     */
    public static void encryptFile(File fileToEncrypt, File encryptedFile, SecretKey key) {
        try {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            try (FileInputStream fis = new FileInputStream(fileToEncrypt);
                 FileOutputStream fos = new FileOutputStream(encryptedFile);
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, read);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Decrypts the content of a file to another file
     *
     * @param encryptedFile the file with the encrypted content
     * @param decryptedFile the file to save the decrypted content
     * @param key the secret key used for decryption
     * @return the length of the decrypted file or -1 in case of error
     */
    public static int decryptFile(File encryptedFile, File decryptedFile, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            try (FileInputStream fis = new FileInputStream(encryptedFile);
                 FileOutputStream fos = new FileOutputStream(decryptedFile);
                 CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, read);
                }
            }
            return (int) decryptedFile.length();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Encrypts a temperature value
     *
     * @param temperature the temperature value to be encrypted
     * @param key the secret key used for encryption
     * @return the encrypted temperature or null in case of error
     */
    public static String encryptTemperature(String temperature, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(temperature.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decrypts the file with the saved temperatures
     *
     * @param temperaturesFile the encrypted file
     * @param key the secret key used for decryption
     * @return the length of the decrypted file or -1 in case of error
     */
    public static int decryptTemperatures(File temperaturesFile, SecretKey key) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(temperaturesFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                String[] content = line.split(",");
                String temperature = decryptTemperature(content[1], key);
                sb.append(content[0]).append("->")
                        .append(temperature).append("\n");
            }
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(temperaturesFile));
            bw.write(sb.toString());
            bw.close();
            return (int) temperaturesFile.length();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Decrypts a temperature value
     *
     * @param temperature the temperature value to be decrypted
     * @param key the secret key used for decryption
     * @return the decrypted temperature or null in case of error
     */
    private static String decryptTemperature(String temperature, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainText = cipher.doFinal(Base64.getDecoder()
                    .decode(temperature));
            return new String(plainText);
        } catch (Exception e) {
            return null;
        }
    }
}
