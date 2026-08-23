package forfun.miningqol.client.party;

/**
 * The mineshaft types the auto-party can warp people to, in the order they are laid
 * out in the picker grid.
 *
 * <p>{@link #ANY} carries no scoreboard id — it matches every shaft, which is what you
 * want someone signed up for when they are only after a pickaxe ability reset.
 */
public enum ShaftType {
    ANY("Any", null),
    TOPAZ_1("Topaz 1", "TOPA_1"),
    TOPAZ_2("Topaz 2", "TOPA_2"),
    SAPPHIRE_1("Sapphire 1", "SAPP_1"),
    SAPPHIRE_2("Sapphire 2", "SAPP_2"),
    AMETHYST_1("Amethyst 1", "AMET_1"),
    AMETHYST_2("Amethyst 2", "AMET_2"),
    AMBER_1("Amber 1", "AMBE_1"),
    AMBER_2("Amber 2", "AMBE_2"),
    JADE_1("Jade 1", "JADE_1"),
    JADE_2("Jade 2", "JADE_2"),
    TITANIUM("Titanium", "TITA_1"),
    UMBER("Umber", "UMBE_1"),
    TUNGSTEN("Tungsten", "TUNG_1"),
    VANGUARD("Vanguard", "FAIR_1"),
    RUBY_1("Ruby 1", "RUBY_1"),
    RUBY_2("Ruby 2", "RUBY_2"),
    RUBY_CRYSTAL("Ruby Crystal", "RUBY_C"),
    ONYX_1("Onyx 1", "ONYX_1"),
    ONYX_2("Onyx 2", "ONYX_2"),
    ONYX_CRYSTAL("Onyx Crystal", "ONYX_C"),
    AQUAMARINE_1("Aquamarine 1", "AQUA_1"),
    AQUAMARINE_2("Aquamarine 2", "AQUA_2"),
    AQUAMARINE_CRYSTAL("Aqua Crystal", "AQUA_C"),
    CITRINE_1("Citrine 1", "CITR_1"),
    CITRINE_2("Citrine 2", "CITR_2"),
    CITRINE_CRYSTAL("Citrine Crystal", "CITR_C"),
    PERIDOT_1("Peridot 1", "PERI_1"),
    PERIDOT_2("Peridot 2", "PERI_2"),
    PERIDOT_CRYSTAL("Peridot Crystal", "PERI_C"),
    JASPER("Jasper", "JASP_1"),
    JASPER_CRYSTAL("Jasper Crystal", "JASP_C"),
    OPAL("Opal", "OPAL_1"),
    OPAL_CRYSTAL("Opal Crystal", "OPAL_C"),
    LITTLEFOOT("Littlefoot", "LITT_L");

    private final String displayName;
    private final String scoreboardId;

    ShaftType(String displayName, String scoreboardId) {
        this.displayName = displayName;
        this.scoreboardId = scoreboardId;
    }

    public String displayName() {
        return displayName;
    }

    /** The token to look for on the scoreboard, or null for {@link #ANY}. */
    public String scoreboardId() {
        return scoreboardId;
    }

    /** The type whose id is present in the (formatting-stripped) scoreboard text, if any. */
    public static ShaftType fromScoreboard(String scoreboardText) {
        for (ShaftType type : values()) {
            if (type.scoreboardId != null && scoreboardText.contains(type.scoreboardId)) {
                return type;
            }
        }
        return null;
    }

    /** Round-trips config values, falling back to null for ids written by a newer build. */
    public static ShaftType byName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
