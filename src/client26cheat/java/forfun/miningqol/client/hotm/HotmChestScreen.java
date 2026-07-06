package forfun.miningqol.client.hotm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.1.2 port of the HOTM config screen.
 *
 * On 1.21 this was a fake chest GUI (client-side GenericContainerScreen) whose
 * clicks were intercepted by HandledScreenMixin. The 26.1.2 GUI overhaul removed
 * immediate-mode rendering, so this is now a real custom Screen that draws the
 * whole 10-row tree at once (no paging needed) and handles its own input via
 * MouseButtonEvent / KeyEvent / CharacterEvent.
 *
 * The public static API of the 1.21 version is preserved:
 * open(), handleClick(int,int), isHotmScreen(String), getCurrentPage().
 * handleClick/getCurrentPage only existed for the 1.21 chest mixin and are
 * inert here (no fake chest slots exist on 26.1.2).
 */
public class HotmChestScreen extends Screen {

    private static final int CELL = 18;          // grid cell size (16px item + 1px border)
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = HotmNode.TREE_ROWS;
    private static final int GRID_W = GRID_COLS * CELL;
    private static final int GRID_H = GRID_ROWS * CELL;
    private static final int BTN_W = 96;
    private static final int BTN_H = 14;

    // GLFW key codes
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_KP_ENTER = 335;
    private static final int KEY_BACKSPACE = 259;

    private int gridX;
    private int gridY;
    private final List<Button> buttons = new ArrayList<>();

    // "Save preset" name-entry overlay (replaces the 1.21 Vexel HotmSaveScreen)
    private boolean saveMode = false;
    private final StringBuilder saveName = new StringBuilder();

    private static class Button {
        final String id;
        int x, y, w, h;
        Button(String id) { this.id = id; }
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public HotmChestScreen() {
        super(Component.literal("HOTM Config - Tokens: "
            + HotmManager.getTree().getUsedTokens() + "/" + HotmManager.getTree().getTotalTokens()));
    }

    /** Opens the HOTM config screen (same entry point as 1.21). */
    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.schedule(() -> client.setScreen(new HotmChestScreen()));
    }

    /**
     * 1.21 mixin hook, kept for API parity. On 26.1.2 the screen handles its
     * own clicks, so this always reports the click as unhandled.
     */
    public static boolean handleClick(int slotIndex, int button) {
        return false;
    }

    public static boolean isHotmScreen(String title) {
        return title != null && title.startsWith("HOTM Config");
    }

    /** Kept for API parity; the 26.1.2 screen shows all rows at once. */
    public static int getCurrentPage() {
        return 0;
    }

