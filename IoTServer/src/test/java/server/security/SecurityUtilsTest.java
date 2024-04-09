package server.security;

import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
class SecurityUtilsTest {
    private static final String CIPHER_PASSWORD = "INSERT_CIPHER_PASSWORD_HERE";
    private static final String dummyData = "INSERT_DUMMY_DATA_HERE";
    private static final String dummyFile = "hello.txt";

    private static final String dummyFileToSign = "hello.txt";
    private static final String dummySignedData = "DUMMY SIGNED DATA";

    @BeforeAll
    static void setUp() {
        File file = new File(dummyFile);
        if (file.exists()) {
            file.delete();
        }
    }

    @Order(1)
    @org.junit.jupiter.api.Test
    void generateKey() {
        assertNotNull(SecurityUtils.generateKey(CIPHER_PASSWORD));
    }

    @Order(2)
    @org.junit.jupiter.api.Test
    @Disabled
    void send2FACode() {
        String code = String.valueOf((int) (Math.random() * 90000) + 10000);
        String email = "INSERT_EMAIL_HERE";
        String apiKey = "INSERT_API_KEY_HERE";
        boolean result = SecurityUtils.send2FACode(code, email, apiKey);
        assertTrue(result);
    }

    @Order(3)
    @Test
    void encryptDataIntoFile() {
        SecurityUtils.encryptDataIntoFile(dummyData, new File(dummyFile), SecurityUtils.generateKey(CIPHER_PASSWORD));
    }

    @Order(4)
    @Test
    void decryptDataFromFile() {
        SecurityUtils.encryptDataIntoFile(dummyData, new File(dummyFile), SecurityUtils.generateKey(CIPHER_PASSWORD));
        assertEquals(dummyData, SecurityUtils.decryptDataFromFile(new File(dummyFile), SecurityUtils.generateKey(CIPHER_PASSWORD)));
    }

    @Order(5)
    @Test
    void signFile() {
        System.setProperty("javax.net.ssl.keyStore", "keystore.test");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        SecurityUtils.signFile(new File(dummyFileToSign), dummySignedData);
        assertTrue(new File(dummyFileToSign).exists());
    }

    @Order(6)
    @Test
    void verifySignature() {
        System.setProperty("javax.net.ssl.keyStore", "keystore.test");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        SecurityUtils.signFile(new File(dummyFileToSign), dummySignedData);
        assertEquals(SecurityUtils.verifySignature(new File(dummyFileToSign)), dummySignedData);
    }

    @AfterAll
    static void tearDown() {
        // Delete the dummy file
        File file = new File(dummyFile);
        if (file.exists()) {
            file.delete();
        }

        // Delete the signed file
        File signedFile = new File(dummyFileToSign);
        if (signedFile.exists()) {
            signedFile.delete();
        }



    }
}