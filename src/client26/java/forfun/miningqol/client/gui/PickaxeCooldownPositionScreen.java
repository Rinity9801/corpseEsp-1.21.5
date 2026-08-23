package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.PickaxeCooldownHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-to-move editor for the pickaxe cooldown HUD (26.1.2). Draws a stand-in
 * cooldown line at the configured position so it works anywhere, not just while a
 * real cooldown is active. Mirrors {@link CommissionHudPositionScreen}.
 */
public class PickaxeCooldownPositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private double grabDx = 0;
    private double grabDy = 0;

    public PickaxeCooldownPositionScreen(Screen parent) {
        super(Component.literal("Move Pickaxe Cooldown HUD"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(ctx, mouseX, mouseY, deltaTicks);

        int x = PickaxeCooldownHUD.getX();
        int y = PickaxeCooldownHUD.getY();
        int w = Math.max(60, PickaxeCooldownHUD.getWidth());
        int h = Math.max(12, PickaxeCooldownHUD.getHeight());

        // Header + instructions
        ctx.fill(0, 8, ctx.guiWidth(), 26, 0x90101018);
        ctx.centeredText(font, Component.literal("Drag (snaps to grid, Shift = free) — scroll to resize — Esc or Done saves"),
            ctx.guiWidth() / 2, 14, 0xFFE9ECF6);

        // HUD preview + outline
        ctx.outline(x - 2, y - 2, w + 4, h + 4, 0xFF7FA8DB);
        ctx.text(font, PickaxeCooldownHUD.getPreviewText(), x, y, 0xFFFFFFFF, true);

        // Done button
        int bw = 84, bh = 20;
        int bx = ctx.guiWidth() / 2 - bw / 2;
        int by = ctx.guiHeight() - 34;
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        ctx.fill(bx, by, bx + bw, by + bh, hover ? 0xF02E5C34 : 0xF0203A24);
        ctx.outline(bx, by, bw, bh, hover ? 0xFF7FDB8A : 0xFF3E7A47);
        ctx.centeredText(font, Component.literal("Done"), ctx.guiWidth() / 2, by + 6, 0xFFDCF5DF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        // Done button
        int bw = 84, bh = 20;
        int bx = width / 2 - bw / 2;
        int by = height - 34;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
            onClose();
            return true;
        }

        int x = PickaxeCooldownHUD.getX();
        int y = PickaxeCooldownHUD.getY();
        int w = Math.max(60, PickaxeCooldownHUD.getWidth());
        int h = Math.max(12, PickaxeCooldownHUD.getHeight());
        if (mx >= x - 2 && mx <= x + w + 2 && my >= y - 2 && my <= y + h + 2) {
            dragging = true;
            grabDx = click.x() - x;
            grabDy = click.y() - y;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging) {
            int nx = (int) Math.round(click.x() - grabDx);
            int ny = (int) Math.round(click.y() - grabDy);
            nx = HudDragSnap.snap(nx);
            ny = HudDragSnap.snap(ny);
            nx = Math.max(0, Math.min(width - 20, nx));
            ny = Math.max(0, Math.min(height - 12, ny));
            PickaxeCooldownHUD.setPosition(nx, ny);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        PickaxeCooldownHUD.setScale(PickaxeCooldownHUD.getScale() + (float) scrollY * 0.05f);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (MiningqolClient.getConfig() != null) {
            MiningqolClient.getConfig().loadFromGame();
            MiningqolClient.getConfig().save();
        }
        Minecraft.getInstance().setScreen(parent);
    }
}
