package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HUD listing the forge slots read off the tab list.
 *
 * <p>Hypixel writes them as {@code 7) Refined Tungsten: 59m} under a Forges header, or
 * {@code 2) EMPTY} for a free slot. Times come straight from the server rather than being
 * counted down locally, so the HUD never claims a forge is ready before Hypixel says so.
 */
public final class ForgeDisplay {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "forge_display");
    /** {@code 7) Refined Tungsten: 59m} and {@code 2) EMPTY}. */
    private static final Pattern SLOT = Pattern.compile("^(\\d+)\\)\\s*(.+?)\\s*$");
    private static final Pattern DURATION =
        Pattern.compile("(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?");
    private static final long REFRESH_MS = 1_000L;

    /** One forge slot as the tab list last described it. */
    private record Slot(int number, String item, String time, long seconds, boolean ready, boolean empty) {}

    private static boolean registered = false;
    private static boolean enabled = false;
    private static boolean showEmpty = false;
    private static boolean sortByTime = true;

    private static final HudAnchor ANCHOR = new HudAnchor(10, 90, ForgeDisplay::getWidth, ForgeDisplay::getHeight);

    private static final float[] titleColor = {1.0f, 170.0f / 255.0f, 0.0f};
    private static final float[] itemColor = {1.0f, 1.0f, 1.0f};
    private static final float[] timeColor = {170.0f / 255.0f, 170.0f / 255.0f, 170.0f / 255.0f};
    private static final float[] readyColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    private static final float[] emptyColor = {105.0f / 255.0f, 105.0f / 255.0f, 105.0f / 255.0f};

    private static List<Slot> slots = new ArrayList<>();
    private static long lastRefresh = 0;

    private ForgeDisplay() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    public static void tick() {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        if (now - lastRefresh < REFRESH_MS) return;
        lastRefresh = now;
        slots = readSlots();
    }

    private static List<Slot> readSlots() {
        List<Slot> found = new ArrayList<>();
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return found;

        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;

            String line = display.getString().replaceAll("§.", "").trim();
            Matcher matcher = SLOT.matcher(line);
            if (!matcher.matches()) continue;

            int number;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                continue;
            }
            String body = matcher.group(2);

            if (body.equalsIgnoreCase("EMPTY")) {
                found.add(new Slot(number, "Empty", "", Long.MAX_VALUE, false, true));
                continue;
            }

            // Everything before the last colon is the item; what follows is the time.
            int split = body.lastIndexOf(':');
            if (split <= 0 || split == body.length() - 1) continue;
            String item = body.substring(0, split).trim();
            String time = body.substring(split + 1).trim();
            if (item.isEmpty() || time.isEmpty()) continue;

            boolean ready = time.toLowerCase(Locale.ROOT).contains("ready");
            found.add(new Slot(number, item, time, ready ? 0 : parseDuration(time), ready, false));
        }

        found.sort((a, b) -> {
            if (sortByTime && a.seconds() != b.seconds()) return Long.compare(a.seconds(), b.seconds());
            return Integer.compare(a.number(), b.number());
        });
        return found;
    }

    /** "1h20m" / "59m" / "45s" to seconds; unparseable text sorts last. */
    private static long parseDuration(String text) {
        Matcher matcher = DURATION.matcher(text.replace(" ", ""));
        if (!matcher.find() || matcher.group(0).isEmpty()) return Long.MAX_VALUE - 1;
        long seconds = 0;
        seconds += group(matcher, 1) * 86_400L;
        seconds += group(matcher, 2) * 3_600L;
        seconds += group(matcher, 3) * 60L;
        seconds += group(matcher, 4);
        return seconds == 0 ? Long.MAX_VALUE - 1 : seconds;
    }

    private static long group(Matcher matcher, int index) {
        String value = matcher.group(index);
        if (value == null) return 0;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static void render(GuiGraphicsExtractor ctx) {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        List<Component> lines = buildLines();
        if (lines.isEmpty()) return;

        Font font = client.font;
        int y = ANCHOR.y();
        for (Component line : lines) {
            ctx.text(font, line, ANCHOR.x(), y, 0xFFFFFFFF, true);
            y += 10;
        }
    }

    private static List<Component> buildLines() {
        List<Component> lines = new ArrayList<>();
        List<Slot> current = slots;
        boolean any = false;
        for (Slot slot : current) {
            if (!slot.empty() || showEmpty) {
                any = true;
                break;
            }
        }
        if (!any) return lines;

        lines.add(Component.literal("Forges:").setStyle(Style.EMPTY.withColor(toRgb(titleColor))));
        for (Slot slot : current) {
            if (slot.empty() && !showEmpty) continue;
            lines.add(formatSlot(slot));
        }
        return lines;
    }

    private static Component formatSlot(Slot slot) {
        if (slot.empty()) {
            return Component.literal(slot.number() + ") Empty")
                .setStyle(Style.EMPTY.withColor(toRgb(emptyColor)));
        }
        float[] valueColor = slot.ready() ? readyColor : timeColor;
        MutableComponent text = Component.literal(slot.number() + ") ")
            .setStyle(Style.EMPTY.withColor(toRgb(timeColor)));
        text.append(Component.literal(slot.item()).setStyle(
            Style.EMPTY.withColor(toRgb(slot.ready() ? readyColor : itemColor))));
        return text.append(Component.literal(": " + slot.time()).setStyle(
            Style.EMPTY.withColor(toRgb(valueColor)).withBold(slot.ready())));
    }

    /** Sample used by the HUD mover, so the box has a sensible size with no forge running. */
    public static List<Component> getPreviewLines() {
        List<Component> lines = buildLines();
        if (!lines.isEmpty()) return lines;
        lines.add(Component.literal("Forges:").setStyle(Style.EMPTY.withColor(toRgb(titleColor))));
        lines.add(formatSlot(new Slot(1, "Refined Tungsten", "59m", 3_540, false, false)));
        lines.add(formatSlot(new Slot(2, "Refined Diamond", "Ready!", 0, true, false)));
        return lines;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) slots = new ArrayList<>();
    }

    public static boolean isShowEmpty() {
        return showEmpty;
    }

    public static void setShowEmpty(boolean value) {
        showEmpty = value;
    }

    public static boolean isSortByTime() {
        return sortByTime;
    }

    public static void setSortByTime(boolean value) {
        sortByTime = value;
    }

    public static int getX() {
        return ANCHOR.x();
    }

    public static int getY() {
        return ANCHOR.y();
    }

    public static void setPosition(int x, int y) {
        ANCHOR.set(x, y);
    }

    /** Edge anchor for the config — see {@link HudAnchor}. */
    public static HudAnchor anchor() { return ANCHOR; }

    public static int getWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return 120;
        int width = 0;
        for (Component line : getPreviewLines()) {
            width = Math.max(width, client.font.width(line));
        }
        return Math.max(60, width);
    }

    public static int getHeight() {
        return Math.max(10, getPreviewLines().size() * 10);
    }

    public static float[] getTitleColor() {
        return titleColor.clone();
    }

    public static void setTitleColor(float red, float green, float blue) {
        setColor(titleColor, red, green, blue);
    }

    public static float[] getItemColor() {
        return itemColor.clone();
    }

    public static void setItemColor(float red, float green, float blue) {
        setColor(itemColor, red, green, blue);
    }

    public static float[] getTimeColor() {
        return timeColor.clone();
    }

    public static void setTimeColor(float red, float green, float blue) {
        setColor(timeColor, red, green, blue);
    }

    public static float[] getReadyColor() {
        return readyColor.clone();
    }

    public static void setReadyColor(float red, float green, float blue) {
        setColor(readyColor, red, green, blue);
    }

    private static int toRgb(float[] color) {
        int red = Math.round(color[0] * 255.0f);
        int green = Math.round(color[1] * 255.0f);
        int blue = Math.round(color[2] * 255.0f);
        return (red << 16) | (green << 8) | blue;
    }

    private static void setColor(float[] color, float red, float green, float blue) {
        color[0] = clampColor(red);
        color[1] = clampColor(green);
        color[2] = clampColor(blue);
    }

    private static float clampColor(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
