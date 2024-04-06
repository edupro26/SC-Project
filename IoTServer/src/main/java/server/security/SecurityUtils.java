package server.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SecurityUtils {
    private static final String API_URL = "https://lmpinto.eu.pythonanywhere.com/2FA";
    private static HttpClient client = HttpClient.newHttpClient();

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
}
