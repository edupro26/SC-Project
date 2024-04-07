package server.security;

import static org.junit.jupiter.api.Assertions.*;
class SecurityUtilsTest {

    @org.junit.jupiter.api.Test
    void generateKey() {
    }

    @org.junit.jupiter.api.Test
    void send2FACode() {
        String code = String.valueOf((int) (Math.random() * 90000) + 10000);
        String email = "INSERT_EMAIL_HERE";
        String apiKey = "INSERT_API_KEY_HERE";
        boolean result = SecurityUtils.send2FACode(code, email, apiKey);
        assertTrue(result);
    }
}