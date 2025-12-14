package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.PickaxeCooldownHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PickaxeCooldownPositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public PickaxeCooldownPositionScreen(Screen parent) {
        super(Text.literal("Position Pickaxe Cooldown"));
        this.parent = parent;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
            "§eDrag the pickaxe cooldown display to reposition it",
            this.width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
            "§7Press ESC when done",
            this.width / 2, 35, 0xFFFFFFFF);

        // Check mouse state via GLFW
        long windowHandle = this.client.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        int hudX = PickaxeCooldownHUD.getX();
        int hudY = PickaxeCooldownHUD.getY();
        int hudWidth = 100;
        int hudHeight = 20;

        if (mouseDown) {
            if (!dragging) {
                // Check if clicking on HUD
                if (mouseX >= hudX - 2 && mouseX <= hudX + hudWidth &&
                    mouseY >= hudY - 2 && mouseY <= hudY + hudHeight) {
                    dragging = true;
                    dragOffsetX = mouseX - hudX;
                    dragOffsetY = mouseY - hudY;
                }
            }
            if (dragging) {
                PickaxeCooldownHUD.setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
            }
        } else {
            dragging = false;
        }

        String cooldownText = "§6Pickobulus: §c30s";
        int x = PickaxeCooldownHUD.getX();
        int y = PickaxeCooldownHUD.getY();

        context.drawTextWithShadow(this.textRenderer, cooldownText, x, y, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (MiningqolClient.getConfig() != null) {
            MiningqolClient.getConfig().loadFromGame();
            MiningqolClient.getConfig().save();
        }

        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
