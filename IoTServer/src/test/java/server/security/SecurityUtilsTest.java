package server.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
class SecurityUtilsTest {
    private static final String CIPHER_PASSWORD = "INSERT_CIPHER_PASSWORD_HERE";
    private static final String dummyData = "INSERT_DUMMY_DATA_HERE";
    private static final String dummyFile = "hello.txt";

    @BeforeAll
    static void setUp() {
        File file = new File(dummyFile);
        if (file.exists()) {
            file.delete();
        }
    }

    @org.junit.jupiter.api.Test
    void generateKey() {
        assertNotNull(SecurityUtils.generateKey(CIPHER_PASSWORD));
    }

    @org.junit.jupiter.api.Test
    @Disabled
    void send2FACode() {
        String code = String.valueOf((int) (Math.random() * 90000) + 10000);
        String email = "INSERT_EMAIL_HERE";
        String apiKey = "INSERT_API_KEY_HERE";
        boolean result = SecurityUtils.send2FACode(code, email, apiKey);
        assertTrue(result);
    }

    @Test
    void encryptDataIntoFile() {
        SecurityUtils.encryptDataIntoFile(dummyData, new File(dummyFile), SecurityUtils.generateKey(CIPHER_PASSWORD));
    }

    @Test
    void decryptDataFromFile() {
        SecurityUtils.encryptDataIntoFile(dummyData, new File(dummyFile), SecurityUtils.generateKey(CIPHER_PASSWORD));
        assertEquals(dummyData, SecurityUtils.decryptDataFromFile(new File(dummyFile), SecurityUtils.generateKey(CIPHER_PASSWORD)));
    }
}