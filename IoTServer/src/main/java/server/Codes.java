package server;

/**
 * Enum containing all the response codes
 * sent from the IoTServer to the IoTDevice
 *
 * @author Eduardo Proença - 57551
 *         Manuel Barral - 52026
 *         Tiago Oliveira - 54979
 */
public enum Codes {

    OK("OK"),
    NOK("NOK"),
    NODM("NODM"),
    NOID("NOID"),
    NOUSER("NOUSER"),
    NOPERM("NOPERM"),
    NODATA("NODATA"),
    OKNEWUSER("OK-NEW-USER"),
    OKUSER("OK-USER"),
    OKDEVID("OK-DEVID"),
    OKTESTED("OK-TESTED"),
    WRONGPWD("WRONG-PWD"),
    NOKDEVID("NOK-DEVID"),
    NOKTESTED("NOK-TESTED");

    private final String name;

    Codes(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
