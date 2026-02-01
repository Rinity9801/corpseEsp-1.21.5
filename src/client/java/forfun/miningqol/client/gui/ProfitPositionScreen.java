package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

public class ProfitPositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public ProfitPositionScreen(Screen parent) {
        super(Text.literal("Position Profit Tracker"));
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
            "§7Scale: " + String.format("%.1fx", ProfitTrackerHUD.getScale()) + " §8| §7Press ESC when done",
            this.width / 2, 35, 0xFFFFFF);

        if (dragging) {
            ProfitTrackerHUD.setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }

        // Render preview (always shows sample data for positioning)
        ProfitTrackerHUD.renderPreview(context);

        // Draw resize indicator border around HUD
        int hudX = ProfitTrackerHUD.getX();
        int hudY = ProfitTrackerHUD.getY();
        int hudWidth = ProfitTrackerHUD.getWidth();
        int hudHeight = ProfitTrackerHUD.getHeight();

        // Draw dashed border
        int borderColor = 0x80FFFFFF;
        context.drawHorizontalLine(hudX - 2, hudX + hudWidth + 1, hudY - 2, borderColor);
        context.drawHorizontalLine(hudX - 2, hudX + hudWidth + 1, hudY + hudHeight + 1, borderColor);
        context.drawVerticalLine(hudX - 2, hudY - 2, hudY + hudHeight + 1, borderColor);
        context.drawVerticalLine(hudX + hudWidth + 1, hudY - 2, hudY + hudHeight + 1, borderColor);
    }

    @Override
    public boolean mouseClicked(Click click, boolean firstClick) {
        if (click.button() == 0) {
            int hudX = ProfitTrackerHUD.getX();
            int hudY = ProfitTrackerHUD.getY();
            int hudWidth = ProfitTrackerHUD.getWidth();
            int hudHeight = ProfitTrackerHUD.getHeight();

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
        float currentScale = ProfitTrackerHUD.getScale();
        float newScale = currentScale + (float)(verticalAmount * 0.1);
        ProfitTrackerHUD.setScale(newScale);
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
