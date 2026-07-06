package forfun.miningqol.client.hotm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 26.1.2 port of the HOTM preset list.
 *
 * On 1.21 this was a fake chest GUI intercepted by HandledScreenMixin; on
 * 26.1.2 it is a real custom Screen. Same interactions as 1.21:
 * left click a preset = load into the editor, right click = load and apply
 * (Auto-HOTM), back button returns to the HOTM config screen.
 *
 * Public static API preserved: open(), handleClick(int,int), isPresetScreen(String).
 */
public class HotmPresetScreen extends Screen {

    private static final int ROW_W = 150;
    private static final int ROW_H = 20;
    private static final int COL_GAP = 10;
    private static final int MAX_PRESETS = 18; // same cap as the 1.21 chest UI

    private static List<String> presetNames;

    private final List<String> names;
    private int rowsPerCol;
    private int cols;
    private int listX;
    private int listY;
    private int backX, backY;
    private static final int BACK_W = 120;
    private static final int BACK_H = 14;

    private HotmPresetScreen(List<String> names) {
        super(Component.literal("HOTM Presets (" + names.size() + " saved)"));
        this.names = names;
    }

    /** Opens the preset list screen (same entry point as 1.21). */
    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        presetNames = HotmTree.listPresets();
        List<String> shown = presetNames.size() > MAX_PRESETS
            ? presetNames.subList(0, MAX_PRESETS)
            : presetNames;
        List<String> namesCopy = List.copyOf(shown);
        client.schedule(() -> client.setScreen(new HotmPresetScreen(namesCopy)));
    }

    /**
     * 1.21 mixin hook, kept for API parity. On 26.1.2 the screen handles its
     * own clicks, so this always reports the click as unhandled.
     */
    public static boolean handleClick(int slotIndex, int button) {
        return false;
    }

    public static boolean isPresetScreen(String title) {
        return title != null && title.startsWith("HOTM Presets");
    }

    @Override
    protected void init() {
        super.init();
        rowsPerCol = Math.max(1, Math.min(names.isEmpty() ? 1 : names.size(), (this.height - 80) / ROW_H));
        cols = names.isEmpty() ? 1 : (names.size() + rowsPerCol - 1) / rowsPerCol;
        int totalW = cols * ROW_W + (cols - 1) * COL_GAP;
        listX = (this.width - totalW) / 2;
        int shownRows = Math.min(names.size(), rowsPerCol);
        listY = Math.max(36, (this.height - shownRows * ROW_H - 40) / 2);
        backX = (this.width - BACK_W) / 2;
        backY = Math.min(this.height - BACK_H - 8, listY + Math.max(shownRows, 1) * ROW_H + 14);
    }

    private int rowX(int index) {
        return listX + (index / rowsPerCol) * (ROW_W + COL_GAP);
    }

    private int rowY(int index) {
        return listY + (index % rowsPerCol) * ROW_H;
    }

    private int presetAt(double mx, double my) {
        for (int i = 0; i < names.size(); i++) {
            int x = rowX(i);
            int y = rowY(i);
            if (mx >= x && mx < x + ROW_W && my >= y && my < y + ROW_H - 2) return i;
        }
        return -1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(ctx, mouseX, mouseY, deltaTicks);

        ctx.centeredText(font, "\u00A76HOTM Presets \u00A77(" + names.size() + " saved)",
            this.width / 2, listY - 18, 0xFFFFFFFF);

        if (names.isEmpty()) {
            ctx.centeredText(font, "\u00A77No presets saved yet", this.width / 2, listY + 4, 0xFFFFFFFF);
        }

        int hovered = presetAt(mouseX, mouseY);
        for (int i = 0; i < names.size(); i++) {
            int x = rowX(i);
            int y = rowY(i);
            boolean hover = i == hovered;
            ctx.fill(x, y, x + ROW_W, y + ROW_H - 2, hover ? 0xE0303040 : 0xC0181820);
            ctx.outline(x, y, ROW_W, ROW_H - 2, hover ? 0xFFFFFFFF : 0xFF404050);
            ctx.item(new ItemStack(Items.WRITTEN_BOOK), x + 1, y + 1);
            ctx.text(font, "\u00A7e" + names.get(i), x + 20, y + 5, 0xFFFFFFFF);
        }

        if (hovered >= 0) {
            ctx.centeredText(font, "\u00A77Left click: Edit \u00A78| \u00A77Right click: Apply",
                this.width / 2, backY - 12, 0xFFFFFFFF);
        }

        boolean backHover = isOverBack(mouseX, mouseY);
        ctx.fill(backX, backY, backX + BACK_W, backY + BACK_H, backHover ? 0xE0303040 : 0xC0202028);
        ctx.outline(backX, backY, BACK_W, BACK_H, backHover ? 0xFFFFFFFF : 0xFF505060);
        ctx.centeredText(font, "\u00A7eBack to HOTM Config", backX + BACK_W / 2, backY + (BACK_H - 8) / 2, 0xFFFFFFFF);
    }

    private boolean isOverBack(double mx, double my) {
        return mx >= backX && mx < backX + BACK_W && my >= backY && my < backY + BACK_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();

        if (isOverBack(mx, my)) {
            HotmChestScreen.open();
            return true;
        }

        int index = presetAt(mx, my);
        if (index >= 0) {
            String name = names.get(index);
            HotmManager.loadPreset(name);

            Minecraft client = Minecraft.getInstance();
            if (click.button() == 1) {
                // Right click = load and apply
                client.schedule(() -> {
                    client.setScreen(null);
                    HotmManager.apply();
                });
            } else {
                // Left click = load into editor
                HotmChestScreen.open();
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
