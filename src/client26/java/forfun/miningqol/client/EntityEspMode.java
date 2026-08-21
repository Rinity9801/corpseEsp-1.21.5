package forfun.miningqol.client;

public enum EntityEspMode {
    BOX("Box"),
    CUSTOM_GLOW("Custom Glow"),
    VANILLA_GLOW("Vanilla Glow");

    private final String displayName;

    EntityEspMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
