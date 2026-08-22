package forfun.miningqol.client.gui

import forfun.miningqol.client.AutoClickerHUD
import forfun.miningqol.client.AutoClickerManager
import forfun.miningqol.client.AutoForgeManager
import forfun.miningqol.client.CommClaimManager
import forfun.miningqol.client.EmptyStashManager
import forfun.miningqol.client.EtherwarpClickManager
import forfun.miningqol.client.InShaftClickManager
import forfun.miningqol.client.ShaftClickerManager
import forfun.miningqol.client.ShaftJoinCdManager
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.Dropdown

/**
 * Cheat-only sidebar categories (Clickers, Automation), contributed to the
 * shared main screen through ExtraCategories. Registered from CheatBootstrap
 * so legit builds never see them.
 */
object CheatGui {
    @JvmStatic
    fun register() {
        ExtraCategories.add(GuiCategory("Clickers", listOf(
            GuiFeature("CoalClick", "Auto clicker — toggle with its keybind", SettingsUi.RED,
                detail = { _, w, width -> coalClick(w, width) },
                status = { AutoClickerManager.isEnabled() }),
            GuiFeature("In Shaft Click", "Cold-aware clicker", SettingsUi.BLUE,
                detail = { _, w, width -> inShaftClick(w, width) },
                status = { InShaftClickManager.isEnabled() }),
            GuiFeature("Shaft Clicker", "Mineshaft clicker", SettingsUi.TEAL,
                detail = { _, w, width -> shaftClicker(w, width) },
                status = { ShaftClickerManager.isEnabled() })
        )))

        ExtraCategories.add(GuiCategory("Automation", listOf(
            GuiFeature("Comm Claim", "/claimcomms — auto commission claiming", SettingsUi.YELLOW,
                detail = { _, w, width -> commClaim(w, width) }),
            GuiFeature("Empty Stash", "Supercraft-loop your material stash", SettingsUi.PURPLE,
                detail = { _, w, width -> emptyStash(w, width) },
                status = { EmptyStashManager.isRunning() }),
            GuiFeature("Auto Forge", "Craft picker whenever The Forge opens", SettingsUi.ORANGE,
                detail = { _, w, width -> autoForge(w, width) },
                status = { AutoForgeManager.isEnabled() }),
            GuiFeature("HOTM Presets", "Heart of the Mountain editor + auto-apply", SettingsUi.TEAL,
                open = { forfun.miningqol.client.hotm.HotmChestScreen.open() })
        )))

        ExtraMiscRows.addToggle("Shaft Join Cooldown", "Block clicks in Glacite GUIs right after you enter a shaft",
            { ShaftJoinCdManager.isEnabled() }) {
            ShaftJoinCdManager.setEnabled(it)
        }
        ExtraWaypointRows.addToggle("Etherwarp Click",
            "Sneak + aim at an etherwarp waypoint (/mqo ether <n>) to right-click",
            { EtherwarpClickManager.isEnabled() }) {
            EtherwarpClickManager.setEnabled(it)
        }
    }

