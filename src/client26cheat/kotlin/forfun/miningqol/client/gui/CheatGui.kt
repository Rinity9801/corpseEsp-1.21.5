package forfun.miningqol.client.gui

import forfun.miningqol.client.AutoClickerHUD
import forfun.miningqol.client.AutoClickerManager
import forfun.miningqol.client.CommClaimManager
import forfun.miningqol.client.EmptyStashManager
import forfun.miningqol.client.InShaftClickManager
import forfun.miningqol.client.ShaftClickerManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Cheat-only settings screens, contributed to the shared main menu through
 * ExtraCategories. Registered from CheatBootstrap so legit builds never see them.
 */
object CheatGui {
    @JvmStatic
    fun register() {
        ExtraCategories.add("CoalClick", "Auto clicker + rod swap", 0xFFFF7788.toInt()) {
            Minecraft.getInstance().setScreen(AutoClickerCategoryScreen(it))
        }
        ExtraCategories.add("In Shaft Click", "Cold-aware clicker", 0xFF77BBFF.toInt()) {
            Minecraft.getInstance().setScreen(InShaftClickCategoryScreen(it))
        }
        ExtraCategories.add("Shaft Clicker", "Mineshaft clicker", 0xFF99FFBB.toInt()) {
            Minecraft.getInstance().setScreen(ShaftClickerCategoryScreen(it))
        }
        ExtraCategories.add("Comm Claim", "Auto commission claiming", 0xFFFFDD66.toInt()) {
            Minecraft.getInstance().setScreen(CommClaimCategoryScreen(it))
        }
        ExtraCategories.add("Empty Stash", "Supercraft-loop your material stash", 0xFFBB99FF.toInt()) {
            Minecraft.getInstance().setScreen(EmptyStashCategoryScreen(it))
        }
        ExtraCategories.add("HOTM Presets", "Heart of the Mountain editor + auto-apply", 0xFF66FFCC.toInt()) {
            forfun.miningqol.client.hotm.HotmChestScreen.open()
        }
    }
}

class AutoClickerCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "CoalClick Settings") {
    private val accent = 0xFFFF7788.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 660f, "CoalClick", "Toggle with its keybind (Controls menu) — always starts disabled")

        var y = 110f
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Mining Slot", 0f, 8f, 1f,
            AutoClickerManager.getMiningSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            AutoClickerManager.setMiningSlot(it.toInt())
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Rod Swap", "Swap to fishing rod between cycles", accent,
            AutoClickerManager.isRodSwapEnabled()) {
            AutoClickerManager.setEnableRodSwap(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Second Drill", "Rotate a second drill into the cycle", accent,
            AutoClickerManager.isSecondDrillEnabled()) {
            AutoClickerManager.setEnableSecondDrill(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Second Drill Slot", 0f, 8f, 1f,
            AutoClickerManager.getSecondDrillSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            AutoClickerManager.setSecondDrillSlot(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Main Drill Delay", 1f, 10f, 1f,
            AutoClickerManager.getMainDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            AutoClickerManager.setMainDrillDelay(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Second Drill Delay", 1f, 10f, 1f,
            AutoClickerManager.getSecondDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            AutoClickerManager.setSecondDrillDelay(it.toInt())
        }
        SettingsUi.toggleRow(panel, panelWidth, y, "HUD", "Show the CoalClick status HUD", accent,
            AutoClickerHUD.isEnabled()) {
            AutoClickerHUD.setEnabled(it)
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}

class InShaftClickCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "In Shaft Click Settings") {
    private val accent = 0xFF77BBFF.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 660f, "In Shaft Click", "Toggle with its keybind (Controls menu) — always starts disabled")

        var y = 110f
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Mining Slot", 0f, 8f, 1f,
            InShaftClickManager.getMiningSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            InShaftClickManager.setMiningSlot(it.toInt())
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Rod Swap", "Swap to fishing rod between cycles", accent,
            InShaftClickManager.isRodSwapEnabled()) {
            InShaftClickManager.setEnableRodSwap(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Second Drill Slot", 0f, 8f, 1f,
            InShaftClickManager.getSecondDrillSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            InShaftClickManager.setSecondDrillSlot(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Main Drill Delay", 1f, 10f, 1f,
            InShaftClickManager.getMainDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            InShaftClickManager.setMainDrillDelay(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Second Drill Delay", 1f, 10f, 1f,
            InShaftClickManager.getSecondDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            InShaftClickManager.setSecondDrillDelay(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Cold Threshold", 0f, 100f, 5f,
            InShaftClickManager.getColdThreshold().toFloat(), accent, { "${it.toInt()} cold" }) {
            InShaftClickManager.setColdThreshold(it.toInt())
        }
        SettingsUi.toggleRow(panel, panelWidth, y, "Toggle Message", "Chat message when toggled via keybind", accent,
            InShaftClickManager.isShowToggleMessage()) {
            InShaftClickManager.setShowToggleMessage(it)
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}

class ShaftClickerCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Shaft Clicker Settings") {
    private val accent = 0xFF99FFBB.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 340f, "Shaft Clicker", "Toggle with its keybind (Controls menu) — always starts disabled")

        var y = 110f
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Mining Slot", 0f, 8f, 1f,
            ShaftClickerManager.getMiningSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            ShaftClickerManager.setMiningSlot(it.toInt())
        }
        SettingsUi.toggleRow(panel, panelWidth, y, "Toggle Message", "Chat message when toggled via keybind", accent,
            ShaftClickerManager.isShowToggleMessage()) {
            ShaftClickerManager.setShowToggleMessage(it)
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}

class CommClaimCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Comm Claim Settings") {
    private val accent = 0xFFFFDD66.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 800f, "Comm Claim", "/claimcomms — automated commission claiming")

        var y = 110f
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Auto Trigger", "Run automatically when all mining commissions complete", accent,
            CommClaimManager.isAutoTrigger()) {
            CommClaimManager.setAutoTrigger(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Loadout Swap", "Swap armor via /loadout during the claim", accent,
            CommClaimManager.isWardrobeSwap()) {
            CommClaimManager.setWardrobeSwap(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Batch Mining", "On: wait for ALL mining commissions. Off: claim each as it completes", accent,
            CommClaimManager.isBatchMining()) {
            CommClaimManager.setBatchMining(it)
        }
        y = SettingsUi.toggleRow(panel, panelWidth, y, "Block Input", "Ignore your clicks/keys while a claim runs (Esc aborts)", accent,
            CommClaimManager.isBlockInput()) {
            CommClaimManager.setBlockInput(it)
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Bat Person Loadout", 1f, 12f, 1f,
            CommClaimManager.getBatPersonSlot().toFloat(), accent, { "loadout ${it.toInt()}" }) {
            CommClaimManager.setBatPersonSlot(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Divan Loadout", 1f, 12f, 1f,
            CommClaimManager.getDivanSlot().toFloat(), accent, { "loadout ${it.toInt()}" }) {
            CommClaimManager.setDivanSlot(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Refined Tool Slot", 0f, 8f, 1f,
            CommClaimManager.getRefinedToolSlot().toFloat(), accent, { "hotbar ${it.toInt() + 1}" }) {
            CommClaimManager.setRefinedToolSlot(it.toInt())
        }
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Tick Delay", 1f, 10f, 1f,
            CommClaimManager.getTickDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            CommClaimManager.setTickDelay(it.toInt())
        }
        SettingsUi.sliderRow(panel, panelWidth, y, "GUI Wait Delay", 1f, 10f, 1f,
            CommClaimManager.getGuiWaitDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            CommClaimManager.setGuiWaitDelay(it.toInt())
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}


class EmptyStashCategoryScreen(parentScreen: Screen) : BaseCategoryScreen(parentScreen, "Empty Stash Settings") {
    private val accent = 0xFFBB99FF.toInt()

    override fun afterInitialization() {
        SettingsUi.overlay(window)
        val panelWidth = 600f
        val panel = SettingsUi.panel(window, panelWidth, 420f,
            "Empty Stash", "/recipe supercraft loop + /viewstash — also /emptystash to toggle")

        // Material row: label + color swatch "icon" + dropdown
        val materials = EmptyStashManager.Material.entries
        val card = xyz.meowing.vexel.components.core.Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(SettingsUi.ROW_WIDTH, xyz.meowing.vexel.components.base.enums.Size.Pixels,
                SettingsUi.ROW_HEIGHT, xyz.meowing.vexel.components.base.enums.Size.Pixels)
            .setPositioning((panelWidth - SettingsUi.ROW_WIDTH) / 2f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 110f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels)
            .childOf(panel)

        xyz.meowing.vexel.components.core.Text("Material", 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 10f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels)
            .childOf(card)

        xyz.meowing.vexel.components.core.Text("What to pull out of the stash", 0xFF888888.toInt(), 12f, false)
            .setPositioning(16f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 33f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels)
            .childOf(card)

        // Real item textures, loaded straight from the vanilla jar off the classpath.
        val iconPaths = mapOf(
            EmptyStashManager.Material.COAL to "/assets/minecraft/textures/item/coal.png",
            EmptyStashManager.Material.REDSTONE to "/assets/minecraft/textures/item/redstone.png",
            EmptyStashManager.Material.LAPIS to "/assets/minecraft/textures/item/lapis_lazuli.png",
            EmptyStashManager.Material.HARDSTONE to "/assets/minecraft/textures/block/stone.png"
        )
        val icons = materials.associateWith { m ->
            xyz.meowing.vexel.components.core.SvgImage(iconPaths[m] ?: "", 22f, 22f)
                .setPositioning(0f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 0f,
                    xyz.meowing.vexel.components.base.enums.Pos.ParentCenter)
                .alignRight()
                .setOffset(-176f, 0f)
                .ignoreMouseEvents()
                .childOf(card)
                .also { it.visible = (m == EmptyStashManager.getMaterial()) }
        }

        val dropdown = xyz.meowing.vexel.elements.Dropdown(
            options = materials.map { it.displayName },
            selectedIndex = materials.indexOf(EmptyStashManager.getMaterial()).coerceAtLeast(0)
        )
            .setSizing(140f, xyz.meowing.vexel.components.base.enums.Size.Pixels,
                32f, xyz.meowing.vexel.components.base.enums.Size.Pixels)
            .setPositioning(0f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 0f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .childOf(card)
        dropdown.onValueChange { index ->
            val material = materials[index as Int]
            EmptyStashManager.setMaterial(material)
            icons.forEach { (m, icon) -> icon.visible = (m == material) }
        }

        var y = 110f + SettingsUi.ROW_HEIGHT + SettingsUi.ROW_SPACING
        y = SettingsUi.sliderRow(panel, panelWidth, y, "Action Delay", 1f, 10f, 1f,
            EmptyStashManager.getActionDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            EmptyStashManager.setActionDelay(it.toInt())
        }

        // Start/Stop row
        val runCard = xyz.meowing.vexel.components.core.Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f
        )
            .setSizing(SettingsUi.ROW_WIDTH, xyz.meowing.vexel.components.base.enums.Size.Pixels,
                SettingsUi.ROW_HEIGHT, xyz.meowing.vexel.components.base.enums.Size.Pixels)
            .setPositioning((panelWidth - SettingsUi.ROW_WIDTH) / 2f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, y,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels)
            .childOf(panel)

        xyz.meowing.vexel.components.core.Text("Run", 0xFFFFFFFF.toInt(), 17f, true)
            .setPositioning(16f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 10f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels)
            .childOf(runCard)

        xyz.meowing.vexel.components.core.Text("Stops on its own when the stash is empty", 0xFF888888.toInt(), 12f, false)
            .setPositioning(16f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 33f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentPixels)
            .childOf(runCard)

        val runButton = xyz.meowing.vexel.elements.Button(
            if (EmptyStashManager.isRunning()) "Stop" else "Start", 0xFFFFFFFF.toInt(), fontSize = 13f)
            .setSizing(110f, xyz.meowing.vexel.components.base.enums.Size.Pixels,
                32f, xyz.meowing.vexel.components.base.enums.Size.Pixels)
            .setPositioning(0f, xyz.meowing.vexel.components.base.enums.Pos.ParentPixels, 0f,
                xyz.meowing.vexel.components.base.enums.Pos.ParentCenter)
            .alignRight()
            .setOffset(-14f, 0f)
            .backgroundColor(0xFF2A2A3A.toInt())
            .borderColor(accent)
            .borderRadius(8f)
            .borderThickness(1f)
            .childOf(runCard)
        runButton.onClick { _ ->
            EmptyStashManager.toggle()
            runButton.text = if (EmptyStashManager.isRunning()) "Stop" else "Start"
            true
        }

        SettingsUi.backButton(panel) { saveAndClose() }
    }
}
