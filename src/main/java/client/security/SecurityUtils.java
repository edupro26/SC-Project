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
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class SecurityUtils {

    private static final String ENC_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78,
                                        (byte) 0x99, (byte) 0x52, (byte) 0x3e,
                                        (byte) 0xea, (byte) 0xf2 };

    private static final int ITERATION_COUNT = 20;

    private static final int KEY_LENGTH = 128;

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

    public static void saveKeyIntoFile(SecretKey key, File keyFile) {
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(key.getEncoded());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

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

    public static void storePubKeyOnTrustStore(File pubKeyFile, String alias) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream tsFis = new FileInputStream(System.getProperty("javax.net.ssl.trustStore"));
            trustStore.load(tsFis, System.getProperty("javax.net.ssl.trustStorePassword").toCharArray());
            tsFis.close();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certStream = new FileInputStream(pubKeyFile);
            Certificate cert = cf.generateCertificate(certStream);
            certStream.close();

            trustStore.setCertificateEntry(alias, cert);

            FileOutputStream tsFos = new FileOutputStream(System.getProperty("javax.net.ssl.trustStore"));
            trustStore.store(tsFos, System.getProperty("javax.net.ssl.trustStorePassword").toCharArray());
            tsFos.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

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

    public static void decryptFile(File encryptedFile, File decryptedFile, SecretKey key) {
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

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
