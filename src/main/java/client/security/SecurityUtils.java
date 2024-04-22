package client.security;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class SecurityUtils {

    private static final String ENC_ALGORITHM = "PBEWithHmacSHA256AndAES_128";

    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78,
                                        (byte) 0x99, (byte) 0x52, (byte) 0x3e,
                                        (byte) 0xea, (byte) 0xf2 };

    private static final int ITERATION_COUNT = 20;

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

            return cipher.unwrap(wrappedKey, ENC_ALGORITHM, Cipher.SECRET_KEY);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void saveParams(byte[] params, File paramsFile) {
        try (FileOutputStream fos = new FileOutputStream(paramsFile)) {
            fos.write(params);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static byte[] readParams(File paramsFile) {
        try (FileInputStream fis = new FileInputStream(paramsFile)) {
            byte[] params = new byte[(int) paramsFile.length()];
            fis.read(params);
            return params;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static Certificate getOwnCertificate(String alias) {
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
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static PrivateKey findPrivateKeyOnKeyStore(String alias) {
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
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
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
            // Save params
            saveParams(cipher.getParameters().getEncoded(),
                    new File(encryptedFile.getAbsolutePath() + ".params"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static void decryptFile(File encryptedFile, File decryptedFile, SecretKey key) {
        try {
            AlgorithmParameters p = AlgorithmParameters.getInstance(ENC_ALGORITHM);
            p.init(readParams(new File(encryptedFile.getAbsolutePath() + ".params")));
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, p);

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

    public static String cypherTemperature(String temperature, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(temperature.getBytes());
            return Arrays.toString(encryptedBytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
