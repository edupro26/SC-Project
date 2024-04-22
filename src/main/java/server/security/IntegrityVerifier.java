package server.security;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
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

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String CLIENT_COPY = "classes/device_info.csv";
    private static final String DOMAINS = "server-files/domains.txt";

    private final String filePath;
    private final SecretKey secret;
    private final Map<String, String> hmacs;

    public IntegrityVerifier(String filePath, String secret) {
        this.filePath = filePath;
        this.secret = SecurityUtils.generateKey(secret);
        this.hmacs = new HashMap<>();
    }

    public void init() {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                loadHmacs();
            } else {
                file.createNewFile();
                hmacs.put(CLIENT_COPY, calculateHMAC(CLIENT_COPY));
                hmacs.put(DOMAINS, null);
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write("CLIENT: " + hmacs.get(CLIENT_COPY) + "\n"
                        + "DOMAINS: " + hmacs.get(DOMAINS) + "\n");
                bw.close();
            }
            System.out.println("Integrity verifier initialized successfully!");
        } catch (IOException e) {
            System.err.println("Integrity verifier failed to initialize!");
        }
    }

    public boolean verifyAll() {
        boolean verified = true;
        for (Map.Entry<String, String> hmac : hmacs.entrySet()) {
            verified &= verify(hmac.getKey());
        }
        return verified;
    }

    public boolean verify(String path) {
        String savedHmac = hmacs.get(path);
        String newHmac = calculateHMAC(path);
        if (savedHmac == null) {
            return newHmac == null;
        } else {
            if(newHmac == null) return false;
            return newHmac.equals(savedHmac);
        }
    }

    public void update() {
        StringBuilder sb = new StringBuilder();
        String hmac = calculateHMAC(DOMAINS);
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                if(line.contains("DOMAINS:")) {
                    sb.append("DOMAINS: ").append(hmac).append("\n");
                } else {
                    sb.append(line).append("\n");
                }
            }
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, false));
            bw.write(sb.toString());
            hmacs.put(DOMAINS, hmac);
            bw.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void loadHmacs() throws IOException {
        File file = new File(filePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            String[] info = line.split(":");
            String hmac = info[1].trim();
            String value = hmac.equals("null") ? null : hmac;
            if (info[0].equals("CLIENT")) {
                hmacs.put(CLIENT_COPY, value);
            }
            if (info[0].equals("DOMAINS")) {
                hmacs.put(DOMAINS, value);
            }
        }
        br.close();
    }

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
