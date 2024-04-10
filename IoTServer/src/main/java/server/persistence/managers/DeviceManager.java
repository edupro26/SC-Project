package server.persistence.managers;

import server.communication.Codes;
import server.components.Device;
import server.components.Domain;
import server.components.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class that manages the devices of the {@code IoTServer}
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 *
 * @see Device
 * @see Domain
 */
public class DeviceManager {

    /**
     * The instance of {@code DeviceManager}
     */
    private static DeviceManager instance = null;

    /**
     * Data structures
     */
    private final String devicesFile;
    private final HashMap<Device, List<Domain>> devices;

    /**
     * Constructs a new {@code DeviceManager}
     *
     * @param filePath the path of the file to be managed
     */
    private DeviceManager(String filePath) {
        devicesFile = filePath;
        devices = new HashMap<>();
    }

    /**
     * Returns the instance of {@code DeviceManager} or creates
     * it if the instance is still null
     *
     * @param filePath the path of the file to be managed
     * @return the instance of {@code DeviceManager}
     */
    public static DeviceManager getInstance(String filePath) {
        if (instance == null) {
            instance = new DeviceManager(filePath);
        }
        return instance;
    }

    /**
     * Saves the {@code Device} as the key, and a list of domains as the value,
     * to the map {@link #devices}. It also writes the device to a devices.txt
     * file located in the server-files folder.
     *
     * @param device the {@code Device} to be saved
     * @param domains a list of {@code Domains} where the {@code Device} is registered
     * @requires {@code device != null && domains != null}
     */
    public synchronized void saveDevice(Device device, List<Domain> domains) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(devicesFile, true))) {
            writer.write(device + "," + device.getLastTemp() + "\n");
            devices.put(device, domains);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Saves the last temperature sent from the given {@code Device} and
     * updates the devices.txt file located in the server-files folder.
     * Returns "OK" if the method concluded with success, "NOK" otherwise
     *
     * @param device the {@code Device}
     * @param temperature the last temperature sent
     * @return status code
     * @see Codes
     * @requires {@code device != null && temperature != null}
     */
    public synchronized String updateLastTemp(Device device, Float temperature) {
        try (BufferedReader in = new BufferedReader(new FileReader(devicesFile))) {
            StringBuilder file = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                String[] temp = line.split(",");
                if (temp[0].equals(device.toString())) {
                    file.append(device).append(",")
                            .append(temperature).append("\n");
                } else {
                    file.append(line).append("\n");
                }
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(devicesFile, false));
            out.write(file.toString());
            out.close();
            device.setLastTemp(temperature);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return Codes.NOK.toString();
        }
        return Codes.OK.toString();
    }

    /**
     * Verifies if a {@code User} has permission to read data
     * sent from the {@code Device}.
     *
     * @param user the {@code User} to verify
     * @param device the {@code Device}
     * @return true, if the user has permission, false otherwise
     */
    public boolean hasPerm(User user, Device device) {
        for (Map.Entry<Device, List<Domain>> entry : devices.entrySet()) {
            if (entry.getKey().equals(device)) {
                for (Domain domain : entry.getValue()) {
                    if (domain.getUsers().contains(user) || domain.getOwner().equals(user))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds a {@code Domain} to the list of {@code Domains} of the
     * given {@code Device}.
     *
     * @param device the {@code Device}
     * @param domain the {@code Domain}
     */
    public void addDomainToDevice(Device device, Domain domain) {
        devices.get(device).add(domain);
    }

    /**
     * Returns a list of {@code Domains} containing all the
     * domains where the given {@code Device} is registered
     *
     * @param device the {@code Device}
     * @return a list of {@code Domains}
     */
    public List<Domain> getDeviceDomains(Device device) {
        return devices.get(device);
    }

    /**
     * Returns a {@code Device} from the map {@link #devices}
     * that matches the {@code Device} given, used as a key.
     *
     * @param device the {@code Device} used as key for the search
     * @return a {@code Device}, if the key matched, null otherwise
     */
    public Device getDevice(Device device) {
        for (Map.Entry<Device, List<Domain>> entry : devices.entrySet()) {
            if (entry.getKey().equals(device))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Returns the map {@link #devices}.
     *
     * @return the map {@link #devices}.
     */
    public HashMap<Device, List<Domain>> getDevices() {
        return devices;
    }

}
