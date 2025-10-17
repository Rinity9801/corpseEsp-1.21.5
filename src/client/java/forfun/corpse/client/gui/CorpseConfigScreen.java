package forfun.corpse.client.gui;

import forfun.corpse.client.CorpseESP;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CorpseConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 200;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    private final Screen parent;

    public CorpseConfigScreen(Screen parent) {
        super(Text.literal("Corpse ESP Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int startY = panelY + 40;

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isLapisEnabled() ? "§9Lapis: §aON" : "§9Lapis: §cOFF"),
            button -> {
                CorpseESP.toggleLapis();
                button.setMessage(Text.literal(CorpseESP.isLapisEnabled() ? "§9Lapis: §aON" : "§9Lapis: §cOFF"));
            })
            .dimensions(buttonX, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isTungstenEnabled() ? "§fTungsten: §aON" : "§fTungsten: §cOFF"),
            button -> {
                CorpseESP.toggleTungsten();
                button.setMessage(Text.literal(CorpseESP.isTungstenEnabled() ? "§fTungsten: §aON" : "§fTungsten: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isUmberEnabled() ? "§6Umber: §aON" : "§6Umber: §cOFF"),
            button -> {
                CorpseESP.toggleUmber();
                button.setMessage(Text.literal(CorpseESP.isUmberEnabled() ? "§6Umber: §aON" : "§6Umber: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isVanguardEnabled() ? "§dVanguard: §aON" : "§dVanguard: §cOFF"),
            button -> {
                CorpseESP.toggleVanguard();
                button.setMessage(Text.literal(CorpseESP.isVanguardEnabled() ? "§dVanguard: §aON" : "§dVanguard: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            button -> this.close())
            .dimensions(buttonX, startY + BUTTON_SPACING * 4 + 10, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC000000);
        context.fill(panelX + 2, panelY + 2, panelX + PANEL_WIDTH - 2, panelY + PANEL_HEIGHT - 2, 0x88000000);

        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4488FF);
        context.drawBorder(panelX + 1, panelY + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 0x884488FF);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 15, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
