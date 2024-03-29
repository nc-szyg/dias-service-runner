package org.openjfx.dto;

public enum ElementType {

    /*
    * Those names are also config file names, so please be careful when you editing it
    * */
    SETTINGS,
    INTERNAL_SETTINGS,
    SERVICE_COMMANDS,
    UPDATE_DAP_FOR_TEST_COMMANDS,
    LINKS,
    OPEN_REMOTE_APPS,
    SKAT_VPN;

    private static final String MY_OWN_PREFIX = "my_own_";
    private static final String CONFIG_FILE_EXTENSION = ".csv";

    public String getDefaultFileName() {
        return name().toLowerCase() + CONFIG_FILE_EXTENSION;
    }

    public String getPersonalizedConfigName() {
        return MY_OWN_PREFIX + getDefaultFileName();
    }

}
