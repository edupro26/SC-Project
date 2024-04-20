package server.security;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
     * File paths
     */
    private static final String ALGORITHM = "SHA-256";
    private static final String DOMAINS = "server-files/domains.txt";

    /**
     * Path and pointer to the checksums file
     */
    private final String filePath;
    private File fileInstance;

    /**
     * Checksums of each data file
     */
    //TODO private String clientChecksum;
    private String domainsChecksum;

    // TODO updated this class logic, given the recent
    //  changes of the devices file.

    /**
     * Constructs a new {@code IntegrityVerifier}
     *
     * @param filePath the path of the file to save checksums
     */
    public IntegrityVerifier(String filePath) {
        domainsChecksum = "";
        this.filePath = filePath;
        if (new File(filePath).exists()) {
            fileInstance = new File(filePath);
            if (verifyAll()){
                System.out.println("File integrity verified!");
            } else {
                System.err.println("Corrupted files found!");
            }
        }
    }

    /**
     * Verifies the integrity of all the files
     *
     * @return true if there are no corrupted files,
     *      false otherwise
     */
    private boolean verifyAll() {
        boolean result = loadChecksums();
        if (!domainsChecksum.isEmpty()) result &= verify(DOMAINS);
        return result;
    }

    /**
     * Loads the checksum values from the file
     *
     * @return true if succeeded, false otherwise
     */
    private boolean loadChecksums() {
        String data = SecurityUtils.verifySignature(fileInstance);
        if (data != null) {
            String[] checksums = data.split("\n");
            domainsChecksum = checksums[0];
            return true;
        }
        return false;
    }

    /**
     * Updated the content of the file with the
     * {@link #domainsChecksum} and the {@link #domainsChecksum}.
     */
    private void updateFile() {
        StringBuilder checksums = new StringBuilder();
        checksums.append(domainsChecksum).append('\n');
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

    /**
     * Verifies the integrity of a file
     *
     * @param filePath the file path
     * @return true if not corrupted, false otherwise
     */
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
            }
        }
        return false;
    }

    /**
     * Returns the checksum value of a file
     *
     * @param file the file
     * @return the checksum, or null in case of an error
     */
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

    /**
     * Updates the checksum, depending on the filepath that was given
     *
     * @param filePath the filepath
     * @param checksum the checksum
     */
    public void updateChecksum(String filePath, String checksum) {
        switch (filePath){
            case DOMAINS -> this.domainsChecksum = checksum;
        }
        updateFile();
    }

}
