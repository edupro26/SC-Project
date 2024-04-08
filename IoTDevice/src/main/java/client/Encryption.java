package client;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class Encryption {
    private static final byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };

    private static final int ITERATION_COUNT = 20;

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

    public static void saveKeyIntoFile(SecretKey key, File keyFile) {
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(key.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public static void saveParams(byte[] params, File paramsFile) {
        try (FileOutputStream fos = new FileOutputStream(paramsFile)) {
            fos.write(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] readParams(File paramsFile) {
        try (FileInputStream fis = new FileInputStream(paramsFile)) {
            byte[] params = new byte[(int) paramsFile.length()];
            fis.read(params);
            return params;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
