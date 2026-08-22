package forfun.miningqol.client.gui;

import forfun.miningqol.client.CommStatsHUD;
import forfun.miningqol.client.CommTracker;
import forfun.miningqol.client.CommissionHUD;
import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.PickaxeCooldownHUD;
import forfun.miningqol.client.RollingMinerCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Moves every currently enabled, positionable HUD from one screen. */
public class HudPositionScreen extends Screen {
    private static final int GUIDE_DISTANCE = 5;
    private static final int HUD_GAP = 4;
    private static final String PICKAXE_PREVIEW = "\u00A76Pickobulus: \u00A7c30s";
    private static final String ROLLING_PREVIEW = "\u00A7aRolling Miner: \u00A72\u2714 Ready";

    private enum Target {
        PICKAXE,
        ROLLING,
        COMMISSIONS,
        COMMISSION_STATS
    }

    private final Screen parent;
    private Target dragging;
    private double grabDx;
    private double grabDy;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("Move HUDs"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(ctx, mouseX, mouseY, deltaTicks);

        ctx.fill(0, 8, ctx.guiWidth(), 39, 0xA0101018);
        ctx.centeredText(font, Component.literal("Drag enabled HUDs to move them; scroll over one to resize"),
            ctx.guiWidth() / 2, 13, 0xFFE9ECF6);
        ctx.centeredText(font, Component.literal("Snaps to screen and nearby HUD edges (hold Shift for free movement)"),
            ctx.guiWidth() / 2, 26, 0xFF9EA4B3);

        if (isVisible(Target.PICKAXE)) {
            ctx.text(font, PICKAXE_PREVIEW, PickaxeCooldownHUD.getX(), PickaxeCooldownHUD.getY(),
                0xFFFFFFFF, true);
            ctx.outline(PickaxeCooldownHUD.getX() - 2, PickaxeCooldownHUD.getY() - 2,
                targetWidth(Target.PICKAXE) + 4, targetHeight(Target.PICKAXE) + 4, 0xFF7FA8DB);
        }
        if (isVisible(Target.ROLLING)) {
            ctx.text(font, ROLLING_PREVIEW, RollingMinerCooldown.getX(), RollingMinerCooldown.getY(),
                0xFFFFFFFF, true);
            ctx.outline(RollingMinerCooldown.getX() - 2, RollingMinerCooldown.getY() - 2,
                targetWidth(Target.ROLLING) + 4, targetHeight(Target.ROLLING) + 4, 0xFF7FDB8A);
        }
        if (!hasVisibleHud()) {
            ctx.centeredText(font, Component.literal("No movable HUDs are enabled."),
                ctx.guiWidth() / 2, ctx.guiHeight() / 2, 0xFFB5BAC8);
        }

        drawGuides(ctx);
        drawDoneButton(ctx, mouseX, mouseY);
    }

    private void drawDoneButton(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int buttonWidth = 84;
        int buttonHeight = 20;
        int x = ctx.guiWidth() / 2 - buttonWidth / 2;
        int y = ctx.guiHeight() - 34;
        boolean hovered = contains(mouseX, mouseY, x, y, buttonWidth, buttonHeight);
        ctx.fill(x, y, x + buttonWidth, y + buttonHeight, hovered ? 0xF02E5C34 : 0xF0203A24);
        ctx.outline(x, y, buttonWidth, buttonHeight, hovered ? 0xFF7FDB8A : 0xFF3E7A47);
        ctx.centeredText(font, Component.literal("Done"), ctx.guiWidth() / 2, y + 6, 0xFFDCF5DF);
    }

