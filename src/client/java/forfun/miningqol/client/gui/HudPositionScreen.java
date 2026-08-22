package forfun.miningqol.client.gui;

import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.RollingMinerCooldown;
import forfun.miningqol.client.collection.CollectionTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Moves every currently enabled, positionable HUD from one screen. */
public class HudPositionScreen extends Screen {
    private static final int GUIDE_DISTANCE = 5;
    private static final int HUD_GAP = 4;
    private static final String PICKAXE_PREVIEW = "\u00A76Pickobulus: \u00A7c30s";
    private static final String ROLLING_PREVIEW = "\u00A7aRolling Miner: \u00A72\u2714 Ready";

    private enum Target {
        PROFIT,
        COLLECTION,
        PICKAXE,
        ROLLING,
        COMMISSIONS
    }

    private final Screen parent;
    private Target dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudPositionScreen(Screen parent) {
        super(Text.literal("Move HUDs"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (dragging != null) {
            moveTarget(dragging, mouseX - dragOffsetX, mouseY - dragOffsetY);
        }

        if (isVisible(Target.PROFIT)) {
            ProfitTrackerHUD.renderPreview(context);
            drawBorder(context, Target.PROFIT, 0xFF64D879);
        }
        if (isVisible(Target.COLLECTION)) {
            CollectionTracker.renderPreview(context);
            drawBorder(context, Target.COLLECTION, 0xFF55C8F0);
        }
        if (isVisible(Target.PICKAXE)) {
            context.drawTextWithShadow(textRenderer, PICKAXE_PREVIEW,
                PickaxeCooldownHUD.getX(), PickaxeCooldownHUD.getY(), 0xFFFFFFFF);
            drawBorder(context, Target.PICKAXE, 0xFF7FA8DB);
        }
        if (isVisible(Target.ROLLING)) {
            context.drawTextWithShadow(textRenderer, ROLLING_PREVIEW,
                RollingMinerCooldown.getX(), RollingMinerCooldown.getY(), 0xFFFFFFFF);
            drawBorder(context, Target.ROLLING, 0xFF7FDB8A);
        }
        if (isVisible(Target.COMMISSIONS)) {
            CommissionHUD.renderPreview(context);
            drawBorder(context, Target.COMMISSIONS, 0xFF88AAFF);
        }

        drawGuides(context);
        context.fill(0, 8, width, 39, 0xA0101018);
        context.drawCenteredTextWithShadow(textRenderer,
            "Drag enabled HUDs to move them; scroll over one to resize", width / 2, 13, 0xFFE9ECF6);
        context.drawCenteredTextWithShadow(textRenderer,
            "Snaps to screen and nearby HUD edges (hold Shift for free movement)", width / 2, 26, 0xFF9EA4B3);

        if (!hasVisibleHud()) {
            context.drawCenteredTextWithShadow(textRenderer, "No movable HUDs are enabled.",
                width / 2, height / 2, 0xFFB5BAC8);
        }
        drawDoneButton(context, mouseX, mouseY);
    }

    private void drawBorder(DrawContext context, Target target, int color) {
        int x = targetX(target) - 2;
        int y = targetY(target) - 2;
        int right = x + targetWidth(target) + 3;
        int bottom = y + targetHeight(target) + 3;
        context.drawHorizontalLine(x, right, y, color);
        context.drawHorizontalLine(x, right, bottom, color);
        context.drawVerticalLine(x, y, bottom, color);
        context.drawVerticalLine(right, y, bottom, color);
    }

    private void drawDoneButton(DrawContext context, int mouseX, int mouseY) {
        int x = width / 2 - 42;
        int y = height - 34;
        boolean hovered = contains(mouseX, mouseY, x, y, 84, 20);
        context.fill(x, y, x + 84, y + 20, hovered ? 0xF02E5C34 : 0xF0203A24);
        context.drawHorizontalLine(x, x + 84, y, hovered ? 0xFF7FDB8A : 0xFF3E7A47);
        context.drawHorizontalLine(x, x + 84, y + 20, hovered ? 0xFF7FDB8A : 0xFF3E7A47);
        context.drawVerticalLine(x, y, y + 20, hovered ? 0xFF7FDB8A : 0xFF3E7A47);
        context.drawVerticalLine(x + 84, y, y + 20, hovered ? 0xFF7FDB8A : 0xFF3E7A47);
        context.drawCenteredTextWithShadow(textRenderer, "Done", width / 2, y + 6, 0xFFDCF5DF);
    }

    private void drawGuides(DrawContext context) {
        if (dragging == null || shiftHeld()) return;

        int x = targetX(dragging);
        int y = targetY(dragging);
        int targetWidth = targetWidth(dragging);
        int targetHeight = targetHeight(dragging);
        int centeredX = (width - targetWidth) / 2;
        int centeredY = (height - targetHeight) / 2;

        if (x == centeredX) {
            context.fill(width / 2, 0, width / 2 + 1, height, 0x9060A5FA);
        } else if (x == 0 || x == width - targetWidth) {
            int lineX = x == 0 ? 0 : width - 1;
            context.fill(lineX, 0, lineX + 1, height, 0x70FFFFFF);
        }
        if (y == centeredY) {
            context.fill(0, height / 2, width, height / 2 + 1, 0x9060A5FA);
        } else if (y == 0 || y == height - targetHeight) {
            int lineY = y == 0 ? 0 : height - 1;
            context.fill(0, lineY, width, lineY + 1, 0x70FFFFFF);
        }

        for (Target other : Target.values()) {
            if (other == dragging || !isVisible(other)) continue;
            int otherX = targetX(other);
            int otherY = targetY(other);
            int otherWidth = targetWidth(other);
            int otherHeight = targetHeight(other);

            if (x == otherX || x + targetWidth == otherX + otherWidth) {
                int lineX = x == otherX ? x : x + targetWidth;
                context.fill(lineX, 0, lineX + 1, height, 0x9060A5FA);
            }
            if (x + targetWidth == otherX || x == otherX + otherWidth) {
                int lineX = x < otherX ? otherX : x;
                context.fill(lineX, 0, lineX + 1, height, 0x907FDB8A);
            }
            if (x + targetWidth + HUD_GAP == otherX || x == otherX + otherWidth + HUD_GAP) {
                int lineX = x < otherX ? x + targetWidth + HUD_GAP / 2 : x - HUD_GAP / 2;
                context.fill(lineX, 0, lineX + 1, height, 0x907FDB8A);
            }
            if (y == otherY || y + targetHeight == otherY + otherHeight) {
                int lineY = y == otherY ? y : y + targetHeight;
                context.fill(0, lineY, width, lineY + 1, 0x9060A5FA);
            }
            if (y + targetHeight == otherY || y == otherY + otherHeight) {
                int lineY = y < otherY ? otherY : y;
                context.fill(0, lineY, width, lineY + 1, 0x907FDB8A);
            }
            if (y + targetHeight + HUD_GAP == otherY || y == otherY + otherHeight + HUD_GAP) {
                int lineY = y < otherY ? y + targetHeight + HUD_GAP / 2 : y - HUD_GAP / 2;
                context.fill(0, lineY, width, lineY + 1, 0x907FDB8A);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean firstClick) {
        if (click.button() == 0 && contains(click.x(), click.y(), width / 2 - 42, height - 34, 84, 20)) {
            close();
            return true;
        }
        if (click.button() == 0) {
            Target target = targetAt(click.x(), click.y());
            if (target != null) {
                dragging = target;
                dragOffsetX = (int) click.x() - targetX(target);
                dragOffsetY = (int) click.y() - targetY(target);
                return true;
            }
        }
        return super.mouseClicked(click, firstClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Target target = targetAt(mouseX, mouseY);
        if (target == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        float amount = (float) verticalAmount * 0.1f;
        switch (target) {
            case PROFIT -> ProfitTrackerHUD.setScale(ProfitTrackerHUD.getScale() + amount);
            case COLLECTION -> CollectionTracker.setScale(CollectionTracker.getScale() + amount);
            case PICKAXE -> PickaxeCooldownHUD.setScale(PickaxeCooldownHUD.getScale() + amount);
            case ROLLING -> { }
            case COMMISSIONS -> CommissionHUD.setScale(CommissionHUD.getScale() + amount);
        }
        return true;
    }

    @Override
    public void close() {
        if (MiningqolClient.getConfig() != null) {
            MiningqolClient.getConfig().loadFromGame();
            MiningqolClient.getConfig().save();
        }
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void moveTarget(Target target, int requestedX, int requestedY) {
        int x = snapX(target, requestedX);
        int y = snapY(target, requestedY);
        switch (target) {
            case PROFIT -> ProfitTrackerHUD.setPosition(x, y);
            case COLLECTION -> CollectionTracker.setPosition(x, y);
            case PICKAXE -> PickaxeCooldownHUD.setPosition(x, y);
            case ROLLING -> RollingMinerCooldown.setPosition(x, y);
            case COMMISSIONS -> CommissionHUD.setPosition(x, y);
        }
    }

    private int snapX(Target target, int value) {
        int targetWidth = targetWidth(target);
        int maximum = Math.max(0, width - targetWidth);
        int clamped = Math.max(0, Math.min(value, maximum));
        if (shiftHeld()) return clamped;

        java.util.List<Integer> candidates = new java.util.ArrayList<>();
        candidates.add(0);
        candidates.add(maximum / 2);
        candidates.add(maximum);
        for (Target other : Target.values()) {
            if (other == target || !isVisible(other)) continue;
            int otherX = targetX(other);
            int otherWidth = targetWidth(other);
            candidates.add(otherX);
            candidates.add(otherX + otherWidth - targetWidth);
            candidates.add(otherX + (otherWidth - targetWidth) / 2);
            candidates.add(otherX - targetWidth);
            candidates.add(otherX + otherWidth);
            candidates.add(otherX - targetWidth - HUD_GAP);
            candidates.add(otherX + otherWidth + HUD_GAP);
        }
        return nearest(clamped, maximum, candidates);
    }

    private int snapY(Target target, int value) {
        int targetHeight = targetHeight(target);
        int maximum = Math.max(0, height - targetHeight);
        int clamped = Math.max(0, Math.min(value, maximum));
        if (shiftHeld()) return clamped;

        java.util.List<Integer> candidates = new java.util.ArrayList<>();
        candidates.add(0);
        candidates.add(maximum / 2);
        candidates.add(maximum);
        for (Target other : Target.values()) {
            if (other == target || !isVisible(other)) continue;
            int otherY = targetY(other);
            int otherHeight = targetHeight(other);
            candidates.add(otherY);
            candidates.add(otherY + otherHeight - targetHeight);
            candidates.add(otherY + (otherHeight - targetHeight) / 2);
            candidates.add(otherY - targetHeight);
            candidates.add(otherY + otherHeight);
            candidates.add(otherY - targetHeight - HUD_GAP);
            candidates.add(otherY + otherHeight + HUD_GAP);
        }
        return nearest(clamped, maximum, candidates);
    }

    private int nearest(int value, int maximum, java.util.List<Integer> candidates) {
        int best = value;
        int bestDistance = GUIDE_DISTANCE + 1;
        for (int candidate : candidates) {
            if (candidate < 0 || candidate > maximum) continue;
            int distance = Math.abs(value - candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean hasVisibleHud() {
        return isVisible(Target.PROFIT) || isVisible(Target.COLLECTION)
            || isVisible(Target.PICKAXE) || isVisible(Target.ROLLING) || isVisible(Target.COMMISSIONS);
    }

    private boolean isVisible(Target target) {
        return switch (target) {
            case PROFIT -> ProfitTrackerHUD.isEnabled();
            case COLLECTION -> CollectionTracker.isTracking();
            case PICKAXE -> PickaxeCooldownHUD.isEnabled();
            case ROLLING -> RollingMinerCooldown.isEnabled();
            case COMMISSIONS -> CommissionHUD.isEnabled();
        };
    }

    private Target targetAt(double mouseX, double mouseY) {
        Target[] hitOrder = {Target.COMMISSIONS, Target.ROLLING, Target.PICKAXE, Target.COLLECTION, Target.PROFIT};
        for (Target target : hitOrder) {
            if (isVisible(target) && contains(mouseX, mouseY, targetX(target), targetY(target),
                targetWidth(target), targetHeight(target))) {
                return target;
            }
        }
        return null;
    }

    private int targetX(Target target) {
        return switch (target) {
            case PROFIT -> ProfitTrackerHUD.getX();
            case COLLECTION -> CollectionTracker.getX();
            case PICKAXE -> PickaxeCooldownHUD.getX();
            case ROLLING -> RollingMinerCooldown.getX();
            case COMMISSIONS -> CommissionHUD.getX();
        };
    }

    private int targetY(Target target) {
        return switch (target) {
            case PROFIT -> ProfitTrackerHUD.getY();
            case COLLECTION -> CollectionTracker.getY();
            case PICKAXE -> PickaxeCooldownHUD.getY();
            case ROLLING -> RollingMinerCooldown.getY();
            case COMMISSIONS -> CommissionHUD.getY();
        };
    }

    private int targetWidth(Target target) {
        return switch (target) {
            case PROFIT -> Math.max(80, ProfitTrackerHUD.getWidth());
            case COLLECTION -> Math.max(80, CollectionTracker.getWidth());
            case PICKAXE -> Math.max(120, PickaxeCooldownHUD.getWidth());
            case ROLLING -> RollingMinerCooldown.getWidth();
            case COMMISSIONS -> Math.max(120, CommissionHUD.getWidth());
        };
    }

    private int targetHeight(Target target) {
        return switch (target) {
            case PROFIT -> Math.max(20, ProfitTrackerHUD.getHeight());
            case COLLECTION -> Math.max(20, CollectionTracker.getHeight());
            case PICKAXE -> 12;
            case ROLLING -> RollingMinerCooldown.getHeight();
            case COMMISSIONS -> Math.max(40, CommissionHUD.getHeight());
        };
    }

    private boolean shiftHeld() {
        if (client == null) return false;
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
