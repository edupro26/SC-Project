package common;

/**
 * Enum containing all the response codes of the {@code IoTServer}.
 *
 * @author Eduardo Proença (57551)
 * @author Manuel Barral (52026)
 * @author Tiago Oliveira (54979)
 */
public enum Codes {

    OK("OK"),
    NOK("NOK"),
    NRD("NRD"),
    NODM("NODM"),
    NOID("NOID"),
    NOUSER("NOUSER"),
    NOPERM("NOPERM"),
    NODATA("NODATA"),
    FOUNDUSER("FOUND-USER"),
    NEWUSER("NEW-USER"),
    OKNEWUSER("OK-NEW-USER"),
    OKUSER("OK-USER"),
    OK2FA("OK-2FA"),
    OKDEVID("OK-DEVID"),
    OKTESTED("OK-TESTED"),
    NOKDEVID("NOK-DEVID"),
    NOKTESTED("NOK-TESTED"),
    CRR("CRR");

    private final String name;

    Codes(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
