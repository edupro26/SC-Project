package server.security;

import server.ServerLogger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to verify integrity of files
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see SecurityUtils
 */
public class IntegrityVerifier {

    /**
     * Algorithm for HMAC calculation
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * File paths
     */
    private static final String CLIENT_COPY = "classes/IoTServer/device_info.csv";
    private static final String DOMAINS = "server/domains.txt";

    /**
     * Pointer to the hmacs.txt file
     */
    private final String filePath;

    /**
     * Data structures
     */
    private final SecretKey secret;
    private final Map<String, String> hmacs;

    /**
     * Constructs a new {@code IntegrityVerifier}
     *
     * @param filePath the file to save HMACs
     * @param secret the secret key
     */
    public IntegrityVerifier(String filePath, String secret) {
        this.filePath = filePath;
        this.secret = SecurityUtils.generateKey(secret);
        this.hmacs = new HashMap<>();
    }

    /**
     * Inicializes this {@code IntegrityVerifier}
     */
    public void init() {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (loadHmacs()) {
                    ServerLogger.logInfo("Integrity verifier initialized successfully!");
                } else {
                    ServerLogger.logError("Integrity verifier failed to initialize!");
                }
            } else {
                file.createNewFile();
                hmacs.put(CLIENT_COPY, calculateHMAC(CLIENT_COPY));
                hmacs.put(DOMAINS, null);
                String init = "CLIENT: " + hmacs.get(CLIENT_COPY) + "\n"
                        + "DOMAINS: " + hmacs.get(DOMAINS) + "\n";
                SecurityUtils.signFile(file, init);
                ServerLogger.logInfo("Integrity verifier initialized successfully!");
            }
        } catch (IOException e) {
            ServerLogger.logError("Integrity verifier failed to initialize!");
        }
    }

    /**
     * Verifies the integrity of all the files
     *
     * @return true if there are no corrupted files,
     *          false otherwise
     */
    public boolean verifyAll() {
        if (hmacs.entrySet().isEmpty()) return false;
        for (Map.Entry<String, String> hmac : hmacs.entrySet()) {
            if (!verify(hmac.getKey())) return false;
        }
        return true;
    }

    /**
     * Verifies the integrity of a file
     *
     * @param path the file path
     * @return true if not corrupted, false otherwise
     */
    public boolean verify(String path) {
        String data = SecurityUtils.verifySignature(new File(filePath));
        if (data == null) return false;
        String savedHmac = hmacs.get(path);
        String newHmac = calculateHMAC(path);
        if (savedHmac == null) {
            return newHmac == null;
        } else {
            if(newHmac == null) return false;
            return newHmac.equals(savedHmac);
        }
    }

    /**
     * Updates the HMAC value of the domains.txt file, both
     * in the map {@link #hmacs} and in the hmacs.txt file
     */
    public void update() {
        String data = SecurityUtils.verifySignature(new File(filePath));
        if (data != null) {
            StringBuilder sb = new StringBuilder();
            String hmac = calculateHMAC(DOMAINS);
            String[] lines = data.split("\n");
            for (String line : lines) {
                if (line.contains("DOMAINS:")) {
                    sb.append("DOMAINS: ").append(hmac).append("\n");
                } else {
                    sb.append(line).append("\n");
                }
            }
            SecurityUtils.signFile(new File(filePath), sb.toString());
            hmacs.put(DOMAINS, hmac);
        }
    }

    /**
     * Loads the HMACS values saved in the hmacs.txt file
     * to the map {@link #hmacs}
     *
     * @return true if the HMACs were loaded, false otherwise
     */
    private boolean loadHmacs() {
        String data = SecurityUtils.verifySignature(new File(filePath));
        if (data != null) {
            String[] lines = data.split("\n");
            for (String line : lines) {
                String[] temp = line.split(":");
                String hmac = temp[1].trim();
                hmac = hmac.equals("null") ? null : hmac;
                if (temp[0].equals("CLIENT")) {
                    hmacs.put(CLIENT_COPY, hmac);
                }
                if (temp[0].equals("DOMAINS")) {
                    hmacs.put(DOMAINS, hmac);
                }
            }
        }
        return data != null;
    }

    /**
     * Calculates the HMAC value of the file in
     * the given path
     *
     * @param path the path to the file
     * @return the HMAC value, null if the file is empty
     *          or an exception occured
     */
    private String calculateHMAC(String path) {
        try {
            byte[] data = readFile(path);
            if (data == null) return null;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secret);
            byte[] hmac = mac.doFinal(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmac) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Reads the content of a file to a byte array
     *
     * @param path the path of the file
     * @return a byte array, null if the file is empty
     *          or does not exist
     * @throws IOException If an I/ O error occurs
     */
    private byte[] readFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            if(data.length == 0) return null;
            return data;
        }
        return null;
    }

}