    @Override
    protected void init() {
        super.init();
        gridX = (this.width - GRID_W) / 2;
        gridY = Math.max(24, (this.height - GRID_H) / 2);

        buttons.clear();
        int bx = gridX + GRID_W + 12;
        int by = gridY + 4;
        for (String id : new String[]{"presets", "save", "apply", "delay", "close"}) {
            Button b = new Button(id);
            b.x = bx;
            b.y = by;
            b.w = BTN_W;
            b.h = BTN_H;
            buttons.add(b);
            by += BTN_H + 5;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(ctx, mouseX, mouseY, deltaTicks);

        HotmTree tree = HotmManager.getTree();

        // Panel behind the grid
        ctx.fill(gridX - 6, gridY - 20, gridX + GRID_W + 6, gridY + GRID_H + 6, 0xC0101018);
        ctx.outline(gridX - 6, gridY - 20, GRID_W + 12, GRID_H + 26, 0xFF404050);

        // Header
        ctx.centeredText(font, "\u00A76HOTM Config \u00A77- \u00A7eTokens: "
                + tree.getUsedTokens() + "/" + tree.getTotalTokens(),
            gridX + GRID_W / 2, gridY - 14, 0xFFFFFFFF);

        HotmNode hovered = nodeAt(mouseX, mouseY);

        // Tree grid (row 0 = HOTM 10 at top, row 9 = HOTM 1 at bottom, as on 1.21)
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = gridX + col * CELL;
                int y = gridY + row * CELL;
                HotmNode node = tree.getNodeAt(row, col);
                if (node == null) {
                    ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x30000000);
                    continue;
                }
                HotmNode.State state = tree.getState(node);
                ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x60000000);
                ctx.outline(x, y, CELL, CELL, node == hovered ? 0xFFFFFFFF : stateColor(node, state));
                ctx.item(new ItemStack(node.getItemForState(state)), x + 1, y + 1);
            }
        }

        // Hovered node info panel (replaces the 1.21 item custom-name tooltips)
        int infoX = Math.max(6, gridX - 6 - 150);
        int infoY = gridY + 4;
        if (hovered != null) {
            HotmNode.State state = tree.getState(hovered);
            ctx.fill(infoX - 4, infoY - 4, infoX + 144, infoY + 46, 0xC0101018);
            ctx.outline(infoX - 4, infoY - 4, 148, 50, 0xFF404050);
            String typeStr = hovered.getType() == HotmNode.Type.ABILITY ? "\u00A75[Ability] " : "";
            ctx.text(font, typeStr + "\u00A7f" + hovered.getDisplayName(), infoX, infoY, 0xFFFFFFFF);
            ctx.text(font, stateLabel(state), infoX, infoY + 12, 0xFFFFFFFF);
            if (hovered.isAlwaysEnabled()) {
                ctx.text(font, "\u00A76Always enabled", infoX, infoY + 24, 0xFFFFFFFF);
            } else {
                ctx.text(font, "\u00A77Click: cycle state", infoX, infoY + 24, 0xFFFFFFFF);
            }
        }

        // Side buttons
        for (Button b : buttons) {
            boolean hover = b.contains(mouseX, mouseY);
            ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, hover ? 0xE0303040 : 0xC0202028);
            ctx.outline(b.x, b.y, b.w, b.h, hover ? 0xFFFFFFFF : 0xFF505060);
            ctx.centeredText(font, buttonLabel(b.id), b.x + b.w / 2, b.y + (b.h - 8) / 2, 0xFFFFFFFF);
        }

        // Save-name modal overlay
        if (saveMode) {
            ctx.fill(0, 0, this.width, this.height, 0xB0000000);
            int boxW = 220, boxH = 58;
            int boxX = (this.width - boxW) / 2;
            int boxY = (this.height - boxH) / 2;
            ctx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0181820);
            ctx.outline(boxX, boxY, boxW, boxH, 0xFF606070);
            ctx.centeredText(font, "\u00A7aSave Preset", boxX + boxW / 2, boxY + 6, 0xFFFFFFFF);
            ctx.text(font, "\u00A77Name: \u00A7f" + saveName + "\u00A7e_", boxX + 8, boxY + 22, 0xFFFFFFFF);
            ctx.text(font, "\u00A78Enter = save, Esc = cancel", boxX + 8, boxY + 40, 0xFFFFFFFF);
        }
    }

    private String buttonLabel(String id) {
        return switch (id) {
            case "presets" -> "\u00A7dPresets";
            case "save" -> "\u00A7aSave Preset";
            case "apply" -> "\u00A7bApply (Auto-HOTM)";
            case "delay" -> "\u00A7eDelay: \u00A7f" + AutoHotmManager.getTickDelay() + "t \u00A77(L+ R-)";
            case "close" -> "\u00A7cClose";
            default -> id;
        };
    }

    private static int stateColor(HotmNode node, HotmNode.State state) {
        if (node.isAlwaysEnabled()) return 0xFFFFAA00;
        return switch (state) {
            case NOT_CLICKED -> 0xFF404040;
            case DISABLED -> 0xFFCC3333;
            case LEVEL_1 -> 0xFF33CC33;
            case MAXED -> 0xFF33CCCC;
            case CHOSEN -> 0xFFCC33CC;
        };
    }

    private static String stateLabel(HotmNode.State state) {
        return switch (state) {
            case NOT_CLICKED -> "\u00A78[OFF]";
            case DISABLED -> "\u00A7c[DISABLED]";
            case LEVEL_1 -> "\u00A7a[LEVEL 1]";
            case MAXED -> "\u00A7b[MAXED]";
            case CHOSEN -> "\u00A7d[CHOSEN]";
        };
    }

    private HotmNode nodeAt(double mx, double my) {
        int col = (int) Math.floor((mx - gridX) / CELL);
        int row = (int) Math.floor((my - gridY) / CELL);
        if (col < 0 || col >= GRID_COLS || row < 0 || row >= GRID_ROWS) return null;
        return HotmManager.getTree().getNodeAt(row, col);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (saveMode) return true; // modal captures clicks

        double mx = click.x();
        double my = click.y();
        int button = click.button();

        for (Button b : buttons) {
            if (!b.contains(mx, my)) continue;
            switch (b.id) {
                case "presets" -> HotmPresetScreen.open();
                case "save" -> {
                    saveMode = true;
                    saveName.setLength(0);
                }
                case "apply" -> {
                    Minecraft client = Minecraft.getInstance();
                    client.schedule(() -> {
                        client.setScreen(null);
                        HotmManager.apply();
                    });
                }
                case "delay" -> {
                    int current = AutoHotmManager.getTickDelay();
                    AutoHotmManager.setTickDelay(button == 1 ? current - 1 : current + 1);
                }
                case "close" -> onClose();
            }
            return true;
        }

        HotmNode node = nodeAt(mx, my);
        if (node != null) {
            HotmTree tree = HotmManager.getTree();
            HotmNode.State current = tree.getState(node);
            tree.setState(node, getNextState(node, current));
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private static HotmNode.State getNextState(HotmNode node, HotmNode.State current) {
        if (node.getType() == HotmNode.Type.PERK) {
            return switch (current) {
                case NOT_CLICKED -> HotmNode.State.LEVEL_1;
                case LEVEL_1 -> HotmNode.State.MAXED;
                case MAXED -> HotmNode.State.DISABLED;
                case DISABLED -> HotmNode.State.NOT_CLICKED;
                default -> HotmNode.State.NOT_CLICKED;
            };
        } else {
            return switch (current) {
                case NOT_CLICKED -> HotmNode.State.CHOSEN;
                case CHOSEN -> HotmNode.State.DISABLED;
                case DISABLED -> HotmNode.State.NOT_CLICKED;
                default -> HotmNode.State.NOT_CLICKED;
            };
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (saveMode) {
            int key = input.key();
            if (key == KEY_ESCAPE) {
                saveMode = false;
                return true;
            }
            if (key == KEY_ENTER || key == KEY_KP_ENTER) {
                String name = saveName.toString().trim();
                if (!name.isEmpty()) {
                    HotmManager.savePreset(name);
                }
                saveMode = false;
                return true;
            }
            if (key == KEY_BACKSPACE) {
                if (saveName.length() > 0) saveName.setLength(saveName.length() - 1);
                return true;
            }
            return true; // modal captures all keys
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (saveMode) {
            if (input.isAllowedChatCharacter() && saveName.length() < 32) {
                saveName.append(input.codepointAsString());
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
