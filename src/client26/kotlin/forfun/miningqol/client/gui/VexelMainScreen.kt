package forfun.miningqol.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import xyz.meowing.vexel.animations.presets.fadeIn
import xyz.meowing.vexel.animations.types.EasingType
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelScreen

/**
 * 26.1.2 main settings menu — two-column grid of category cards. Cheat-only
 * categories are contributed through ExtraCategories (registered by CheatGui,
 * which only exists in the -cheat source tree).
 */
class VexelMainScreen : VexelScreen("MiningQOL Settings") {

    private class Category(
        val title: String,
        val description: String,
        val accent: Int,
        val open: (Screen) -> Unit
    )

    private fun categories(): List<Category> {
        val builtIn = listOf(
            Category("Commission HUD", "On-screen commission tracker", 0xFF88AAFF.toInt()) {
                Minecraft.getInstance().setScreen(CommissionHudCategoryScreen(it))
            },
            Category("Ordered Waypoints", "Guided mining routes (/mqo)", 0xFFAAFF88.toInt()) {
                Minecraft.getInstance().setScreen(OrderedWaypointsCategoryScreen(it))
            },
            Category("Shaft ESP", "Mineshaft highlights", 0xFF88DDFF.toInt()) {
                Minecraft.getInstance().setScreen(ShaftESPCategoryScreen(it))
            },
            Category("Corpse ESP", "Frozen corpse waypoints", 0xFFFFCC66.toInt()) {
                Minecraft.getInstance().setScreen(CorpseESPCategoryScreen(it))
            },
            Category("Pickaxe Cooldown", "Ability cooldown HUD", 0xFFCC88FF.toInt()) {
                Minecraft.getInstance().setScreen(PickaxeCooldownCategoryScreen(it))
            },
            Category("Misc", "Filet warning and other extras", 0xFFFFAA55.toInt()) {
                Minecraft.getInstance().setScreen(MiscCategoryScreen(it))
            }
        )
        return builtIn + ExtraCategories.entries.map { e ->
            Category(e.title, e.description, e.accent, e.open)
        }
    }

    override fun afterInitialization() {
        SettingsUi.overlay(window)

        val cats = categories()
        val columns = 2
        val rows = (cats.size + columns - 1) / columns
        val cardWidth = 335f
        val cardHeight = 78f
        val gap = 14f
        val panelWidth = 40f + columns * cardWidth + (columns - 1) * gap
        val panelHeight = 120f + rows * (cardHeight + gap) + 60f
        val panel = SettingsUi.panel(window, panelWidth, panelHeight,
            "MiningQOL", "Minecraft 26.1.2")

        cats.forEachIndexed { index, category ->
            val col = index % columns
            val row = index / columns
            val x = 20f + col * (cardWidth + gap)
            val y = 110f + row * (cardHeight + gap)

            val card = Rectangle(
                backgroundColor = 0xF01E1E1E.toInt(),
                borderColor = 0xFF2A2A2A.toInt(),
                borderRadius = 12f,
                borderThickness = 1f,
                hoverColor = 0xF0252525.toInt()
            )
                .setSizing(cardWidth, Size.Pixels, cardHeight, Size.Pixels)
                .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
                .childOf(panel)

            Rectangle(backgroundColor = category.accent, borderRadius = 12f)
                .setSizing(5f, Size.Pixels, 100f, Size.Percent)
                .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
                .ignoreMouseEvents()
                .childOf(card)
                .apply {
                    borderRadiusTopRight = 0f
                    borderRadiusBottomRight = 0f
                }

            Text(category.title, 0xFFFFFFFF.toInt(), 17f, true)
                .setPositioning(20f, Pos.ParentPixels, 18f, Pos.ParentPixels)
                .childOf(card)

            Text(category.description, 0xFF888888.toInt(), 11f, false)
                .setPositioning(20f, Pos.ParentPixels, 44f, Pos.ParentPixels)
                .childOf(card)

            card.onClick { _ ->
                category.open(this)
                true
            }

            card.fadeIn(300 + index * 60L, EasingType.EASE_OUT)
        }
    }
}