    private void drawGuides(GuiGraphicsExtractor ctx) {
        if (dragging == null || HudDragSnap.shiftHeld()) {
            return;
        }
        int x = targetX(dragging);
        int y = targetY(dragging);
        int targetWidth = targetWidth(dragging);
        int targetHeight = targetHeight(dragging);
        int centeredX = (ctx.guiWidth() - targetWidth) / 2;
        int centeredY = (ctx.guiHeight() - targetHeight) / 2;

        if (x == centeredX) {
            ctx.fill(ctx.guiWidth() / 2, 0, ctx.guiWidth() / 2 + 1, ctx.guiHeight(), 0x9060A5FA);
        } else if (x == 0 || x == ctx.guiWidth() - targetWidth) {
            int lineX = x == 0 ? 0 : ctx.guiWidth() - 1;
            ctx.fill(lineX, 0, lineX + 1, ctx.guiHeight(), 0x70FFFFFF);
        }
        if (y == centeredY) {
            ctx.fill(0, ctx.guiHeight() / 2, ctx.guiWidth(), ctx.guiHeight() / 2 + 1, 0x9060A5FA);
        } else if (y == 0 || y == ctx.guiHeight() - targetHeight) {
            int lineY = y == 0 ? 0 : ctx.guiHeight() - 1;
            ctx.fill(0, lineY, ctx.guiWidth(), lineY + 1, 0x70FFFFFF);
        }

        for (Target other : Target.values()) {
            if (other == dragging || !isVisible(other)) continue;
            int otherX = targetX(other);
            int otherY = targetY(other);
            int otherWidth = targetWidth(other);
            int otherHeight = targetHeight(other);

            if (x == otherX || x + targetWidth == otherX + otherWidth) {
                int lineX = x == otherX ? x : x + targetWidth;
                ctx.fill(lineX, 0, lineX + 1, ctx.guiHeight(), 0x9060A5FA);
            }
            if (x + targetWidth == otherX || x == otherX + otherWidth) {
                int lineX = x < otherX ? otherX : x;
                ctx.fill(lineX, 0, lineX + 1, ctx.guiHeight(), 0x907FDB8A);
            }
            if (x + targetWidth + HUD_GAP == otherX || x == otherX + otherWidth + HUD_GAP) {
                int lineX = x < otherX ? x + targetWidth + HUD_GAP / 2 : x - HUD_GAP / 2;
                ctx.fill(lineX, 0, lineX + 1, ctx.guiHeight(), 0x907FDB8A);
            }
            if (y == otherY || y + targetHeight == otherY + otherHeight) {
                int lineY = y == otherY ? y : y + targetHeight;
                ctx.fill(0, lineY, ctx.guiWidth(), lineY + 1, 0x9060A5FA);
            }
            if (y + targetHeight == otherY || y == otherY + otherHeight) {
                int lineY = y < otherY ? otherY : y;
                ctx.fill(0, lineY, ctx.guiWidth(), lineY + 1, 0x907FDB8A);
            }
            if (y + targetHeight + HUD_GAP == otherY || y == otherY + otherHeight + HUD_GAP) {
                int lineY = y < otherY ? y + targetHeight + HUD_GAP / 2 : y - HUD_GAP / 2;
                ctx.fill(0, lineY, ctx.guiWidth(), lineY + 1, 0x907FDB8A);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        if (click.button() == 0 && isDoneButton(mouseX, mouseY)) {
            onClose();
            return true;
        }
        if (click.button() == 0) {
            Target target = targetAt(mouseX, mouseY);
            if (target != null) {
                dragging = target;
                grabDx = click.x() - targetX(target);
                grabDy = click.y() - targetY(target);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging == null) {
            return super.mouseDragged(click, dx, dy);
        }
        int targetWidth = targetWidth(dragging);
        int targetHeight = targetHeight(dragging);
        int x = snapX(dragging, (int) Math.round(click.x() - grabDx));
        int y = snapY(dragging, (int) Math.round(click.y() - grabDy));
        setPosition(dragging, x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Target target = targetAt(mouseX, mouseY);
        if (target == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        float amount = (float) scrollY * 0.05f;
        switch (target) {
            case PICKAXE -> PickaxeCooldownHUD.setScale(PickaxeCooldownHUD.getScale() + amount);
            case ROLLING -> { }
            case COMMISSIONS -> CommissionHUD.setScale(CommissionHUD.getScale() + amount);
            case COMMISSION_STATS -> CommStatsHUD.setScale(CommStatsHUD.getScale() + amount);
        }
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

    private boolean hasVisibleHud() {
        return isVisible(Target.PICKAXE)
            || isVisible(Target.ROLLING)
            || isVisible(Target.COMMISSIONS)
            || isVisible(Target.COMMISSION_STATS);
    }

    private boolean isVisible(Target target) {
        return switch (target) {
            case PICKAXE -> PickaxeCooldownHUD.isEnabled();
            case ROLLING -> RollingMinerCooldown.isEnabled();
            case COMMISSIONS -> CommissionHUD.isEnabled();
            case COMMISSION_STATS -> CommTracker.isStatsEnabled();
        };
    }

    private Target targetAt(double mouseX, double mouseY) {
        Target[] hitOrder = {Target.COMMISSION_STATS, Target.COMMISSIONS, Target.ROLLING, Target.PICKAXE};
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
            case PICKAXE -> PickaxeCooldownHUD.getX();
            case ROLLING -> RollingMinerCooldown.getX();
            case COMMISSIONS -> CommissionHUD.getX();
            case COMMISSION_STATS -> CommStatsHUD.getX();
        };
    }

    private int targetY(Target target) {
        return switch (target) {
            case PICKAXE -> PickaxeCooldownHUD.getY();
            case ROLLING -> RollingMinerCooldown.getY();
            case COMMISSIONS -> CommissionHUD.getY();
            case COMMISSION_STATS -> CommStatsHUD.getY();
        };
    }

    private int targetWidth(Target target) {
        return switch (target) {
            case PICKAXE -> Math.max(120, PickaxeCooldownHUD.getWidth());
            case ROLLING -> RollingMinerCooldown.getWidth();
            case COMMISSIONS -> Math.max(120, CommissionHUD.getWidth());
            case COMMISSION_STATS -> Math.max(60, CommStatsHUD.getWidth());
        };
    }

    private int targetHeight(Target target) {
        return switch (target) {
            case PICKAXE -> 12;
            case ROLLING -> RollingMinerCooldown.getHeight();
            case COMMISSIONS -> Math.max(60, CommissionHUD.getHeight());
            case COMMISSION_STATS -> Math.max(14, CommStatsHUD.getHeight());
        };
    }

    private void setPosition(Target target, int x, int y) {
        switch (target) {
            case PICKAXE -> PickaxeCooldownHUD.setPosition(x, y);
            case ROLLING -> RollingMinerCooldown.setPosition(x, y);
            case COMMISSIONS -> CommissionHUD.setPosition(x, y);
            case COMMISSION_STATS -> CommStatsHUD.setPosition(x, y);
        }
    }

    private int snapX(Target target, int value) {
        int targetWidth = targetWidth(target);
        int maximum = Math.max(0, width - targetWidth);
        int clamped = Math.max(0, Math.min(value, maximum));
        if (HudDragSnap.shiftHeld()) return clamped;

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
        if (HudDragSnap.shiftHeld()) return clamped;

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

    private boolean isDoneButton(double mouseX, double mouseY) {
        return contains(mouseX, mouseY, width / 2 - 42, height - 34, 84, 20);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
