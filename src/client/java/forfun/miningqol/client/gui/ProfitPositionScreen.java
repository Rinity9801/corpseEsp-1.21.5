package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
        // Call super.render first which handles background
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
            "§eDrag the profit tracker to reposition it",
            this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
            "§7Press ESC when done",
            this.width / 2, 35, 0xFFFFFF);

        if (dragging) {
            ProfitTrackerHUD.setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }

        ProfitTrackerHUD.render(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { 
            int hudX = ProfitTrackerHUD.getX();
            int hudY = ProfitTrackerHUD.getY();
            int hudWidth = 120;
            int hudHeight = 30;

            if (mouseX >= hudX && mouseX <= hudX + hudWidth &&
                mouseY >= hudY && mouseY <= hudY + hudHeight) {
                dragging = true;
                dragOffsetX = (int)mouseX - hudX;
                dragOffsetY = (int)mouseY - hudY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
