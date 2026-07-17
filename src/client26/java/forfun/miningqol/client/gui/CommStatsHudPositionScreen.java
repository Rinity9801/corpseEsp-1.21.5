package forfun.miningqol.client.gui;

import forfun.miningqol.client.CommStatsHUD;
import forfun.miningqol.client.MiningqolClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-to-move / scroll-to-resize editor for the commission stats HUD.
 * The panel itself is drawn by CommStatsHUD.renderNvg (editor mode).
 * Mirrors {@link CommissionHudPositionScreen}.
 */
public class CommStatsHudPositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private double grabDx = 0;
    private double grabDy = 0;

    public CommStatsHudPositionScreen(Screen parent) {
        super(Component.literal("Move Commission Stats HUD"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(ctx, mouseX, mouseY, deltaTicks);

        ctx.fill(0, 8, ctx.guiWidth(), 26, 0x90101018);
        ctx.centeredText(font, Component.literal("Drag (snaps to grid, Shift = free) — scroll to resize — Esc or Done saves"),
            ctx.guiWidth() / 2, 14, 0xFFE9ECF6);

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

        int bw = 84, bh = 20;
        int bx = width / 2 - bw / 2;
        int by = height - 34;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
            onClose();
            return true;
        }

        int x = CommStatsHUD.getX();
        int y = CommStatsHUD.getY();
        int w = Math.max(60, CommStatsHUD.getWidth());
        int h = Math.max(14, CommStatsHUD.getHeight());
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
            ny = Math.max(0, Math.min(height - 14, ny));
            CommStatsHUD.setPosition(nx, ny);
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
        CommStatsHUD.setScale(CommStatsHUD.getScale() + (float) scrollY * 0.05f);
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
