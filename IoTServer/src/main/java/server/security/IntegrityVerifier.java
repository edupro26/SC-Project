package server.security;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IntegrityVerifier {

    private static final String ALGORITHM = "SHA-256";
    private static final String DOMAINS = "server-files/domains.txt";
    private static final String DEVICES = "server-files/devices.txt";

    private final String filePath;
    private File fileInstance;

    //TODO private String clientChecksum;
    private String domainsChecksum;
    private String devicesChecksum;

    public IntegrityVerifier(String filePath) {
        domainsChecksum = "";
        devicesChecksum = "";
        this.filePath = filePath;
        if (new File(filePath).exists()) {
            fileInstance = new File(filePath);
            if (verifyAll()){
                System.out.println("File integrity verified");
            } else {
                System.err.println("Corrupted files were found!");
            }
        }
    }

    private boolean verifyAll() {
        boolean result = loadChecksums();
        if (!domainsChecksum.isEmpty()) result &= verify(DOMAINS);
        if (!devicesChecksum.isEmpty()) result &= verify(DEVICES);
        return result;
    }

    private boolean loadChecksums() {
        String data = SecurityUtils.verifySignature(fileInstance);
        if (data != null) {
            String[] checksums = data.split("\n");
            domainsChecksum = checksums[0];
            //TODO devicesChecksum = checksums[1];
            return true;
        }
        return false;
    }

    public boolean verify(String filePath) {
        File file = new File(filePath);
        String data = SecurityUtils.verifySignature(fileInstance);
        if (data != null) {
            switch (filePath) {
                case DOMAINS -> {
                    String checksum = calculateChecksum(file);
                    if (checksum.equals(domainsChecksum))
                        return true;
                }
                case DEVICES -> {
                    String checksum = calculateChecksum(file);
                    if (checksum.equals(devicesChecksum))
                        return true;
                }
            }
        }
        return false;
    }

    public String calculateChecksum(File file) {
        StringBuilder checksum = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            fis.close();
            byte[] digest = md.digest();
            for (byte b : digest) {
                checksum.append(Integer
                        .toString((b & 0xff) + 0x100, 16)
                        .substring(1));
            }
            return checksum.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    public void updateFile() {
        StringBuilder checksums = new StringBuilder();
        checksums.append(domainsChecksum).append('\n')
                .append(devicesChecksum).append('\n');
        try {
            if(fileInstance == null) {
                new File(filePath).createNewFile();
                fileInstance = new File(filePath);
            }
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter(fileInstance, false));
            bw.write(checksums.toString());
            bw.close();
            SecurityUtils.signFile(fileInstance, checksums.toString());
        } catch (IOException e) {
            System.out.println("Error while updating checksums: " + e.getMessage());
        }
    }

    public void setDomainsChecksum(String domainsChecksum) {
        this.domainsChecksum = domainsChecksum;
    }

    public void setDevicesChecksum(String devicesChecksum) {
        this.devicesChecksum = devicesChecksum;
    }

}
