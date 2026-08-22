package forfun.miningqol.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Scans and suppresses client-side sounds.
 *
 * <p>Every sound funnels through {@code SoundEngine.play}, which
 * {@code forfun.miningqol.mixin.client.SoundEngineMixin} hooks — so this sees Hypixel's
 * custom sounds (which are vanilla ids replayed at odd pitches) as well as vanilla ones.
 *
 * <p>Scan mode records each distinct id+pitch pair once and prints it with a clickable
 * [block] button; the rules it produces are what actually silence a sound.
 *
 * <p>Rule syntax: {@code <id>} blocks every pitch, {@code <id>@<pitch>} blocks only that
 * pitch (±0.01). {@code *} is a wildcard in the id, e.g. {@code *.enderman.*}.
 */
public class SoundBlocker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sybau/Sound");
    private static final int MAX_CAPTURED = 2000;
    private static final float PITCH_TOLERANCE = 0.01f;

    private static boolean scanning = false;
    private static boolean verbose = false;
    private static boolean blockingEnabled = true;

    // Counts every sound the mixin hands us, scanning or not — if this stays 0 the hook
    // isn't firing, which is the first thing to rule out when a sound doesn't show up.
    private static int totalSeen = 0;
    private static boolean capWarned = false;
    private static boolean readWarned = false;

    // Key is "id@pitch" (pitch to 2dp) so the same sound at a different pitch — how Hypixel
    // distinguishes most of its custom sounds — shows up as its own entry.
    private static final Map<String, Capture> captured = new LinkedHashMap<>();
    private static final List<Rule> rules = new ArrayList<>();

    public static class Capture {
        public final String id;
        public final float pitch;
        public final String source;
        public float volume;
        public int count;

        Capture(String id, float pitch, String source, float volume) {
            this.id = id;
            this.pitch = pitch;
            this.source = source;
            this.volume = volume;
            this.count = 0;
        }

        public String key() {
            return id + "@" + fmt(pitch);
        }
    }

    private static class Rule {
        final String raw;
        final String idPattern;   // lowercase, may contain '*'
        final Float pitch;        // null = any pitch

        Rule(String raw, String idPattern, Float pitch) {
            this.raw = raw;
            this.idPattern = idPattern;
            this.pitch = pitch;
        }

        boolean matches(String id, float actualPitch) {
            if (pitch != null && Math.abs(pitch - actualPitch) > PITCH_TOLERANCE) return false;
            return matchesId(id);
        }

        private boolean matchesId(String id) {
            if (idPattern.indexOf('*') < 0) return idPattern.equals(id);
            // Simple glob: split on '*' and walk the literal chunks in order.
            String[] parts = idPattern.split("\\*", -1);
            int cursor = 0;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) continue;
                if (i == 0) {
                    if (!id.startsWith(part)) return false;
                    cursor = part.length();
                    continue;
                }
                int at = id.indexOf(part, cursor);
                if (at < 0) return false;
                cursor = at + part.length();
            }
            String last = parts[parts.length - 1];
            return last.isEmpty() || id.endsWith(last);
        }
    }

    // ---------------------------------------------------------------- hook entry point

    /**
     * Called from the SoundEngine mixin for every sound about to play.
     *
     * @return true if the sound should be suppressed
     */
    public static boolean handle(SoundInstance instance) {
        totalSeen++;

        String id;
        float pitch;
        float volume;
        String source;
        try {
            id = instance.getIdentifier().toString();
            pitch = instance.getPitch();
            volume = instance.getVolume();
            source = instance.getSource().getName();
        } catch (Throwable t) {
            // Shouldn't happen now the hook sits after resolve(), but never let a sound
            // crash the engine. Logged once so a bad instance can't flood the log.
            if (!readWarned) {
                readWarned = true;
                LOGGER.warn("[SoundScan] unreadable sound instance {}", instance.getClass().getName(), t);
            }
            return false;
        }

        if (scanning) {
            record(instance, id, pitch, volume, source);
        }
        if (!blockingEnabled) return false;

        String lower = id.toLowerCase(Locale.ROOT);
        synchronized (rules) {
            for (Rule rule : rules) {
                if (rule.matches(lower, pitch)) return true;
            }
        }
        return false;
    }

    private static void record(SoundInstance instance, String id, float pitch, float volume, String source) {
        // Everything goes to the log, so a sound that never reaches chat is still recoverable.
        LOGGER.info("[SoundScan] {} pitch={} vol={} source={} class={}",
            id, fmt(pitch), fmt(volume), source, instance.getClass().getSimpleName());

        String key = id + "@" + fmt(pitch);
        Capture capture;
        boolean isNew;
        synchronized (captured) {
            capture = captured.get(key);
            isNew = capture == null;
            if (isNew) {
                if (captured.size() >= MAX_CAPTURED) {
                    if (!capWarned) {
                        capWarned = true;
                        info("§cCapture list full (" + MAX_CAPTURED + ") — run §f/soundscan clear");
                    }
                    return;
                }
                capture = new Capture(id, pitch, source, volume);
                captured.put(key, capture);
            }
            capture.count++;
            capture.volume = volume;
        }

        if (!isNew && !verbose) return;

        int index;
        synchronized (captured) {
            index = new ArrayList<>(captured.keySet()).indexOf(key) + 1;
        }
        MutableComponent line = Component.literal("§6[MQO] ")
            .append(Component.literal("#" + index + " ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(id).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" pitch ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(fmt(pitch)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" vol ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(fmt(volume)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" [" + source + "]").withStyle(ChatFormatting.DARK_GRAY));

        if (verbose) {
            try {
                line.append(Component.literal(String.format(Locale.ROOT, " @%.0f,%.0f,%.0f",
                    instance.getX(), instance.getY(), instance.getZ())).withStyle(ChatFormatting.DARK_GRAY));
            } catch (Throwable ignored) {}
        }

        line.append(blockButton(id + "@" + fmt(pitch), " [block]"))
            .append(blockButton(id, " [block-all-pitches]"));
        sendChat(line);
    }

    private static MutableComponent blockButton(String rule, String label) {
        return Component.literal(label).withStyle(style -> style
            .withColor(ChatFormatting.RED)
            .withClickEvent(new ClickEvent.RunCommand("soundblock add " + rule))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Block " + rule))));
    }

    // ---------------------------------------------------------------- scan controls

    public static void setScanning(boolean on) {
        scanning = on;
        info(on
            ? "§aSound scan ON §7- play the sound now, then §f/soundscan list"
            : "§cSound scan OFF");
    }

    public static boolean isScanning() {
        return scanning;
    }

    public static void toggleScanning() {
        setScanning(!scanning);
    }

    public static void setVerbose(boolean on) {
        verbose = on;
        info(on ? "§eVerbose scan ON §7(logs every repeat)" : "§7Verbose scan OFF");
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static void clearCaptured() {
        synchronized (captured) {
            captured.clear();
        }
        capWarned = false;
        info("§7Cleared captured sounds.");
    }

    /** Diagnostic: if "sounds seen" is 0 the mixin isn't firing at all. */
    public static void status() {
        info("§fSound scanner status:");
        info("  scanning: " + (scanning ? "§aON" : "§cOFF") + " §7| verbose: " + (verbose ? "§aON" : "§7off"));
        info("  §7sounds seen by hook: §b" + totalSeen);
        info("  §7captured (unique): §b" + capturedList().size());
        info("  §7blocking: " + (blockingEnabled ? "§aON" : "§cOFF") + " §7| rules: §b" + getRules().size());
    }

    public static void listCaptured() {
        List<Capture> list = capturedList();
        if (list.isEmpty()) {
            info("§7No sounds captured. Run §f/soundscan§7 then trigger the sound.");
            return;
        }
        info("§fCaptured sounds (§b" + list.size() + "§f):");
        for (int i = 0; i < list.size(); i++) {
            Capture c = list.get(i);
            MutableComponent line = Component.literal("  ")
                .append(Component.literal("#" + (i + 1) + " ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(c.id).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" p" + fmt(c.pitch)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" v" + fmt(c.volume)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" x" + c.count).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" [" + c.source + "]").withStyle(ChatFormatting.DARK_GRAY))
                .append(blockButton(c.key(), " [block]"))
                .append(blockButton(c.id, " [all]"));
            sendChat(line);
        }
    }

    public static List<Capture> capturedList() {
        synchronized (captured) {
            return new ArrayList<>(captured.values());
        }
    }

    /** Adds a block rule from a 1-based index into the captured list. */
    public static void blockCaptured(int index, boolean allPitches) {
        List<Capture> list = capturedList();
        if (index < 1 || index > list.size()) {
            info("§cNo captured sound #" + index + " §7(have " + list.size() + ")");
            return;
        }
        Capture c = list.get(index - 1);
        addRule(allPitches ? c.id : c.key());
    }

    // ---------------------------------------------------------------- block rules

    public static void addRule(String raw) {
        Rule rule = parse(raw);
        if (rule == null) {
            info("§cBad rule: §f" + raw + " §7(use id or id@pitch)");
            return;
        }
        synchronized (rules) {
            for (Rule existing : rules) {
                if (existing.raw.equalsIgnoreCase(rule.raw)) {
                    info("§7Already blocked: §f" + rule.raw);
                    return;
                }
            }
            rules.add(rule);
        }
        info("§cBlocked §f" + rule.raw);
        saveConfig();
    }

    public static void removeRule(int index) {
        String removed;
        synchronized (rules) {
            if (index < 1 || index > rules.size()) {
                info("§cNo rule #" + index + " §7(have " + rules.size() + ")");
                return;
            }
            removed = rules.remove(index - 1).raw;
        }
        info("§aUnblocked §f" + removed);
        saveConfig();
    }

    public static void clearRules() {
        synchronized (rules) {
            rules.clear();
        }
        info("§aCleared all sound blocks.");
        saveConfig();
    }

    public static void listRules() {
        List<String> raws = getRules();
        if (raws.isEmpty()) {
            info("§7No sounds blocked. Add one with §f/soundblock add <id>[@pitch]");
            return;
        }
        info("§fBlocked sounds (§c" + raws.size() + "§f)"
            + (blockingEnabled ? "" : " §e- blocking is currently OFF") + ":");
        for (int i = 0; i < raws.size(); i++) {
            final int number = i + 1;
            MutableComponent line = Component.literal("  ")
                .append(Component.literal("#" + number + " ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(raws.get(i)).withStyle(ChatFormatting.RED))
                .append(Component.literal(" [remove]").withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent.RunCommand("soundblock remove " + number))));
            sendChat(line);
        }
    }

    private static Rule parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return null;
        String idPart = trimmed;
        Float pitch = null;
        int at = trimmed.lastIndexOf('@');
        if (at > 0) {
            try {
                pitch = Float.parseFloat(trimmed.substring(at + 1));
                idPart = trimmed.substring(0, at);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        idPart = idPart.toLowerCase(Locale.ROOT);
        // Bare "entity.enderman.teleport" means the vanilla namespace.
        if (idPart.indexOf(':') < 0 && idPart.indexOf('*') < 0) {
            idPart = "minecraft:" + idPart;
        }
        String canonical = pitch == null ? idPart : idPart + "@" + fmt(pitch);
        return new Rule(canonical, idPart, pitch);
    }

    /**
     * The corpse "ding" on Hypixel is a note-block harp, blocked at every pitch.
     *
     * <p>Backed by a normal rule rather than its own flag, so the Misc toggle, /soundblock
     * list and the saved config can't disagree about whether it's on.
     */
    public static final String CORPSE_DING_RULE = "minecraft:block.note_block.harp";

    public static boolean isCorpseDingBlocked() {
        synchronized (rules) {
            for (Rule rule : rules) {
                if (rule.raw.equalsIgnoreCase(CORPSE_DING_RULE)) return true;
            }
        }
        return false;
    }

    public static void setCorpseDingBlocked(boolean blocked) {
        if (blocked == isCorpseDingBlocked()) return;
        synchronized (rules) {
            if (blocked) {
                Rule rule = parse(CORPSE_DING_RULE);
                if (rule != null) rules.add(rule);
            } else {
                rules.removeIf(rule -> rule.raw.equalsIgnoreCase(CORPSE_DING_RULE));
            }
        }
        saveConfig();
    }

    public static void setBlockingEnabled(boolean on) {
        blockingEnabled = on;
    }

    public static boolean isBlockingEnabled() {
        return blockingEnabled;
    }

    /** Rule strings for config persistence. */
    public static List<String> getRules() {
        synchronized (rules) {
            List<String> out = new ArrayList<>(rules.size());
            for (Rule r : rules) out.add(r.raw);
            return out;
        }
    }

    /** Replaces every rule (config load). Silent — no chat, no re-save. */
    public static void setRules(List<String> raws) {
        synchronized (rules) {
            rules.clear();
            if (raws == null) return;
            for (String raw : raws) {
                Rule rule = parse(raw);
                if (rule != null) rules.add(rule);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void saveConfig() {
        MiningqolClient.saveConfig();
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void info(String message) {
        sendChat(Component.literal("§6[MQO] §f" + message));
    }

    private static void sendChat(Component message) {
        // reply, not log: the scanner only talks when you explicitly turned it on, so
        // muting the mod's chat logs shouldn't leave it silently doing nothing.
        MqoChat.reply(message);
    }
}
