package server.persistence.managers;

import server.components.*;

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
     * {@code Object} lock to control concurrency
     */
    private final Object devicesLock;

    /**
     * Data structures
     */
    private final HashMap<Device, List<Domain>> devices;

    /**
     * Constructs a new {@code DeviceManager}
     */
    private DeviceManager() {
        devices = new HashMap<>();
        devicesLock = new Object();
    }

    /**
     * Returns the instance of {@code DeviceManager} or creates
     * it if the instance is still null
     *
     * @return the instance of {@code DeviceManager}
     */
    public static DeviceManager getInstance() {
        if (instance == null) {
            instance = new DeviceManager();
        }
        return instance;
    }

    /**
     * Saves the {@code Device} as the key, and a list of domains as the value,
     * to the map {@link #devices}.
     *
     * @param device the {@code Device} to be saved
     * @param domains a list of {@code Domains} where the {@code Device} is registered
     * @requires {@code device != null && domains != null}
     */
    public void saveDevice(Device device, List<Domain> domains) {
        synchronized (devicesLock) {
            devices.put(device, domains);
        }
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
