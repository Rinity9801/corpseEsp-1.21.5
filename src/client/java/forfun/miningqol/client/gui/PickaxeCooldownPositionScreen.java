package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.PickaxeCooldownHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

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
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
            "§eDrag to reposition, scroll to resize",
            this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
            "§7Scale: " + String.format("%.1fx", PickaxeCooldownHUD.getScale()) + " §8| §7Press ESC when done",
            this.width / 2, 35, 0xFFFFFF);

        if (dragging) {
            PickaxeCooldownHUD.setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }

        int x = PickaxeCooldownHUD.getX();
        int y = PickaxeCooldownHUD.getY();

        String cooldownText = "§6Pickobulus: §c30s";
        context.drawTextWithShadow(this.textRenderer, cooldownText, x, y, 0xFFFFFFFF);

        // Draw border around HUD
        int hudWidth = PickaxeCooldownHUD.getWidth();
        int hudHeight = PickaxeCooldownHUD.getHeight();
        int borderColor = 0x80FFFFFF;
        context.drawHorizontalLine(x - 2, x + hudWidth + 1, y - 2, borderColor);
        context.drawHorizontalLine(x - 2, x + hudWidth + 1, y + hudHeight + 1, borderColor);
        context.drawVerticalLine(x - 2, y - 2, y + hudHeight + 1, borderColor);
        context.drawVerticalLine(x + hudWidth + 1, y - 2, y + hudHeight + 1, borderColor);
    }

    @Override
    public boolean mouseClicked(Click click, boolean firstClick) {
        if (click.button() == 0) {
            int hudX = PickaxeCooldownHUD.getX();
            int hudY = PickaxeCooldownHUD.getY();
            int hudWidth = PickaxeCooldownHUD.getWidth();
            int hudHeight = PickaxeCooldownHUD.getHeight();

            double mouseX = click.x();
            double mouseY = click.y();

            if (mouseX >= hudX - 5 && mouseX <= hudX + hudWidth + 5 &&
                mouseY >= hudY - 5 && mouseY <= hudY + hudHeight + 5) {
                dragging = true;
                dragOffsetX = (int)mouseX - hudX;
                dragOffsetY = (int)mouseY - hudY;
                return true;
            }
        }
        return super.mouseClicked(click, firstClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            dragging = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float currentScale = PickaxeCooldownHUD.getScale();
        float newScale = currentScale + (float)(verticalAmount * 0.1);
        PickaxeCooldownHUD.setScale(newScale);
        return true;
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
