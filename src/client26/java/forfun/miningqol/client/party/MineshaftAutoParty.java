package forfun.miningqol.client.party;

import forfun.miningqol.client.MqoChat;
import forfun.miningqol.client.ShaftESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Warps signed-up players into the mineshaft they asked for.
 *
 * <p>While you are in a mineshaft the sidebar is scanned for a shaft id ({@code JASP_1},
 * {@code TOPA_2}, …), the tab list for corpse counts, and the ESP for a Littlefoot. A
 * player is warped when any of their sign-ups matches. The first is invited with
 * {@code /party <name>} and the rest with {@code /p invite <name>}, capped at
 * {@link #MAX_INVITES}; two seconds after the first person joins the party is warped.
 * If nobody has joined within the configured disband timeout the party is disbanded,
 * player who is offline or has invites blocked cannot wedge the whole thing.
 */
public final class MineshaftAutoParty {
    /** Party of four: you plus three. */
    private static final int MAX_INVITES = 3;
    private static final long WARP_DELAY_MS = 2_000L;
    /** Gap between invites — Hypixel drops commands sent in the same breath. */
    private static final long INVITE_INTERVAL_MS = 600L;
    private static final long DISBAND_DELAY_MS = 2_000L;
    public static final int MIN_DISBAND_SECONDS = 3;
    public static final int MAX_DISBAND_SECONDS = 30;
    private static final long SCAN_INTERVAL_MS = 500L;
    /** Grace period after entering a shaft before committing to a party. */
    private static final long SHAFT_SETTLE_MS = 2_000L;
    /** Consecutive scans without a mineshaft sidebar before the shaft counts as left. */
    private static final int SHAFT_EXIT_SCANS = 4;
    /** Quiet period after a party finishes, so nothing can immediately re-party. */
    private static final long PARTY_COOLDOWN_MS = 10_000L;

    /** A trailing count on a tab list corpse line, as in "Lapis Corpse: 3". */
    private static final Pattern CORPSE_COUNT = Pattern.compile("(\\d+)");

    private enum Stage { IDLE, WAITING_JOIN, WARP_PENDING, DISBAND_PENDING }

    /** What one player is signed up for. Any single match is enough to warp them. */
    private static final class Signup {
        final Set<ShaftType> shafts = EnumSet.noneOf(ShaftType.class);
        final Map<CorpseType, Set<Integer>> corpses = new EnumMap<>(CorpseType.class);
        /** Warp on the ESP seeing a Littlefoot, whatever shaft it is. */
        boolean littlefootMob;

        Set<Integer> countsFor(CorpseType type) {
            return corpses.computeIfAbsent(type, key -> new TreeSet<>());
        }

        boolean isEmpty() {
            if (littlefootMob) return false;
            if (!shafts.isEmpty()) return false;
            for (Set<Integer> counts : corpses.values()) {
                if (!counts.isEmpty()) return false;
            }
            return true;
        }
    }

    private static boolean enabled = false;
    private static boolean disbandAfterWarp = true;
    /** How long to wait for someone to join before giving up on the party. */
    private static int disbandSeconds = 10;

    /** Player name (as typed) -> what they want. Insertion ordered: the list is a priority. */
    private static final Map<String, Signup> signups = new LinkedHashMap<>();

    private static Stage stage = Stage.IDLE;
    private static long stageStartedAt;
    private static long actionAt;
    private static long lastScan;
    /** Set once a party has gone out for the current shaft; cleared on leaving it. */
    private static boolean actedThisShaft;
    private static long shaftSeenAt;
    private static int missedScans;
    private static long partyEndedAt;
    /** True while we are a guest in someone else's party, tracked off Hypixel's chat. */
    private static boolean foreignParty;
    /**
     * True when we arrived in the current shaft as someone else's guest.
     *
     * <p>Captured once on entry and held for the whole shaft: the party that warped us in
     * gets disbanded soon after, and without this the shaft would become fair game the
     * moment it did. Only shafts found under our own steam are partied into.
     */
    private static boolean shaftFromWarp;
    /** Everyone invited for the current shaft, so a top-up cannot double-invite. */
    private static final Set<String> roster = new LinkedHashSet<>();
    /**
     * Highest corpse count seen so far in the current shaft, cleared on leaving it.
     *
     * <p>Looting a corpse flips its tab line to LOOTED, which would otherwise walk the
     * count down while a party is still being assembled — and a shaft that had two Lapis
     * still had two Lapis.
     */
    private static final Map<CorpseType, Integer> shaftCorpsePeak = new EnumMap<>(CorpseType.class);
    private static final java.util.ArrayDeque<String> pendingInvites = new java.util.ArrayDeque<>();
    private static long nextInviteAt;

    private MineshaftAutoParty() {}

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            abort();
            return;
        }

        long now = System.currentTimeMillis();
        advance(client, now);

        if (!enabled) return;
        // Scanning continues through WAITING_JOIN so a match that only becomes visible
        // after the party went out can still be added to it.
        if (stage != Stage.IDLE && stage != Stage.WAITING_JOIN) return;
        if (now - lastScan < SCAN_INTERVAL_MS) return;
        lastScan = now;

        String sidebar = sidebarText(client);
        // Mineshafts only — no other scoreboard should be searched for shaft ids.
        if (sidebar == null || !sidebar.contains("Mineshaft")) {
            // Hypixel rebuilds the scoreboard periodically, so a single empty reading is
            // not proof the shaft was left — and treating it as such re-arms the trigger
            // and parties everyone a second time.
            if (++missedScans >= SHAFT_EXIT_SCANS) {
                actedThisShaft = false;
                shaftSeenAt = 0;
                shaftFromWarp = false;
                shaftCorpsePeak.clear();
            }
            return;
        }
        missedScans = 0;
        if (shaftSeenAt == 0) {
            shaftSeenAt = now;
            shaftFromWarp = foreignParty;
        }

        Map<CorpseType, Integer> corpses = observeCorpses(client);
        // The tab list and the ESP fill in a beat after the shaft loads. Acting on the
        // first scan would commit to whoever matched instantly — an "Any" sign-up always
        // does — and lock out the corpse and Littlefoot sign-ups about to match.
        if (now - shaftSeenAt < SHAFT_SETTLE_MS) return;

        ShaftType shaft = ShaftType.fromScoreboard(sidebar);
        boolean littlefoot = ShaftESP.hasLittlefoot();
        List<String> wanted = playersWanting(shaft, corpses, littlefoot);

        if (stage == Stage.IDLE && !actedThisShaft) {
            // Every command below assumes we lead the party, so stay out of the way
            // while we are a guest in someone else's.
            if (foreignParty || shaftFromWarp || wanted.isEmpty()) return;
            if (now - partyEndedAt < PARTY_COOLDOWN_MS) return;
            actedThisShaft = true;
            startParty(client, describe(shaft, corpses, littlefoot), wanted);
        } else if (stage == Stage.WAITING_JOIN) {
            topUp(wanted);
        }
    }

    /** Queues anyone who started matching after the party went out, up to the cap. */
    private static void topUp(List<String> wanted) {
        for (String name : wanted) {
            if (roster.size() >= MAX_INVITES) return;
            if (roster.add(name)) {
                pendingInvites.add(name);
                MqoChat.reply("§6[Auto Party] §7Late match: §f" + name);
            }
        }
    }

    private static void startParty(Minecraft client, String reason, List<String> wanted) {
        List<String> targets = wanted.size() > MAX_INVITES ? wanted.subList(0, MAX_INVITES) : wanted;

        pendingInvites.clear();
        roster.clear();
        roster.addAll(targets);
        send(client, "party " + targets.get(0));
        for (int i = 1; i < targets.size(); i++) {
            pendingInvites.add(targets.get(i));
        }

        stage = Stage.WAITING_JOIN;
        stageStartedAt = System.currentTimeMillis();
        nextInviteAt = stageStartedAt + INVITE_INTERVAL_MS;
        MqoChat.reply("§6[Auto Party] §f" + reason + " §7→ §f" + String.join("§7, §f", targets)
            + (pendingInvites.isEmpty() ? "" : " §7(+" + pendingInvites.size() + " queued)"));
    }

    private static void advance(Minecraft client, long now) {
        // Drain the invite queue across both stages: someone joining early must not
        // strand the players who have not been invited yet.
        if (!pendingInvites.isEmpty()
            && (stage == Stage.WAITING_JOIN || stage == Stage.WARP_PENDING)
            && now >= nextInviteAt) {
            String invitee = pendingInvites.poll();
            send(client, "p invite " + invitee);
            MqoChat.reply("§6[Auto Party] §7Invited §f" + invitee);
            nextInviteAt = now + INVITE_INTERVAL_MS;
        }

        switch (stage) {
            case WAITING_JOIN -> {
                if (now - stageStartedAt >= disbandSeconds * 1000L) {
                    send(client, "p disband");
                    MqoChat.log("§6[Auto Party] §7Nobody joined in " + disbandSeconds + "s — disbanded.");
                    finishFlow(now);
                }
            }
            case WARP_PENDING -> {
                // Never warp with invites still queued, or the stragglers miss the trip.
                if (pendingInvites.isEmpty() && now >= actionAt) {
                    send(client, "p warp");
                    if (disbandAfterWarp) {
                        stage = Stage.DISBAND_PENDING;
                        actionAt = now + DISBAND_DELAY_MS;
                    } else {
                        finishFlow(now);
                    }
                }
            }
            case DISBAND_PENDING -> {
                if (now >= actionAt) {
                    send(client, "p disband");
                    finishFlow(now);
                }
            }
            default -> {
            }
        }
    }

    /** Ends the current flow and starts the quiet period before another can begin. */
    private static void finishFlow(long now) {
        stage = Stage.IDLE;
        pendingInvites.clear();
        roster.clear();
        partyEndedAt = now;
    }

    /**
     * Tracks party membership and starts the warp countdown on the first join.
     *
     * <p>Membership matters because every command this class sends assumes we are the
     * party leader: inviting, warping and disbanding all fail as a guest.
     */
    public static void onGameMessage(String message) {
        if (message == null) return;
        String clean = message.replaceAll("§.", "");

        // "You have joined Bob's party!" — us joining someone, not someone joining us.
        if (clean.contains("You have joined") && clean.contains("party")) foreignParty = true;
        if (clean.contains("The party was transferred to you")) foreignParty = false;
        if (clean.contains("You left the party")
            || clean.contains("You have been kicked from the party")
            || clean.contains("You are not currently in a party")
            || clean.contains("has disbanded the party")
            || clean.contains("The party was disbanded")) {
            foreignParty = false;
        }

        if (!enabled) return;
        if (stage != Stage.WAITING_JOIN) return;
        if (!clean.contains("joined the party")) return;

        stage = Stage.WARP_PENDING;
        actionAt = System.currentTimeMillis() + WARP_DELAY_MS;
    }

    /** Drops any in-flight party state without sending commands (world change, disconnect). */
    public static void abort() {
        stage = Stage.IDLE;
        actedThisShaft = false;
        shaftSeenAt = 0;
        shaftFromWarp = false;
        missedScans = 0;
        shaftCorpsePeak.clear();
        pendingInvites.clear();
        roster.clear();
    }

    private static void send(Minecraft client, String command) {
        if (client.player == null) return;
        client.player.connection.sendCommand(command);
    }

    // ---- detection ---------------------------------------------------------

    /** The sidebar as one formatting-stripped string, or null when there is no sidebar. */
    private static String sidebarText(Minecraft client) {
        if (client.level == null) return null;
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return null;

        StringBuilder text = new StringBuilder(sidebar.getDisplayName().getString());
        Collection<PlayerTeam> teams = scoreboard.getPlayerTeams();
        for (PlayerTeam team : teams) {
            for (String member : team.getPlayers()) {
                text.append('\n')
                    .append(team.getPlayerPrefix().getString())
                    .append(member)
                    .append(team.getPlayerSuffix().getString());
            }
        }
        return text.toString().replaceAll("§.", "");
    }

    /** Tab list entries, formatting stripped, blanks dropped. */
    private static List<String> tabLines(Minecraft client) {
        List<String> lines = new ArrayList<>();
        if (client.getConnection() == null) return lines;
        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;
            String clean = display.getString().replaceAll("§.", "").trim();
            if (!clean.isEmpty()) lines.add(clean);
        }
        return lines;
    }

    /**
     * Corpse counts off the tab list.
     *
     * <p>Handles both shapes Hypixel could be using: an explicit number on the line
     * ("Lapis Corpse: 3"), which wins, or one line per corpse, which is tallied.
     */
    /**
     * Whether an upper-cased tab line marks a corpse as already taken.
     *
     * <p>Hypixel writes the available state as {@code NOT LOOTED}, which contains the
     * word LOOTED — so the negatives have to be ruled out before the positive.
     */
    private static boolean isLooted(String upperCasedLine) {
        if (!upperCasedLine.contains("LOOTED")) return false;
        return !upperCasedLine.contains("NOT LOOTED") && !upperCasedLine.contains("UNLOOTED");
    }

    /** Folds the current reading into the shaft's running peak and returns that peak. */
    private static Map<CorpseType, Integer> observeCorpses(Minecraft client) {
        for (Map.Entry<CorpseType, Integer> entry : corpseCounts(client).entrySet()) {
            shaftCorpsePeak.merge(entry.getKey(), entry.getValue(), Math::max);
        }
        return shaftCorpsePeak;
    }

    private static Map<CorpseType, Integer> corpseCounts(Minecraft client) {
        Map<CorpseType, Integer> lineCount = new EnumMap<>(CorpseType.class);
        Map<CorpseType, Integer> lastNumber = new EnumMap<>(CorpseType.class);

        for (String line : tabLines(client)) {
            String upper = line.toUpperCase(Locale.ROOT);
            if (isLooted(upper)) continue;

            for (CorpseType type : CorpseType.values()) {
                if (!type.matches(upper)) continue;
                lineCount.merge(type, 1, Integer::sum);
                Matcher matcher = CORPSE_COUNT.matcher(line);
                if (matcher.find()) {
                    try {
                        lastNumber.put(type, Integer.parseInt(matcher.group(1)));
                    } catch (NumberFormatException ignored) {
                        // absurdly long digit run; the line tally still counts it
                    }
                }
                break;   // one corpse per line
            }
        }

        Map<CorpseType, Integer> counts = new EnumMap<>(CorpseType.class);
        for (CorpseType type : CorpseType.values()) {
            Integer lines = lineCount.get(type);
            if (lines == null || lines == 0) continue;
            // One line means a summary ("Lapis Corpse: 3") and the number on it is the
            // count. Several lines means the corpses are listed one per line, and any
            // numbers on them are indices, so the line tally is the count instead.
            Integer number = lastNumber.get(type);
            counts.put(type, lines == 1 && number != null && number > 0 ? number : lines);
        }
        return counts;
    }

    private static List<String> playersWanting(ShaftType shaft, Map<CorpseType, Integer> corpses,
                                               boolean littlefoot) {
        List<String> wanted = new ArrayList<>();
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            if (matches(entry.getValue(), shaft, corpses, littlefoot)) {
                wanted.add(entry.getKey());
            }
        }
        return wanted;
    }

    private static boolean matches(Signup signup, ShaftType shaft, Map<CorpseType, Integer> corpses,
                                   boolean littlefoot) {
        if (signup.shafts.contains(ShaftType.ANY)) return true;
        if (shaft != null && signup.shafts.contains(shaft)) return true;
        // Separate from the LITT_L shaft type on purpose: that is an id on the scoreboard,
        // this is the mob actually being there, which the ESP can see in any shaft.
        if (littlefoot && signup.littlefootMob) return true;

        // Corpses stand on their own: a sign-up with nothing but counts ticked still
        // matches any shaft carrying them, whatever its shaft id.
        for (Map.Entry<CorpseType, Integer> found : corpses.entrySet()) {
            Set<Integer> counts = signup.corpses.get(found.getKey());
            if (counts == null) continue;
            // Each pick is a floor, not an exact count: picking 2 means "2 or more", which
            // is how people read it, and is the only reading that copes with a shaft
            // holding more corpses than the picker can offer.
            for (Integer wanted : counts) {
                if (found.getValue() >= wanted) return true;
            }
        }
        return false;
    }

    private static String describe(ShaftType shaft, Map<CorpseType, Integer> corpses, boolean littlefoot) {
        List<String> parts = new ArrayList<>();
        if (shaft != null) parts.add(shaft.displayName());
        if (littlefoot) parts.add("Littlefoot");
        for (Map.Entry<CorpseType, Integer> found : corpses.entrySet()) {
            parts.add(found.getValue() + "x " + found.getKey().displayName());
        }
        return parts.isEmpty() ? "Mineshaft" : String.join(", ", parts);
    }

    /**
     * Prints what the detector currently sees — the sidebar and tab list it scanned, the
     * shaft it matched, the corpse counts it read, and who is signed up. The point of
     * failure this exists for is an id or count not being where the scan looks, which is
     * otherwise invisible.
     */
    public static void dumpDetection() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        String sidebar = sidebarText(client);
        if (sidebar == null) {
            MqoChat.reply("§6[Auto Party] §cNo sidebar scoreboard to read.");
            return;
        }

        ShaftType shaft = ShaftType.fromScoreboard(sidebar);
        Map<CorpseType, Integer> live = corpseCounts(client);
        Map<CorpseType, Integer> corpses = observeCorpses(client);
        boolean littlefoot = ShaftESP.hasLittlefoot();

        MqoChat.reply("§6[Auto Party] §7In mineshaft: §f" + sidebar.contains("Mineshaft"));
        MqoChat.reply("§6[Auto Party] §7Shaft id: §f"
            + (shaft == null ? "§cnone found" : shaft.scoreboardId() + " §7(" + shaft.displayName() + ")"));
        MqoChat.reply("§6[Auto Party] §7Littlefoot (ESP): §f" + littlefoot);
        MqoChat.reply("§6[Auto Party] §7Guest in another party: §f" + foreignParty);
        MqoChat.reply("§6[Auto Party] §7Warped into this shaft: §f" + shaftFromWarp
            + (shaftFromWarp || foreignParty ? " §7(suspended)" : ""));
        MqoChat.reply("§6[Auto Party] §7Corpses now: §f"
            + (live.isEmpty() ? "§cnone unlooted" : describeCorpses(live)));
        MqoChat.reply("§6[Auto Party] §7Corpses this shaft: §f"
            + (corpses.isEmpty() ? "§cnone seen" : describeCorpses(corpses)));

        List<String> wanted = playersWanting(shaft, corpses, littlefoot);
        MqoChat.reply("§6[Auto Party] §7Would warp: §f"
            + (wanted.isEmpty() ? "§7nobody" : String.join(", ", wanted)));

        // Detected counts repeated here so this block alone explains every verdict.
        MqoChat.reply("§6[Auto Party] §7Sign-ups (" + signups.size() + ") §7vs §f"
            + (corpses.isEmpty() ? "no corpses" : describeCorpses(corpses))
            + (shaft == null ? "" : " §7+ §f" + shaft.displayName())
            + (littlefoot ? " §7+ §fLittlefoot" : ""));
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            Signup signup = entry.getValue();
            boolean hit = matches(signup, shaft, corpses, littlefoot);
            MqoChat.reply((hit ? "§a  MATCH " : "§8  ---   ") + "§f" + entry.getKey()
                + " §7| " + describeSignup(signup));
        }

        MqoChat.reply("§6[Auto Party] §7Sidebar:");
        for (String line : sidebar.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) MqoChat.reply("§8  " + trimmed);
        }
        List<String> tab = tabLines(client);
        MqoChat.reply("§6[Auto Party] §7Tab entries: §f" + tab.size() + " §7(corpse lines below)");
        for (int i = 0; i < tab.size(); i++) {
            String line = tab.get(i);
            String upper = line.toUpperCase(Locale.ROOT);
            CorpseType matched = null;
            for (CorpseType type : CorpseType.values()) {
                if (type.matches(upper)) {
                    matched = type;
                    break;
                }
            }
            boolean corpseish = matched != null || upper.contains("LOOTED") || upper.contains("CORPSE");
            if (!corpseish) continue;

            String tag;
            if (matched == null) tag = " §8[not a corpse] §8";
            else if (isLooted(upper)) tag = " §c[looted] §8";
            else tag = " §a[" + matched.displayName() + "] §8";
            MqoChat.reply("§8  " + i + tag + line);
        }
    }

    /** One player's stored picks, so a sign-up that silently failed to save is visible. */
    private static String describeSignup(Signup signup) {
        List<String> parts = new ArrayList<>();
        if (signup.shafts.isEmpty()) {
            parts.add("shafts: none");
        } else {
            List<String> names = new ArrayList<>();
            for (ShaftType type : signup.shafts) {
                names.add(type.displayName());
            }
            parts.add("shafts: " + String.join("/", names));
        }
        List<String> picks = new ArrayList<>();
        for (Map.Entry<CorpseType, Set<Integer>> corpse : signup.corpses.entrySet()) {
            for (Integer count : corpse.getValue()) {
                picks.add(corpse.getKey().displayName() + " " + count + "+");
            }
        }
        parts.add("corpses: " + (picks.isEmpty() ? "none" : String.join("/", picks)));
        if (signup.littlefootMob) parts.add("littlefoot mob");
        return String.join(" §7| §f", parts);
    }

    private static String describeCorpses(Map<CorpseType, Integer> corpses) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<CorpseType, Integer> found : corpses.entrySet()) {
            parts.add(found.getKey().displayName() + " " + found.getValue());
        }
        return String.join(", ", parts);
    }

    // ---- settings ----------------------------------------------------------

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) abort();
    }

    public static boolean isDisbandAfterWarp() {
        return disbandAfterWarp;
    }

    public static void setDisbandAfterWarp(boolean value) {
        disbandAfterWarp = value;
    }

    public static int getDisbandSeconds() {
        return disbandSeconds;
    }

    public static void setDisbandSeconds(int seconds) {
        disbandSeconds = Math.max(MIN_DISBAND_SECONDS, Math.min(MAX_DISBAND_SECONDS, seconds));
    }

    public static List<String> players() {
        return new ArrayList<>(signups.keySet());
    }

    /** Adds a player if the name looks usable and is not already listed; returns the stored name. */
    public static String addPlayer(String rawName) {
        if (rawName == null) return null;
        String name = rawName.trim();
        if (name.isEmpty() || name.length() > 16 || !name.matches("\\w+")) return null;
        for (String existing : signups.keySet()) {
            if (existing.equalsIgnoreCase(name)) return existing;
        }
        signups.put(name, new Signup());
        return name;
    }

    public static void removePlayer(String name) {
        signups.remove(name);
    }

    public static boolean isSelected(String name, ShaftType type) {
        Signup signup = signups.get(name);
        return signup != null && signup.shafts.contains(type);
    }

    public static boolean isLittlefootMob(String name) {
        Signup signup = signups.get(name);
        return signup != null && signup.littlefootMob;
    }

    public static void toggleLittlefootMob(String name) {
        Signup signup = signups.get(name);
        if (signup == null) return;
        signup.littlefootMob = !signup.littlefootMob;
    }

    public static void toggleType(String name, ShaftType type) {
        Signup signup = signups.get(name);
        if (signup == null) return;
        if (!signup.shafts.remove(type)) signup.shafts.add(type);
    }

    public static boolean isCorpseSelected(String name, CorpseType type, int count) {
        Signup signup = signups.get(name);
        return signup != null && signup.corpses.containsKey(type) && signup.corpses.get(type).contains(count);
    }

    public static void toggleCorpse(String name, CorpseType type, int count) {
        Signup signup = signups.get(name);
        if (signup == null) return;
        Set<Integer> counts = signup.countsFor(type);
        if (!counts.remove(count)) counts.add(count);
    }

    /** How many shafts plus corpse counts a player has ticked — the list row's badge. */
    public static int selectionCount(String name) {
        Signup signup = signups.get(name);
        if (signup == null) return 0;
        int count = signup.shafts.size() + (signup.littlefootMob ? 1 : 0);
        for (Set<Integer> counts : signup.corpses.values()) {
            count += counts.size();
        }
        return count;
    }

    /** How many players have picked at least one thing — the settings card's summary. */
    public static int activeSignupCount() {
        int count = 0;
        for (Signup signup : signups.values()) {
            if (!signup.isEmpty()) count++;
        }
        return count;
    }

    // ---- persistence -------------------------------------------------------

    public static Map<String, List<String>> exportSignups() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            List<String> names = new ArrayList<>();
            for (ShaftType type : entry.getValue().shafts) {
                names.add(type.name());
            }
            out.put(entry.getKey(), names);
        }
        return out;
    }

    /** Corpse picks as {@code "LAPIS:3"} strings, keyed by player. */
    public static Map<String, List<String>> exportCorpseSignups() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            List<String> encoded = new ArrayList<>();
            for (Map.Entry<CorpseType, Set<Integer>> corpse : entry.getValue().corpses.entrySet()) {
                for (Integer count : corpse.getValue()) {
                    encoded.add(corpse.getKey().name() + ":" + count);
                }
            }
            out.put(entry.getKey(), encoded);
        }
        return out;
    }

    /** Names of the players who want the Littlefoot mob, for the config round-trip. */
    public static List<String> exportMobSignups() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            if (entry.getValue().littlefootMob) out.add(entry.getKey());
        }
        return out;
    }

    public static void importSignups(Map<String, List<String>> shafts, Map<String, List<String>> corpses,
                                     List<String> littlefootMob) {
        signups.clear();
        if (shafts != null) {
            for (Map.Entry<String, List<String>> entry : shafts.entrySet()) {
                String name = entry.getKey();
                if (name == null || name.isEmpty()) continue;
                Signup signup = signups.computeIfAbsent(name, key -> new Signup());
                if (entry.getValue() == null) continue;
                for (String raw : entry.getValue()) {
                    ShaftType type = ShaftType.byName(raw);
                    if (type != null) signup.shafts.add(type);
                }
            }
        }
        if (corpses != null) {
            for (Map.Entry<String, List<String>> entry : corpses.entrySet()) {
                String name = entry.getKey();
                if (name == null || name.isEmpty()) continue;
                Signup signup = signups.computeIfAbsent(name, key -> new Signup());
                if (entry.getValue() == null) continue;
                for (String raw : entry.getValue()) {
                    if (raw == null) continue;
                    String[] parts = raw.split(":");
                    if (parts.length != 2) continue;
                    CorpseType type = CorpseType.byName(parts[0]);
                    if (type == null) continue;
                    try {
                        int count = Integer.parseInt(parts[1]);
                        if (count >= 1 && count <= CorpseType.MAX_COUNT) signup.countsFor(type).add(count);
                    } catch (NumberFormatException ignored) {
                        // written by a newer build, or hand-edited; skip the entry
                    }
                }
            }
        }
        if (littlefootMob != null) {
            for (String name : littlefootMob) {
                if (name == null || name.isEmpty()) continue;
                signups.computeIfAbsent(name, key -> new Signup()).littlefootMob = true;
            }
        }
    }
}
