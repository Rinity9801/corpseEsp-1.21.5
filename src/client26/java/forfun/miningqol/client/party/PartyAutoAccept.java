package forfun.miningqol.client.party;

import forfun.miningqol.client.MqoChat;
import forfun.miningqol.client.PickaxeCooldownHUD;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts party invites from a trusted list of players.
 *
 * <p>The inviter is read out of Hypixel's invite line. Rank prefixes sit in brackets, so
 * the name pattern deliberately excludes {@code [} and {@code ]} and lands on the bare
 * username regardless of rank.
 */
public final class PartyAutoAccept {
    private static final Pattern INVITE =
        Pattern.compile("([A-Za-z0-9_]{1,16}) has invited you to join their party");

    private static boolean enabled = false;
    /** Refuse invites mid-ability, so a warp cannot waste a Mining Speed Boost. */
    private static boolean blockDuringAbility = true;
    /** Refuse invites while in a shaft, so a warp cannot pull you out of one. */
    private static boolean blockInShaft = true;
    private static final Set<String> allowed = new LinkedHashSet<>();

    private PartyAutoAccept() {}

    public static void onGameMessage(String message) {
        if (!enabled || message == null || allowed.isEmpty()) return;

        Matcher matcher = INVITE.matcher(message.replaceAll("§.", ""));
        if (!matcher.find()) return;

        String inviter = matcher.group(1);
        String match = null;
        for (String name : allowed) {
            if (name.equalsIgnoreCase(inviter)) {
                match = name;
                break;
            }
        }
        if (match == null) return;

        // Accepting warps you somewhere else, which is exactly what you do not want
        // partway through an ability or partway through a shaft.
        if (blockDuringAbility && PickaxeCooldownHUD.isAbilityActive()) {
            MqoChat.log("§6[Auto Party] §7Ignored §f" + inviter + "§7 — ability active ("
                + PickaxeCooldownHUD.getActiveSecondsRemaining() + "s left)");
            return;
        }
        if (blockInShaft && MineshaftAutoParty.isInMineshaft()) {
            MqoChat.log("§6[Auto Party] §7Ignored §f" + inviter + "§7 — still in a shaft");
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.connection.sendCommand("p accept " + inviter);
        MqoChat.log("§6[Auto Party] §7Accepted an invite from §f" + inviter);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isBlockDuringAbility() {
        return blockDuringAbility;
    }

    public static void setBlockDuringAbility(boolean value) {
        blockDuringAbility = value;
    }

    public static boolean isBlockInShaft() {
        return blockInShaft;
    }

    public static void setBlockInShaft(boolean value) {
        blockInShaft = value;
    }

    public static List<String> names() {
        return new ArrayList<>(allowed);
    }

    /** Adds a name if it looks usable and is not already listed; returns the stored name. */
    public static String add(String rawName) {
        if (rawName == null) return null;
        String name = rawName.trim();
        if (name.isEmpty() || name.length() > 16 || !name.matches("\\w+")) return null;
        for (String existing : allowed) {
            if (existing.equalsIgnoreCase(name)) return existing;
        }
        allowed.add(name);
        return name;
    }

    public static void remove(String name) {
        allowed.remove(name);
    }

    public static void setNames(List<String> names) {
        allowed.clear();
        if (names == null) return;
        for (String name : names) {
            add(name);
        }
    }
}
