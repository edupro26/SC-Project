package client;

import client.security.SecurityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionTest {

    /**
     * Password to generate the test symmetric key
     */
    private static final String KEY_PASSWORD = "password";
    /**
     * File to store the symmetric key
     */
    private static final String KEY_FILE = "key.txt";
    /**
     * File to store the encrypted symmetric key with RSA
     */
    private static final String KEY_ENCRYPTED_FILE = "key_encrypted.txt";
    /**
     * File to store the decrypted symmetric key with RSA
     */
    private static final String KEY_DECRYPTED_FILE = "key_decrypted.txt";
    /**
     * Truststore file with one keypair for testing encryption and decryption of symmetric keys
     */
    private static final String TRUSTSTORE_FILE = "keystore.test";
    /**
     * Keystore file with one keypair for testing encryption and decryption of symmetric keys
     */
    private static final String KEYSTORE_FILE = "keystore.test";
    /**
     * Password for the truststore and keystore
     */
    private static final String KEYSTORE_PASSWORD = "password";
    /**
     * Alias of the keypair in the keystore/truststore for testing encryption and decryption of symmetric keys
     */
    private static final String PUBLIC_KEY_ALIAS = "testKeyPair";
    /**
     * File with one example of aq public key to test the storage of public keys in the truststore
     */
    private static final String PK_FILE = "test1.cer";
    /**
     * Alias of the keypair to be stored in the truststore for testing the storage of public keys
     */
    private static final String NEW_PUBLIC_KEY_ALIAS = "test1";


    @BeforeAll
    static void setUp() {
        File keyFile = new File("key.txt");
        if (keyFile.exists()) {
            keyFile.delete();
        }
        File keyEncryptedFile = new File(KEY_ENCRYPTED_FILE);
        if (keyEncryptedFile.exists()) {
            keyEncryptedFile.delete();
        }
        File keyDecryptedFile = new File(KEY_DECRYPTED_FILE);
        if (keyDecryptedFile.exists()) {
            keyDecryptedFile.delete();
        }

        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE);
        System.setProperty("javax.net.ssl.trustStorePassword", KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);

    }

    @org.junit.jupiter.api.Test
    void generateKey() {
        SecretKey key = SecurityUtils.generateKey(KEY_PASSWORD);
        assertNotNull(key);
    }

    @org.junit.jupiter.api.Test
    void saveKeyIntoFile() {
        SecretKey key = SecurityUtils.generateKey(KEY_PASSWORD);
        SecurityUtils.saveKeyIntoFile(key, new File(KEY_FILE));
        assertTrue(new File(KEY_FILE).exists());
    }



    @org.junit.jupiter.api.Test
    void findPublicKeyOnTrustStore() {
        PublicKey pubKey = SecurityUtils.findPublicKeyOnTrustStore(PUBLIC_KEY_ALIAS);
        assertNotNull(pubKey);
    }

    @Test
    void findPrivateKeyOnKeyStore() {
        PrivateKey privateKey = SecurityUtils.getPrivateKey(PUBLIC_KEY_ALIAS);
        assertNotNull(privateKey);
    }

    @org.junit.jupiter.api.Test
    void encryptKeyWithRSA() {
        SecretKey key = SecurityUtils.generateKey(KEY_PASSWORD);
        PublicKey pubKey = SecurityUtils.findPublicKeyOnTrustStore(PUBLIC_KEY_ALIAS);
        SecurityUtils.encryptKeyWithRSA(key, pubKey, KEY_ENCRYPTED_FILE);
        assertTrue(new File(KEY_ENCRYPTED_FILE).exists());
    }

    @Test
    void decryptKeyWithRSA() {
        SecretKey key = SecurityUtils.generateKey(KEY_PASSWORD);
        PublicKey pubKey = SecurityUtils.findPublicKeyOnTrustStore(PUBLIC_KEY_ALIAS);
        System.out.println(pubKey);
        SecurityUtils.encryptKeyWithRSA(key, pubKey, KEY_ENCRYPTED_FILE);
        PrivateKey privateKey = SecurityUtils.getPrivateKey(PUBLIC_KEY_ALIAS);
        System.out.println(privateKey);
        Key decryptedKey = SecurityUtils.decryptKeyWithRSA(new File(KEY_ENCRYPTED_FILE), privateKey);
        assertEquals(key, decryptedKey);
    }

    @Test
    void storePubKeyOnTrustStore() {
        SecurityUtils.storePubKeyOnTrustStore(new File(PK_FILE), NEW_PUBLIC_KEY_ALIAS);
        assertNotNull(SecurityUtils.findPublicKeyOnTrustStore(NEW_PUBLIC_KEY_ALIAS));
    }



    @AfterAll
    static void tearDown() {
        File keyFile = new File(KEY_FILE);
        if (keyFile.exists()) {
            keyFile.delete();
        }
        File keyEncryptedFile = new File(KEY_ENCRYPTED_FILE);
        if (keyEncryptedFile.exists()) {
            keyEncryptedFile.delete();
        }
        File keyDecryptedFile = new File(KEY_DECRYPTED_FILE);
        if (keyDecryptedFile.exists()) {
            keyDecryptedFile.delete();
        }

    }

    @Test
    void encryptFile() {
        File fileToEncrypt = new File("file.txt");
        File encryptedFile = new File("file_encrypted.txt");
        SecretKey key = SecurityUtils.generateKey(KEY_PASSWORD);
        SecurityUtils.encryptKeyWithRSA(key, SecurityUtils.findPublicKeyOnTrustStore(PUBLIC_KEY_ALIAS), KEY_ENCRYPTED_FILE);
        SecretKey key2 = (SecretKey) SecurityUtils.decryptKeyWithRSA(new File(KEY_ENCRYPTED_FILE), SecurityUtils.getPrivateKey(PUBLIC_KEY_ALIAS));
        SecurityUtils.encryptFile(fileToEncrypt, encryptedFile, key2);
        assertTrue(encryptedFile.exists());

    }

    @Test
    void decryptFile() {
        File fileToEncrypt = new File("file.txt");
        File encryptedFile = new File("file_encrypted.txt");
        SecretKey key = SecurityUtils.generateKey(KEY_PASSWORD);
        SecurityUtils.encryptKeyWithRSA(key, SecurityUtils.findPublicKeyOnTrustStore(PUBLIC_KEY_ALIAS), KEY_ENCRYPTED_FILE);
        SecretKey key2 = (SecretKey) SecurityUtils.decryptKeyWithRSA(new File(KEY_ENCRYPTED_FILE), SecurityUtils.getPrivateKey(PUBLIC_KEY_ALIAS));
        SecurityUtils.encryptFile(fileToEncrypt, encryptedFile, key2);
        File decryptedFile = new File("file_decrypted.txt");
        SecurityUtils.decryptFile(encryptedFile, decryptedFile, key2);
        assertTrue(decryptedFile.exists());
    }
}