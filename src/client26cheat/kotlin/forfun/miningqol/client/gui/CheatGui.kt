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
import xyz.meowing.vexel.elements.TextInput

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
                detail = { host, w, width -> autoForge(host, w, width) },
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
        y = SettingsUi.inlineSlider(w, width, y, "Mining Slot", 1f, 9f, 1f,
            (AutoClickerManager.getMiningSlot() + 1).toFloat(), accent, { "slot ${it.toInt()}" }) {
            AutoClickerManager.setMiningSlot(it.toInt() - 1)
        }
        y = SettingsUi.inlineToggle(w, width, y, "Second Drill", "Rotate a second drill into the cycle", accent,
            { AutoClickerManager.isSecondDrillEnabled() }) { AutoClickerManager.setEnableSecondDrill(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Second Drill Slot", 1f, 9f, 1f,
            (AutoClickerManager.getSecondDrillSlot() + 1).toFloat(), accent, { "slot ${it.toInt()}" }) {
            AutoClickerManager.setSecondDrillSlot(it.toInt() - 1)
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
        y = SettingsUi.inlineSlider(w, width, y, "Mining Slot", 1f, 9f, 1f,
            (InShaftClickManager.getMiningSlot() + 1).toFloat(), accent, { "slot ${it.toInt()}" }) {
            InShaftClickManager.setMiningSlot(it.toInt() - 1)
        }
        y = SettingsUi.inlineSlider(w, width, y, "Second Drill Slot", 1f, 9f, 1f,
            (InShaftClickManager.getSecondDrillSlot() + 1).toFloat(), accent, { "slot ${it.toInt()}" }) {
            InShaftClickManager.setSecondDrillSlot(it.toInt() - 1)
        }
        y = SettingsUi.inlineToggle(w, width, y, "Third Drill", "Swap to a third drill for the ability right-click", accent,
            { InShaftClickManager.isThirdDrillEnabled() }) { InShaftClickManager.setThirdDrillEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Third Drill Slot", 1f, 9f, 1f,
            (InShaftClickManager.getThirdDrillSlot() + 1).toFloat(), accent, { "slot ${it.toInt()}" }) {
            InShaftClickManager.setThirdDrillSlot(it.toInt() - 1)
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
        y = SettingsUi.inlineSlider(w, width, y, "Mining Slot", 1f, 9f, 1f,
            (ShaftClickerManager.getMiningSlot() + 1).toFloat(), accent, { "slot ${it.toInt()}" }) {
            ShaftClickerManager.setMiningSlot(it.toInt() - 1)
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
        y = SettingsUi.inlineToggle(w, width, y, "Loadout Swap", "Swap loadouts around the claim via /loadout", accent,
            { CommClaimManager.isWardrobeSwap() }) { CommClaimManager.setWardrobeSwap(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Batch Mining", "On: wait for ALL comms. Off: claim each as it completes", accent,
            { CommClaimManager.isBatchMining() }) { CommClaimManager.setBatchMining(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Block Input", "Ignore your clicks/keys while a claim runs (Esc aborts)", accent,
            { CommClaimManager.isBlockInput() }) { CommClaimManager.setBlockInput(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Hide GUI", "Don't render loadout/pigeon menus during a claim", accent,
            { CommClaimManager.isHideGui() }) { CommClaimManager.setHideGui(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Claim Loadout", 1f, 12f, 1f,
            CommClaimManager.getBatPersonSlot().toFloat(), accent, { "loadout ${it.toInt()}" }) {
            CommClaimManager.setBatPersonSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Return Loadout", 1f, 12f, 1f,
            CommClaimManager.getDivanSlot().toFloat(), accent, { "loadout ${it.toInt()}" }) {
            CommClaimManager.setDivanSlot(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Mining Tool Slot", 1f, 9f, 1f,
            (CommClaimManager.getRefinedToolSlot() + 1).toFloat(), accent, { "hotbar ${it.toInt()}" }) {
            CommClaimManager.setRefinedToolSlot(it.toInt() - 1)
        }
        y = SettingsUi.inlineSlider(w, width, y, "Action Delay", 1f, 10f, 1f,
            CommClaimManager.getTickDelay().toFloat(), accent, { "${it.toInt() * 50}ms" }) {
            CommClaimManager.setTickDelay(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Menu Timeout", 5f, 20f, 1f,
            CommClaimManager.getGuiWaitDelay().coerceAtLeast(5).toFloat(), accent, { "${it.toInt()}s" }) {
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

    private fun autoForge(host: VexelMainScreen, w: Rectangle, width: Float): Float {
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

        y = SettingsUi.inlineSectionHeader(w, y, "Picker Buttons")
        for (label in AutoForgeManager.builtinLabels()) {
            y = SettingsUi.inlineToggle(w, width, y, label, "Show this craft on the picker", accent,
                { AutoForgeManager.isBuiltinShown(label) }) { AutoForgeManager.setBuiltinShown(label, it) }
        }

        y = SettingsUi.inlineSectionHeader(w, y, "Recorded Crafts")
        y = SettingsUi.inlineCard(w, width, y, 40f).let {
            Text("Press Record, open The Forge and click through a craft once. It saves itself when The Forge reopens.",
                SettingsUi.TEXT_MUTED, 11f, false)
                .setPositioning(18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .childOf(it)
            y + 52f
        }

        val armed = AutoForgeManager.isRecordArmed()
        val recordColor = if (armed) SettingsUi.RED else accent
        Button(if (armed) "● Armed — open The Forge (click to cancel)" else "● Record next craft",
            SettingsUi.TEXT_PRIMARY, fontSize = 14f)
            .setSizing(if (armed) 320f else 200f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, y + 4f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.tint(recordColor, 0.14f))
            .borderColor(SettingsUi.edge(recordColor, 0.65f))
            .borderRadius(10f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .hoverColors(SettingsUi.tint(recordColor, 0.24f), SettingsUi.TEXT_PRIMARY)
            .onClick { _ ->
                AutoForgeManager.armRecording()
                host.refreshDetail()
                true
            }
            .childOf(w)
        y += 56f

        for (recorded in AutoForgeManager.getRecordedCrafts().toList()) {
            val card = SettingsUi.inlineCard(w, width, y, 92f)

            Text("Name", SettingsUi.TEXT_MUTED, 12f, false)
                .setPositioning(18f, Pos.ParentPixels, 14f, Pos.ParentPixels)
                .childOf(card)
            val labelInput = TextInput(initialValue = recorded.label ?: "", placeholder = "Shown on the button", fontSize = 13f)
                .setSizing(220f, Size.Pixels, 36f, Size.Pixels)
                .setPositioning(18f, Pos.ParentPixels, 38f, Pos.ParentPixels)
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
                .borderRadius(9f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .childOf(card)
            labelInput.onValueChange { value ->
                recorded.label = value as String
                AutoForgeManager.refreshRecordedCrafts()
            }

            Text("Clicks (${recorded.steps.size})", SettingsUi.TEXT_MUTED, 12f, false)
                .setPositioning(256f, Pos.ParentPixels, 14f, Pos.ParentPixels)
                .childOf(card)
            Text(recorded.summary(), SettingsUi.TEXT_SECONDARY, 12f, false)
                .setPositioning(256f, Pos.ParentPixels, 48f, Pos.ParentPixels)
                .childOf(card)

            // Show/hide on the picker without losing the recording.
            val shownColor = if (recorded.shown) SettingsUi.GREEN else SettingsUi.TEXT_DIM
            Button(if (recorded.shown) "Shown" else "Hidden", SettingsUi.TEXT_PRIMARY, fontSize = 12f)
                .setSizing(70f, Size.Pixels, 40f, Size.Pixels)
                .setPositioning(-70f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .backgroundColor(SettingsUi.tint(shownColor, if (recorded.shown) 0.16f else 0.08f))
                .borderColor(SettingsUi.edge(shownColor, if (recorded.shown) 0.8f else 0.4f))
                .borderRadius(9f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .hoverColors(SettingsUi.tint(shownColor, 0.26f), SettingsUi.TEXT_PRIMARY)
                .onClick { _ ->
                    recorded.shown = !recorded.shown
                    AutoForgeManager.refreshRecordedCrafts()
                    host.refreshDetail()
                    true
                }
                .childOf(card)

            Button("x", SettingsUi.RED, fontSize = 20f)
                .setSizing(40f, Size.Pixels, 40f, Size.Pixels)
                .setPositioning(-18f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.RED, 0.6f))
                .borderRadius(9f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .hoverColors(SettingsUi.alpha(SettingsUi.CARD_HOVER), SettingsUi.RED)
                .onClick { _ ->
                    AutoForgeManager.removeRecordedCraft(recorded)
                    host.refreshDetail()
                    true
                }
                .childOf(card)

            y += 104f
        }

        return y
    }
}
