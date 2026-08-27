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
    public static final int MIN_WARP_DELAY_SECONDS = 1;
    public static final int MAX_WARP_DELAY_SECONDS = 15;
    /** Gap between invites — Hypixel drops commands sent in the same breath. */
    private static final long INVITE_INTERVAL_MS = 600L;
    public static final int MIN_DISBAND_SECONDS = 3;
    public static final int MAX_DISBAND_SECONDS = 30;
    private static final long SCAN_INTERVAL_MS = 500L;
    /** Floor on the settle, so the Littlefoot ESP gets a moment even when the tab list is quick. */
    private static final long MIN_SETTLE_MS = 1_500L;
    public static final int MIN_SETTLE_SECONDS = 2;
    public static final int MAX_SETTLE_SECONDS = 15;
    /** Consecutive scans without a mineshaft sidebar before the shaft counts as left. */
    private static final int SHAFT_EXIT_SCANS = 4;
    /** Quiet period after a party finishes, so nothing can immediately re-party. */
    private static final long PARTY_COOLDOWN_MS = 10_000L;
    /** How long a warp keeps disqualifying the next shaft we land in. */
    private static final long WARP_ARRIVAL_WINDOW_MS = 30_000L;

    /** A trailing count on a tab list corpse line, as in "Lapis Corpse: 3". */
    private static final Pattern CORPSE_COUNT = Pattern.compile("(\\d+)");
    /** "Bob joined the party." — rank prefixes sit in brackets, so they cannot be captured. */
    private static final Pattern JOINED =
        Pattern.compile("([A-Za-z0-9_]{1,16}) joined the party");

    private enum Stage { IDLE, WAITING_JOIN, WARP_PENDING, DISBAND_PENDING }

    /** What one player is signed up for. Any single match is enough to warp them. */
    private static final class Signup {
        final Set<ShaftType> shafts = EnumSet.noneOf(ShaftType.class);
        /** Shafts to keep this player out of, whatever else would have matched. */
        final Set<ShaftType> blocked = EnumSet.noneOf(ShaftType.class);
        final Map<CorpseType, Set<Integer>> corpses = new EnumMap<>(CorpseType.class);
        /** Warp on the ESP seeing a Littlefoot, whatever shaft it is. */
        boolean littlefootMob;
        /** Per-player switch: off keeps every pick but skips the player when a shaft is found. */
        boolean enabled = true;

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
    /** Quiet period after the latest join before warping, so stragglers are not left behind. */
    private static float warpDelaySeconds = 5f;
    /** Whether the give-up path disbands, or just stops quietly and leaves the party standing. */
    private static boolean disbandOnTimeout = true;
    /** Longest to wait for the tab list before committing to a party anyway. */
    private static int settleSeconds = 5;
    /** Set while scanning when the tab list's corpse section has rendered. */
    private static boolean sawCorpseSection;

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
    /**
     * Set when a party we are a guest of warps us, and deliberately not cleared by
     * {@link #abort()} — the world change that a warp causes runs abort, so a flag that
     * did not survive it would be gone before the new shaft is ever seen.
     */
    private static boolean warpArrival;
    private static long warpArrivalAt;
    private static boolean debugChat;
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
        // Scanning continues through every live stage. Stopping once somebody joined meant
        // an "Any" sign-up that matched instantly could party, join, and shut the scan down
        // before the tab list had populated — stranding a corpse sign-up that was about to
        // match. Starting a party is still gated on IDLE below.
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
            // A warp seen in the last half minute counts even if the party that sent us
            // has already been disbanded by the time the shaft finished loading.
            shaftFromWarp = foreignParty || (warpArrival && now - warpArrivalAt < WARP_ARRIVAL_WINDOW_MS);
            warpArrival = false;
        }
        // Sticky: being a guest at any point during this shaft is enough to disqualify it,
        // so a disband part way through cannot hand the shaft back to the auto-party.
        if (foreignParty) shaftFromWarp = true;

        Map<CorpseType, Integer> corpses = observeCorpses(client);
        // The tab list and the ESP fill in a beat after the shaft loads, and an "Any"
        // sign-up matches with no data at all. Committing early would party them alone and
        // leave every corpse sign-up to trickle in as a follow-up invite, so hold until the
        // corpse section has rendered — or until the settle runs out, for a shaft that
        // never shows one.
        long waited = now - shaftSeenAt;
        boolean ready = sawCorpseSection && waited >= MIN_SETTLE_MS;
        if (!ready && waited < settleSeconds * 1000L) return;

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
        } else if (stage != Stage.IDLE) {
            topUp(wanted);
        }
    }

    /**
     * Queues anyone who started matching after the party went out, up to the cap.
     *
     * <p>These go through {@code /p invite} rather than {@code /party}, since by now the
     * party exists and they are being added to it. Spaced apart so the throttle does not
     * eat them.
     */
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
        // /party takes every name at once, so the whole group goes out in one command
        // rather than a first invite plus follow-ups that Hypixel's throttle can drop.
        send(client, "party " + String.join(" ", targets));

        stage = Stage.WAITING_JOIN;
        stageStartedAt = System.currentTimeMillis();
        nextInviteAt = stageStartedAt + INVITE_INTERVAL_MS;
        MqoChat.reply("§6[Auto Party] §f" + reason + " §7→ §f" + String.join("§7, §f", targets));
    }

    private static void advance(Minecraft client, long now) {
        // Drain the invite queue across both stages: someone joining early must not
        // strand the players who have not been invited yet.
        if (!pendingInvites.isEmpty() && stage != Stage.IDLE && now >= nextInviteAt) {
            String invitee = pendingInvites.poll();
            send(client, "p invite " + invitee);
            MqoChat.reply("§6[Auto Party] §7Invited §f" + invitee);
            nextInviteAt = now + INVITE_INTERVAL_MS;
        }

        switch (stage) {
            case WAITING_JOIN -> {
                if (now - stageStartedAt >= disbandSeconds * 1000L) {
                    if (disbandOnTimeout) {
                        send(client, "p disband");
                        MqoChat.log("§6[Auto Party] §7Nobody joined in " + disbandSeconds + "s — disbanded.");
                    } else {
                        MqoChat.log("§6[Auto Party] §7Nobody joined in " + disbandSeconds
                            + "s — gave up, party left standing.");
                    }
                    finishFlow(now);
                }
            }
            case WARP_PENDING -> {
                // Never warp with invites still queued, or the stragglers miss the trip.
                if (pendingInvites.isEmpty() && now >= actionAt) {
                    send(client, "p warp");
                    if (disbandAfterWarp) {
                        // Same quiet period as the warp, so the disband is only reached
                        // once nobody new has joined for that long.
                        stage = Stage.DISBAND_PENDING;
                        actionAt = now + (long) (warpDelaySeconds * 1000f);
                    } else {
                        finishFlow(now);
                    }
                }
            }
            case DISBAND_PENDING -> {
                // Never disband on someone who has an invite still in flight.
                if (pendingInvites.isEmpty() && now >= actionAt) {
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

        if (debugChat) {
            String lower = clean.toLowerCase(Locale.ROOT);
            if (lower.contains("party") || lower.contains("warp")) {
                MqoChat.reply("§8[party-chat] §7" + clean);
            }
        }

        // "You have joined Bob's party!" — us joining someone, not someone joining us.
        if (clean.contains("You have joined") && clean.contains("party")) foreignParty = true;
        if (clean.contains("The party was transferred to you")) foreignParty = false;
        // Hypixel's party warp reads "Party Leader, <name>, summoned you to their server."
        // Latched here because the party is routinely disbanded seconds later, before the
        // new shaft's scoreboard has populated — and because the world change in between
        // runs abort(), which is why this flag is deliberately not cleared there.
        if (foreignParty && (clean.contains("summoned you to") || clean.contains("warped"))) {
            warpArrival = true;
            warpArrivalAt = System.currentTimeMillis();
        }
        if (clean.contains("You left the party")
            || clean.contains("You have been kicked from the party")
            || clean.contains("You are not currently in a party")
            || clean.contains("has disbanded the party")
            || clean.contains("The party was disbanded")) {
            foreignParty = false;
        }

        if (!enabled) return;
        // Accepted in every live stage. A join during WARP_PENDING pushes the warp back;
        // a join during DISBAND_PENDING pulls the flow back to a warp, so someone who
        // accepts just after the trip went out gets taken along instead of disbanded on.
        if (stage != Stage.WAITING_JOIN && stage != Stage.WARP_PENDING
            && stage != Stage.DISBAND_PENDING) return;
        if (!clean.contains("joined the party")) return;

        Matcher joiner = JOINED.matcher(clean);
        String name = joiner.find() ? joiner.group(1) : null;

        boolean afterWarp = stage == Stage.DISBAND_PENDING;
        stage = Stage.WARP_PENDING;
        actionAt = System.currentTimeMillis() + (long) (warpDelaySeconds * 1000f);
        MqoChat.reply("§6[Auto Party] §f" + (name == null ? "Someone" : name)
            + " §7joined — warping in " + formatSeconds(warpDelaySeconds) + "s"
            + (afterWarp ? " §7(late, warping again)" : ""));
    }

    /**
     * Cancels a party that is already out, disbanding it first.
     *
     * <p>Switching warps off or dropping a player only stopped the *next* party before;
     * anyone already invited could still accept and be warped, which is not what either
     * action looks like it should do.
     */
    private static void cancelActiveParty(String reason) {
        if (stage == Stage.IDLE) {
            abort();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) send(client, "p disband");
        MqoChat.reply("§6[Auto Party] §7" + reason + " — party cancelled.");
        long now = System.currentTimeMillis();
        abort();
        partyEndedAt = now;
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

    /** Echoes party and warp chat lines, for pinning down Hypixel's exact wording. */
    public static boolean toggleChatDebug() {
        debugChat = !debugChat;
        return debugChat;
    }

    /**
     * Dumps every tab list entry, unfiltered.
     *
     * <p>Reads {@code getOnlinePlayers()} rather than the listed-only view the scanners
     * use, and marks which entries are missing from that view — an unlisted entry is
     * invisible to the corpse and forge parsers, which is one way a corpse that is plainly
     * on screen never gets counted.
     */
    public static void dumpTabList() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            MqoChat.reply("§6[Tab] §cNot connected.");
            return;
        }

        Collection<PlayerInfo> all = client.getConnection().getOnlinePlayers();
        Set<PlayerInfo> listed = new LinkedHashSet<>(client.getConnection().getListedOnlinePlayers());
        MqoChat.reply("§6[Tab] §7entries: §f" + all.size() + " §7listed: §f" + listed.size()
            + " §8(* = unlisted, invisible to the scanners)");

        int index = 0;
        for (PlayerInfo info : all) {
            Component display = info.getTabListDisplayName();
            String text;
            if (display == null) {
                String profile = info.getProfile() == null ? null : info.getProfile().name();
                text = "§8<no display name> " + (profile == null ? "" : profile);
            } else {
                String clean = display.getString().replaceAll("§.", "");
                text = clean.trim().isEmpty() ? "§8<blank>" : "§7" + clean;
            }
            MqoChat.reply((listed.contains(info) ? "§8 " : "§c*") + index + " " + text);
            index++;
        }
        MqoChat.reply("§6[Tab] §8Order is the packet's, not the on-screen order.");
    }

    /** Whether the sidebar says we are in a mineshaft right now. */
    public static boolean isInMineshaft() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return false;
        String sidebar = sidebarText(client);
        return sidebar != null && sidebar.contains("Mineshaft");
    }

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
        sawCorpseSection = false;

        for (String line : tabLines(client)) {
            String upper = line.toUpperCase(Locale.ROOT);
            // The "Frozen Corpses:" header shows the section has rendered, whether or not
            // this shaft actually holds any.
            if (upper.contains("CORPSE")) sawCorpseSection = true;
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
        // Paused players keep their picks but never get invited.
        if (!signup.enabled) return false;
        // A block beats every positive rule, including Any, corpses and the Littlefoot.
        if (shaft != null && signup.blocked.contains(shaft)) return false;

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
        MqoChat.reply("§6[Auto Party] §7Corpse section rendered: §f" + sawCorpseSection);
        MqoChat.reply("§6[Auto Party] §7Guest in another party: §f" + foreignParty
            + " §7(chat debug: " + debugChat + ")");
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
        if (!signup.blocked.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ShaftType type : signup.blocked) {
                names.add(type.displayName());
            }
            parts.add("blocked: " + String.join("/", names));
        }
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
        boolean wasEnabled = enabled;
        enabled = value;
        if (!value && wasEnabled) cancelActiveParty("Warps switched off");
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

    public static boolean isDisbandOnTimeout() {
        return disbandOnTimeout;
    }

    public static void setDisbandOnTimeout(boolean value) {
        disbandOnTimeout = value;
    }

    public static int getSettleSeconds() {
        return settleSeconds;
    }

    public static void setSettleSeconds(int seconds) {
        settleSeconds = Math.max(MIN_SETTLE_SECONDS, Math.min(MAX_SETTLE_SECONDS, seconds));
    }

    public static float getWarpDelaySeconds() {
        return warpDelaySeconds;
    }

    /** Half-second steps are enough here, and keep the chat message readable. */
    public static void setWarpDelaySeconds(float seconds) {
        float clamped = Math.max(MIN_WARP_DELAY_SECONDS, Math.min(MAX_WARP_DELAY_SECONDS, seconds));
        warpDelaySeconds = Math.round(clamped * 2f) / 2f;
    }

    private static String formatSeconds(float seconds) {
        return seconds == Math.floor(seconds) ? String.valueOf((int) seconds)
            : String.format(java.util.Locale.US, "%.1f", seconds);
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
        // They may already hold an invite from the party that is out right now.
        if (roster.contains(name)) cancelActiveParty("Removed " + name);
    }

    public static boolean isPlayerEnabled(String name) {
        Signup signup = signups.get(name);
        return signup != null && signup.enabled;
    }

    public static void togglePlayerEnabled(String name) {
        Signup signup = signups.get(name);
        if (signup == null) return;
        signup.enabled = !signup.enabled;
        // They may already hold an invite from the party that is out right now.
        if (!signup.enabled && roster.contains(name)) cancelActiveParty("Paused " + name);
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
        if (!signup.shafts.remove(type)) {
            signup.shafts.add(type);
            signup.blocked.remove(type);   // wanting and blocking the same shaft is meaningless
        }
    }

    public static boolean isBlocked(String name, ShaftType type) {
        Signup signup = signups.get(name);
        return signup != null && signup.blocked.contains(type);
    }

    public static void toggleBlocked(String name, ShaftType type) {
        Signup signup = signups.get(name);
        if (signup == null || type == ShaftType.ANY) return;
        if (!signup.blocked.remove(type)) {
            signup.blocked.add(type);
            signup.shafts.remove(type);
        }
    }

    public static int blockedCount(String name) {
        Signup signup = signups.get(name);
        return signup == null ? 0 : signup.blocked.size();
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

    /** How many enabled players have picked at least one thing — the settings card's summary. */
    public static int activeSignupCount() {
        int count = 0;
        for (Signup signup : signups.values()) {
            if (signup.enabled && !signup.isEmpty()) count++;
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

    /** Blocked shaft types per player, for the config round-trip. */
    public static Map<String, List<String>> exportBlockedSignups() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            List<String> names = new ArrayList<>();
            for (ShaftType type : entry.getValue().blocked) {
                names.add(type.name());
            }
            out.put(entry.getKey(), names);
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

    /** Names of the players switched off, for the config round-trip. */
    public static List<String> exportDisabledPlayers() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Signup> entry : signups.entrySet()) {
            if (!entry.getValue().enabled) out.add(entry.getKey());
        }
        return out;
    }

    public static void importSignups(Map<String, List<String>> shafts, Map<String, List<String>> corpses,
                                     List<String> littlefootMob, Map<String, List<String>> blocked,
                                     List<String> disabled) {
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
        if (blocked != null) {
            for (Map.Entry<String, List<String>> entry : blocked.entrySet()) {
                String name = entry.getKey();
                if (name == null || name.isEmpty() || entry.getValue() == null) continue;
                Signup signup = signups.computeIfAbsent(name, key -> new Signup());
                for (String raw : entry.getValue()) {
                    ShaftType type = ShaftType.byName(raw);
                    if (type != null && type != ShaftType.ANY) signup.blocked.add(type);
                }
            }
        }
        if (disabled != null) {
            for (String name : disabled) {
                Signup signup = name == null ? null : signups.get(name);
                if (signup != null) signup.enabled = false;
            }
        }
    }
}
