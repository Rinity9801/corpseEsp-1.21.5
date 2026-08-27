package forfun.miningqol.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Mineshaft mobs the ESP recognises by skin.
 *
 * <p>Hypixel's humanoid mobs are fake players wearing a fixed skin, so the texture hash in the
 * profile's {@code textures} property identifies the mob exactly — unlike the nametag armour
 * stand, which floats above the mob and only carries a name. Matching the skin puts the box on
 * the mob itself and lets each kind be switched on its own. The Littlefoot has its own colour;
 * the rest draw in the Mob ESP colour.
 *
 * <p>Hashes are the tail of {@code http://textures.minecraft.net/texture/<hash>}.
 */
public enum SkinMob {
    LITTLEFOOT("Littlefoot", "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b"),
    GLACITE_CAVER("Glacite Caver", "ef3178fb4bd2c629c218ec03fd4a96bfdc846b1f5625743c49eb205b873ae0d5"),
    GLACITE_BOWMAN("Glacite Bowman", "3e1cef33161ec42226aa8220f1b1cc02e8ede6dea7cdd487402f559f3c8fdab6"),
    GLACITE_MAGE("Glacite Mage", "f941d0e9413b50507919e2679a02a034a37cd0661b7c2de646a076d636033f42");

    private final String displayName;
    private final String skinHash;
    private boolean enabled = true;

    SkinMob(String displayName, String skinHash) {
        this.displayName = displayName;
        this.skinHash = skinHash;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public static @Nullable SkinMob byHash(@Nullable String hash) {
        if (hash == null) return null;
        for (SkinMob mob : values()) {
            if (mob.skinHash.equalsIgnoreCase(hash)) return mob;
        }
        return null;
    }

    public static @Nullable SkinMob byName(@Nullable String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The skin texture hash out of a player's profile, or null.
     *
     * <p>Read from the profile's {@code textures} property rather than the loaded skin: the
     * property arrives with the spawn packet, so a mob is identified the tick it appears
     * rather than whenever its texture finishes downloading.
     */
    public static @Nullable String skinHash(Player player) {
        GameProfile profile = player.getGameProfile();
        if (profile == null) return null;
        for (Property property : profile.properties().get("textures")) {
            try {
                String json = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
                JsonObject textures = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
                if (textures == null) continue;
                JsonObject skin = textures.getAsJsonObject("SKIN");
                if (skin == null || !skin.has("url")) continue;
                String url = skin.get("url").getAsString();
                int slash = url.lastIndexOf('/');
                return slash < 0 ? url : url.substring(slash + 1);
            } catch (RuntimeException ignored) {
                // not a textures blob we understand; try the next property, if any
            }
        }
        return null;
    }
}
