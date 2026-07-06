package forfun.miningqol.client.gui;

import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.MiningqolClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class CommissionHudPositionScreen extends Screen {
    private static final int SNAP_DISTANCE = 8;
    private static final int SNAPLINE_COLOR = 0xA060A5FA;
    private static final int EDGE_SNAPLINE_COLOR = 0x90FFFFFF;

    private final Screen parent;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public CommissionHudPositionScreen(Screen parent) {
        super(Text.literal("Position Commission HUD"));
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
            "§7Scale: " + String.format("%.1fx", CommissionHUD.getScale()) + " §8| §7Press ESC when done",
            this.width / 2, 35, 0xFFFFFF);

        if (dragging) {
            updateDraggedPosition(mouseX, mouseY);
        }

        CommissionHUD.renderPreview(context);

        int x = CommissionHUD.getX();
        int y = CommissionHUD.getY();
        int hudWidth = CommissionHUD.getWidth();
        int hudHeight = CommissionHUD.getHeight();
        int borderColor = 0x80FFFFFF;

        drawSnaplines(context, x, y, hudWidth, hudHeight);

        context.drawHorizontalLine(x - 2, x + hudWidth + 1, y - 2, borderColor);
        context.drawHorizontalLine(x - 2, x + hudWidth + 1, y + hudHeight + 1, borderColor);
        context.drawVerticalLine(x - 2, y - 2, y + hudHeight + 1, borderColor);
        context.drawVerticalLine(x + hudWidth + 1, y - 2, y + hudHeight + 1, borderColor);
    }

    @Override
    public boolean mouseClicked(Click click, boolean firstClick) {
        if (click.button() == 0) {
            int hudX = CommissionHUD.getX();
            int hudY = CommissionHUD.getY();
            int hudWidth = CommissionHUD.getWidth();
            int hudHeight = CommissionHUD.getHeight();

            double mouseX = click.x();
            double mouseY = click.y();
            if (mouseX >= hudX - 5 && mouseX <= hudX + hudWidth + 5 &&
                mouseY >= hudY - 5 && mouseY <= hudY + hudHeight + 5) {
                dragging = true;
                dragOffsetX = (int) mouseX - hudX;
                dragOffsetY = (int) mouseY - hudY;
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
        CommissionHUD.setScale(CommissionHUD.getScale() + (float) (verticalAmount * 0.1));
        return true;
    }

    private void updateDraggedPosition(int mouseX, int mouseY) {
        int hudWidth = CommissionHUD.getWidth();
        int hudHeight = CommissionHUD.getHeight();

        int targetX = mouseX - dragOffsetX;
        int targetY = mouseY - dragOffsetY;

        int centerX = (this.width - hudWidth) / 2;
        int centerY = (this.height - hudHeight) / 2;
        int rightX = this.width - hudWidth;
        int bottomY = this.height - hudHeight;

        if (Math.abs(targetX - 0) <= SNAP_DISTANCE) {
            targetX = 0;
        } else if (Math.abs(targetX - centerX) <= SNAP_DISTANCE) {
            targetX = centerX;
        } else if (Math.abs(targetX - rightX) <= SNAP_DISTANCE) {
            targetX = rightX;
        }

        if (Math.abs(targetY - 0) <= SNAP_DISTANCE) {
            targetY = 0;
        } else if (Math.abs(targetY - centerY) <= SNAP_DISTANCE) {
            targetY = centerY;
        } else if (Math.abs(targetY - bottomY) <= SNAP_DISTANCE) {
            targetY = bottomY;
        }

        targetX = Math.max(0, Math.min(targetX, rightX));
        targetY = Math.max(0, Math.min(targetY, bottomY));
        CommissionHUD.setPosition(targetX, targetY);
    }

    private void drawSnaplines(DrawContext context, int x, int y, int hudWidth, int hudHeight) {
        int centerX = (this.width - hudWidth) / 2;
        int centerY = (this.height - hudHeight) / 2;
        int rightX = this.width - hudWidth;
        int bottomY = this.height - hudHeight;

        if (Math.abs(x - 0) <= SNAP_DISTANCE) {
            context.fill(0, 0, 1, this.height, EDGE_SNAPLINE_COLOR);
        } else if (Math.abs(x - centerX) <= SNAP_DISTANCE) {
            int lineX = this.width / 2;
            context.fill(lineX, 0, lineX + 1, this.height, SNAPLINE_COLOR);
        } else if (Math.abs(x - rightX) <= SNAP_DISTANCE) {
            context.fill(this.width - 1, 0, this.width, this.height, EDGE_SNAPLINE_COLOR);
        }

        if (Math.abs(y - 0) <= SNAP_DISTANCE) {
            context.fill(0, 0, this.width, 1, EDGE_SNAPLINE_COLOR);
        } else if (Math.abs(y - centerY) <= SNAP_DISTANCE) {
            int lineY = this.height / 2;
            context.fill(0, lineY, this.width, lineY + 1, SNAPLINE_COLOR);
        } else if (Math.abs(y - bottomY) <= SNAP_DISTANCE) {
            context.fill(0, this.height - 1, this.width, this.height, EDGE_SNAPLINE_COLOR);
        }
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