    private fun coalClick(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.RED
        var y = 0f
        y = SettingsUi.inlineSlider(w, width, y, "Mining Slot", 0f, 8f, 1f,
            AutoClickerManager.getMiningSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            AutoClickerManager.setMiningSlot(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "Second Drill", "Rotate a second drill into the cycle", accent,
            { AutoClickerManager.isSecondDrillEnabled() }) { AutoClickerManager.setEnableSecondDrill(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Second Drill Slot", 0f, 8f, 1f,
            AutoClickerManager.getSecondDrillSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            AutoClickerManager.setSecondDrillSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Main Drill Delay", 1f, 10f, 1f,
            AutoClickerManager.getMainDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            AutoClickerManager.setMainDrillDelay(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Second Drill Delay", 1f, 10f, 1f,
            AutoClickerManager.getSecondDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            AutoClickerManager.setSecondDrillDelay(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "HUD", "Show the CoalClick status HUD", accent,
            { AutoClickerHUD.isEnabled() }) { AutoClickerHUD.setEnabled(it) }
        return y
    }

    private fun inShaftClick(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.BLUE
        var y = 0f
        y = SettingsUi.inlineSlider(w, width, y, "Mining Slot", 0f, 8f, 1f,
            InShaftClickManager.getMiningSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            InShaftClickManager.setMiningSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Second Drill Slot", 0f, 8f, 1f,
            InShaftClickManager.getSecondDrillSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            InShaftClickManager.setSecondDrillSlot(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "Third Drill", "Swap to a third drill for the ability right-click", accent,
            { InShaftClickManager.isThirdDrillEnabled() }) { InShaftClickManager.setThirdDrillEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Third Drill Slot", 0f, 8f, 1f,
            InShaftClickManager.getThirdDrillSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            InShaftClickManager.setThirdDrillSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Main Drill Delay", 1f, 10f, 1f,
            InShaftClickManager.getMainDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            InShaftClickManager.setMainDrillDelay(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Third Drill Delay", 1f, 10f, 1f,
            InShaftClickManager.getSecondDrillDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            InShaftClickManager.setSecondDrillDelay(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Cold Threshold", 0f, 100f, 1f,
            InShaftClickManager.getColdThreshold().toFloat(), accent, { "${it.toInt()} cold" }) {
            InShaftClickManager.setColdThreshold(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "Toggle Message", "Chat message when toggled via keybind", accent,
            { InShaftClickManager.isShowToggleMessage() }) { InShaftClickManager.setShowToggleMessage(it) }
        return y
    }

    private fun shaftClicker(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.TEAL
        var y = 0f
        y = SettingsUi.inlineSlider(w, width, y, "Mining Slot", 0f, 8f, 1f,
            ShaftClickerManager.getMiningSlot().toFloat(), accent, { "slot ${it.toInt() + 1}" }) {
            ShaftClickerManager.setMiningSlot(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "Toggle Message", "Chat message when toggled via keybind", accent,
            { ShaftClickerManager.isShowToggleMessage() }) { ShaftClickerManager.setShowToggleMessage(it) }
        return y
    }

    private fun commClaim(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.YELLOW
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Auto Trigger", "Run when all mining commissions complete", accent,
            { CommClaimManager.isAutoTrigger() }) { CommClaimManager.setAutoTrigger(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Loadout Swap", "Swap armor via /loadout during the claim", accent,
            { CommClaimManager.isWardrobeSwap() }) { CommClaimManager.setWardrobeSwap(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Batch Mining", "On: wait for ALL comms. Off: claim each as it completes", accent,
            { CommClaimManager.isBatchMining() }) { CommClaimManager.setBatchMining(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Block Input", "Ignore your clicks/keys while a claim runs (Esc aborts)", accent,
            { CommClaimManager.isBlockInput() }) { CommClaimManager.setBlockInput(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Hide GUI", "Don't render loadout/pigeon menus during a claim", accent,
            { CommClaimManager.isHideGui() }) { CommClaimManager.setHideGui(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Bat Person Loadout", 1f, 12f, 1f,
            CommClaimManager.getBatPersonSlot().toFloat(), accent, { "loadout ${it.toInt()}" }) {
            CommClaimManager.setBatPersonSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Divan Loadout", 1f, 12f, 1f,
            CommClaimManager.getDivanSlot().toFloat(), accent, { "loadout ${it.toInt()}" }) {
            CommClaimManager.setDivanSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Refined Tool Slot", 0f, 8f, 1f,
            CommClaimManager.getRefinedToolSlot().toFloat(), accent, { "hotbar ${it.toInt() + 1}" }) {
            CommClaimManager.setRefinedToolSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Tick Delay", 1f, 10f, 1f,
            CommClaimManager.getTickDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            CommClaimManager.setTickDelay(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "GUI Wait Delay", 1f, 10f, 1f,
            CommClaimManager.getGuiWaitDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            CommClaimManager.setGuiWaitDelay(it.toInt())
        }
        return y
    }

    private fun emptyStash(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.PURPLE
        var y = 0f

        // Material card: label + item icon + dropdown
        val materials = EmptyStashManager.Material.entries
        val card = SettingsUi.inlineCard(w, width, y, 60f)

        Text("Material", SettingsUi.TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(card)
        Text("What to pull out of the stash", SettingsUi.TEXT_MUTED, 11f, false)
            .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
            .childOf(card)

        // Real item textures, loaded straight from the vanilla jar off the classpath.
        val iconPaths = mapOf(
            EmptyStashManager.Material.COAL to "/assets/minecraft/textures/item/coal.png",
            EmptyStashManager.Material.REDSTONE to "/assets/minecraft/textures/item/redstone.png",
            EmptyStashManager.Material.LAPIS to "/assets/minecraft/textures/item/lapis_lazuli.png",
            EmptyStashManager.Material.HARDSTONE to "/assets/minecraft/textures/block/stone.png",
            EmptyStashManager.Material.GLACITE to "/assets/minecraft/textures/block/packed_ice.png"
        )
        val icons = materials.associateWith { m ->
            xyz.meowing.vexel.components.core.SvgImage(iconPaths[m] ?: "", 22f, 22f)
                .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .setOffset(-176f, 0f)
                .ignoreMouseEvents()
                .childOf(card)
                .also { it.visible = (m == EmptyStashManager.getMaterial()) }
        }

        val dropdown = Dropdown(
            options = materials.map { it.displayName },
            selectedIndex = materials.indexOf(EmptyStashManager.getMaterial()).coerceAtLeast(0)
        )
            .setSizing(140f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-16f, 0f)
            .childOf(card)
        dropdown.onValueChange { index ->
            val material = materials[index as Int]
            EmptyStashManager.setMaterial(material)
            icons.forEach { (m, icon) -> icon.visible = (m == material) }
        }
        y += 72f

        y = SettingsUi.inlineSlider(w, width, y, "Action Delay", 1f, 10f, 1f,
            EmptyStashManager.getActionDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            EmptyStashManager.setActionDelay(it.toInt())
        }

        // Start/Stop card
        val runCard = SettingsUi.inlineCard(w, width, y, 60f)
        Text("Run", SettingsUi.TEXT_PRIMARY, 16f, true)
            .setPositioning(18f, Pos.ParentPixels, 12f, Pos.ParentPixels)
            .childOf(runCard)
        Text("Stops on its own when the stash is empty", SettingsUi.TEXT_MUTED, 11f, false)
            .setPositioning(18f, Pos.ParentPixels, 36f, Pos.ParentPixels)
            .childOf(runCard)

        val runButton = Button(
            if (EmptyStashManager.isRunning()) "Stop" else "Start", SettingsUi.TEXT_PRIMARY, fontSize = 13f)
            .setSizing(110f, Size.Pixels, 32f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .setOffset(-14f, 0f)
            .backgroundColor(SettingsUi.tint(accent, 0.14f))
            .borderColor(SettingsUi.tint(accent, 0.4f))
            .borderRadius(8f)
            .borderThickness(1f)
            .childOf(runCard)
        runButton.onClick { _ ->
            EmptyStashManager.toggle()
            runButton.text = if (EmptyStashManager.isRunning()) "Stop" else "Start"
            true
        }
        y += 72f
        return y
    }

    private fun autoForge(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.ORANGE
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Show the craft picker when The Forge opens", accent,
            { AutoForgeManager.isEnabled() }) { AutoForgeManager.setEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Tick Delay", 1f, 10f, 1f,
            AutoForgeManager.getTickDelay().toFloat(), accent, { "${it.toInt()} ticks" }) {
            AutoForgeManager.setTickDelay(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Amount", 1f, 7f, 1f,
            AutoForgeManager.getRunCount().toFloat(), accent, { "${it.toInt()}x per click" }) {
            AutoForgeManager.setRunCount(it.toInt())
        }
        return y
    }
}
