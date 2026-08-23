package forfun.miningqol.client.party;

/**
 * Corpse kinds counted off the tab list, so someone can sign up for "any shaft with
 * three Lapis corpses" rather than for a specific gemstone.
 *
 * <p>The tab list names them in caps ({@code LAPIS:unlooted}), one line per corpse, so
 * matching is done against upper-cased tokens rather than the display name.
 */
public enum CorpseType {
    LAPIS("Lapis", "LAPIS"),
    UMBER("Umber", "UMBER"),
    TUNGSTEN("Tungsten", "TUNGSTEN", "TUNG"),
    // Vanguard corpses use the same FAIR id the Vanguard shaft type does.
    VANGUARD("Vanguard", "VANGUARD", "FAIR");

    /** The highest corpse count the picker offers; the top pick means "or more". */
    public static final int MAX_COUNT = 4;

    private final String displayName;
    private final String[] tokens;

    CorpseType(String displayName, String... tokens) {
        this.displayName = displayName;
        this.tokens = tokens;
    }

    public String displayName() {
        return displayName;
    }

    /** Whether an already upper-cased tab list line names this corpse. */
    public boolean matches(String upperCasedLine) {
        for (String token : tokens) {
            if (upperCasedLine.contains(token)) return true;
        }
        return false;
    }

    public static CorpseType byName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
