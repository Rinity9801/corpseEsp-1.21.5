package forfun.miningqol.client.gui

import forfun.miningqol.client.BlockOverlay
import forfun.miningqol.client.CommTracker
import forfun.miningqol.client.CommissionHUD
import forfun.miningqol.client.CorpseESP
import forfun.miningqol.client.CritParticleDrop
import forfun.miningqol.client.EfficientMinerOverlay
import forfun.miningqol.client.EntityEspMode
import forfun.miningqol.client.FiletWarning
import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.MqoChat
import forfun.miningqol.client.PickaxeCooldownHUD
import forfun.miningqol.client.ForgeDisplay
import forfun.miningqol.client.gui.HudPositionScreen
import forfun.miningqol.client.party.MineshaftAutoParty
import forfun.miningqol.client.party.PartyAutoAccept
import net.minecraft.client.Minecraft
import forfun.miningqol.client.RollingMinerCooldown
import forfun.miningqol.client.ShaftESP
import forfun.miningqol.client.SoundBlocker
import forfun.miningqol.client.waypoints.OrderedWaypointManager
import xyz.meowing.vexel.components.core.Rectangle

/**
 * Detail-panel builders for the built-in features. Each populates the detail
 * wrapper from y = 0 and returns the y below its last control.
 */
object FeatureDetails {

    fun blockOverlay(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.SKY
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Replace the vanilla targeted-block outline", accent,
            { BlockOverlay.isEnabled() }) { BlockOverlay.setEnabled(it) }
        y = SettingsUi.inlineChoice(w, width, y, "Mode", "Click to cycle the overlay style", accent,
            { BlockOverlay.getMode().displayName }) { BlockOverlay.cycleMode() }
        y = SettingsUi.inlineColor(w, width, y, "Outline Color",
            { BlockOverlay.getOutlineColor() },
            { r, g, b -> BlockOverlay.setOutlineColor(r, g, b) },
            { BlockOverlay.getOutlineAlpha() },
            { BlockOverlay.setOutlineAlpha(it) })
        y = SettingsUi.inlineColor(w, width, y, "Fill Color",
            { BlockOverlay.getFillColor() },
            { r, g, b -> BlockOverlay.setFillColor(r, g, b) },
            { BlockOverlay.getFillAlpha() },
            { BlockOverlay.setFillAlpha(it) })
        y = SettingsUi.inlineSlider(w, width, y, "Line Width", 1f, 10f, 0.1f,
            BlockOverlay.getLineWidth(), accent, { String.format("%.1f px", it) }) {
            BlockOverlay.setLineWidth(it)
        }
        y = SettingsUi.inlineToggle(w, width, y, "Phase", "Draw the overlay through walls", accent,
            { BlockOverlay.isPhase() }) { BlockOverlay.setPhase(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Hide with Etherwarp", "Hide while sneaking with an ethermerged AOTE/AOTV", accent,
            { BlockOverlay.isHideDuringEtherwarp() }) { BlockOverlay.setHideDuringEtherwarp(it) }
        return y
    }

    fun commissionHud(host: VexelMainScreen, w: Rectangle, width: Float): Float {
        val accent = SettingsUi.BLUE
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Show the commission HUD while in the mines", accent,
            { CommissionHUD.isEnabled() }) { CommissionHUD.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Background", "Dark backdrop behind the HUD", accent,
            { CommissionHUD.isBackgroundEnabled() }) { CommissionHUD.setBackgroundEnabled(it) }
        y = SettingsUi.inlineDropdown(
            w, width, y, "Layout", "Arrange commissions in a 2x2 grid or one stacked column", accent,
            listOf("2x2 Grid", "Stacked Column"),
            if (CommissionHUD.getLayoutMode() == CommissionHUD.LayoutMode.COLUMN) 1 else 0
        ) { index ->
            CommissionHUD.setLayoutMode(
                if (index == 1) CommissionHUD.LayoutMode.COLUMN else CommissionHUD.LayoutMode.GRID
            )
        }
        y = SettingsUi.inlineToggle(w, width, y, "Commission Stats", "Total completed + comms/hour (/commtrack reset)", accent,
            { CommTracker.isStatsEnabled() }) { CommTracker.setStatsEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Scale", 0.5f, 2.0f, 0.05f,
            CommissionHUD.getScale(), accent, { String.format("%.2fx", it) }) {
            CommissionHUD.setScale(it)
        }
        return y
    }

    fun orderedWaypoints(host: VexelMainScreen, w: Rectangle, width: Float): Float {
        val accent = SettingsUi.GREEN
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Render and advance the loaded route", accent,
            { OrderedWaypointManager.isEnabledRaw() }) { OrderedWaypointManager.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Trace Line", "Line from your crosshair to the next waypoint", accent,
            { OrderedWaypointManager.isTraceLineEnabled() }) { OrderedWaypointManager.setTraceLineEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Show Number", "Waypoint #number on the label", accent,
            { OrderedWaypointManager.isShowName() }) { OrderedWaypointManager.setShowName(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Show Distance", "Distance in meters on the label", accent,
            { OrderedWaypointManager.isShowDistance() }) { OrderedWaypointManager.setShowDistance(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Trigger Range", 1f, 10f, 0.5f,
            OrderedWaypointManager.getWaypointRange(), accent, { String.format("%.1f blocks", it) }) {
            OrderedWaypointManager.setWaypointRange(it)
        }
        y = SettingsUi.inlineSlider(w, width, y, "Upcoming Waypoints", 0f, 5f, 1f,
            OrderedWaypointManager.getNextCount().toFloat(), accent, { "${it.toInt()} shown" }) {
            OrderedWaypointManager.setNextCount(it.toInt())
        }
        for (row in ExtraWaypointRows.toggles) {
            y = SettingsUi.inlineToggle(w, width, y, row.title, row.description, accent, row.get, row.set)
        }
        return y
    }

    fun waypointBlockCheck(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.GREEN
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Lobby Check", "Scan upcoming waypoints; flag wrong lobbies red", accent,
            { OrderedWaypointManager.isLobbyCheckEnabled() }) { OrderedWaypointManager.setLobbyCheckEnabled(it) }
        y = SettingsUi.inlineTextInput(w, width, y, "Lobby Check Block", "Block id to expect (e.g. minecraft:coal_ore)",
            OrderedWaypointManager.getLobbyCheckBlock()) {
            OrderedWaypointManager.setLobbyCheckBlock(it.trim())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Check Interval", 2f, 30f, 1f,
            OrderedWaypointManager.getLobbyCheckInterval().toFloat(), accent, { "every ${it.toInt()} waypoints" }) {
            OrderedWaypointManager.setLobbyCheckInterval(it.toInt())
        }
        y = SettingsUi.inlineSlider(w, width, y, "Check Radius", 1f, 5f, 1f,
            OrderedWaypointManager.getLobbyCheckRadius().toFloat(), accent, { "${it.toInt()} blocks" }) {
            OrderedWaypointManager.setLobbyCheckRadius(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "Skip Mined-Out Waypoints", "/mqo skip keeps going past empty waypoints", accent,
            { OrderedWaypointManager.isSkipObstructed() }) { OrderedWaypointManager.setSkipObstructed(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Mined-Out Threshold", 0f, 20f, 1f,
            OrderedWaypointManager.getObstructedThreshold().toFloat(), accent, { "${it.toInt()} blocks or fewer" }) {
            OrderedWaypointManager.setObstructedThreshold(it.toInt())
        }
        return y
    }

    fun waypointOutline(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.GREEN
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Block Outline", "Highlight matching blocks around the next waypoint", accent,
            { OrderedWaypointManager.isBlockOutlineAroundWaypoint() }) { OrderedWaypointManager.setBlockOutlineAroundWaypoint(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Outline Fill", "Tint the outlined blocks as well as edging them", accent,
            { OrderedWaypointManager.isBlockOutlineFill() }) { OrderedWaypointManager.setBlockOutlineFill(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Outline Thickness", 0.5f, 9f, 0.5f,
            OrderedWaypointManager.getBlockOutlineThickness(), accent, { String.format("%.1f", it) }) {
            OrderedWaypointManager.setBlockOutlineThickness(it)
        }
        return y
    }

    fun waypointColors(w: Rectangle, width: Float): Float {
        var y = 0f
        y = SettingsUi.inlineColor(w, width, y, "Current Waypoint",
            { OrderedWaypointManager.getCurrentWaypointColor() },
            { r, g, b -> OrderedWaypointManager.setCurrentWaypointColor(r, g, b) },
            { OrderedWaypointManager.getCurrentWaypointAlpha() },
            { OrderedWaypointManager.setCurrentWaypointAlpha(it) })
        y = SettingsUi.inlineColor(w, width, y, "Next Waypoints",
            { OrderedWaypointManager.getNextWaypointColor() },
            { r, g, b -> OrderedWaypointManager.setNextWaypointColor(r, g, b) },
            { OrderedWaypointManager.getNextWaypointAlpha() },
            { OrderedWaypointManager.setNextWaypointAlpha(it) })
        y = SettingsUi.inlineColor(w, width, y, "Previous Waypoint",
            { OrderedWaypointManager.getPreviousWaypointColor() },
            { r, g, b -> OrderedWaypointManager.setPreviousWaypointColor(r, g, b) },
            { OrderedWaypointManager.getPreviousWaypointAlpha() },
            { OrderedWaypointManager.setPreviousWaypointAlpha(it) })
        y = SettingsUi.inlineColor(w, width, y, "Trace Line",
            { OrderedWaypointManager.getTraceLineColor() },
            { r, g, b -> OrderedWaypointManager.setTraceLineColor(r, g, b) },
            { OrderedWaypointManager.getTraceLineAlpha() },
            { OrderedWaypointManager.setTraceLineAlpha(it) })
        return y
    }

    fun shaftEsp(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.CYAN
        var y = 0f
        y = SettingsUi.inlineDropdown(w, width, y, "Render Mode", "Choose boxes, Prisma glow, or Minecraft glow", accent,
            EntityEspMode.values().map { it.displayName }, ShaftESP.getRenderMode().ordinal) {
            ShaftESP.setRenderMode(EntityEspMode.values()[it])
        }
        y = SettingsUi.inlineToggle(w, width, y, "Littlefoot ESP", "Highlight the Littlefoot in mineshafts", accent,
            { ShaftESP.isLittlefootEnabled() }) { ShaftESP.setLittlefootEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Littlefoot Tracer", "Line from your crosshair to the Littlefoot", accent,
            { ShaftESP.isLittlefootTracer() }) { ShaftESP.setLittlefootTracer(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Mob ESP", "Highlight mineshaft mobs", accent,
            { ShaftESP.isMobsEnabled() }) { ShaftESP.setMobsEnabled(it) }
        for (row in ExtraEspRows.shaft) {
            y = SettingsUi.inlineToggle(w, width, y, row.title, row.description, accent, row.get, row.set)
        }
        y = SettingsUi.inlineColor(w, width, y, "Mob ESP Color",
            { ShaftESP.getMobColor() },
            { r, g, b -> ShaftESP.setMobColor(r, g, b) },
            { ShaftESP.getMobAlpha() },
            { ShaftESP.setMobAlpha(it) })
        return y
    }

    fun corpseEsp(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.YELLOW
        var y = 0f
        y = SettingsUi.inlineDropdown(w, width, y, "Render Mode", "Choose boxes, Prisma glow, or Minecraft glow", accent,
            EntityEspMode.values().map { it.displayName }, CorpseESP.getRenderMode().ordinal) {
            CorpseESP.setRenderMode(EntityEspMode.values()[it])
        }
        y = SettingsUi.inlineToggle(w, width, y, "Lapis Corpses", "Track Lapis armor corpses", accent,
            { CorpseESP.isLapisEnabled() }) { if (CorpseESP.isLapisEnabled() != it) CorpseESP.toggleLapis() }
        y = SettingsUi.inlineToggle(w, width, y, "Tungsten Corpses", "Track Tungsten armor corpses", accent,
            { CorpseESP.isTungstenEnabled() }) { if (CorpseESP.isTungstenEnabled() != it) CorpseESP.toggleTungsten() }
        y = SettingsUi.inlineToggle(w, width, y, "Umber Corpses", "Track Umber armor corpses", accent,
            { CorpseESP.isUmberEnabled() }) { if (CorpseESP.isUmberEnabled() != it) CorpseESP.toggleUmber() }
        y = SettingsUi.inlineToggle(w, width, y, "Vanguard Corpses", "Track Vanguard armor corpses", accent,
            { CorpseESP.isVanguardEnabled() }) { if (CorpseESP.isVanguardEnabled() != it) CorpseESP.toggleVanguard() }
        for (row in ExtraEspRows.corpse) {
            y = SettingsUi.inlineToggle(w, width, y, row.title, row.description, accent, row.get, row.set)
        }
        return y
    }

    /** The three states the pickaxe HUD can print, for the colour picker's live preview. */
    private fun pickaxePreviewRows(): List<HudRow> {
        fun line(label: String, labelColor: FloatArray, value: String, valueColor: FloatArray) =
            if (PickaxeCooldownHUD.isCooldownOnly()) {
                listOf(HudSegment(value, SettingsUi.rgbOf(valueColor)))
            } else {
                listOf(
                    HudSegment("$label: ", SettingsUi.rgbOf(labelColor)),
                    HudSegment(value, SettingsUi.rgbOf(valueColor))
                )
            }
        return listOf(
            HudRow("ON COOLDOWN", line("Pickobulus",
                PickaxeCooldownHUD.getCooldownLabelColor(), "30s",
                PickaxeCooldownHUD.getCooldownValueColor())),
            HudRow("ABILITY ACTIVE", line("Mining Speed Boost",
                PickaxeCooldownHUD.getActiveLabelColor(), "12s",
                PickaxeCooldownHUD.getActiveValueColor())),
            HudRow("READY", line("Pickobulus",
                PickaxeCooldownHUD.getReadyLabelColor(), "\u2714 Ready",
                PickaxeCooldownHUD.getReadyValueColor()))
        )
    }

    private fun rollingPreviewRows(): List<HudRow> {
        fun line(value: String, labelColor: FloatArray, valueColor: FloatArray) = listOf(
            HudSegment("Rolling Miner: ", SettingsUi.rgbOf(labelColor)),
            HudSegment(value, SettingsUi.rgbOf(valueColor))
        )
        return listOf(
            HudRow("ON COOLDOWN", line("12s",
                RollingMinerCooldown.getCooldownLabelColor(),
                RollingMinerCooldown.getCooldownValueColor())),
            HudRow("READY", line("\u2714 Ready",
                RollingMinerCooldown.getReadyLabelColor(),
                RollingMinerCooldown.getReadyValueColor()))
        )
    }

    fun pickaxeCooldown(host: VexelMainScreen, w: Rectangle, width: Float): Float {
        val accent = SettingsUi.PURPLE
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Show the cooldown HUD", accent,
            { PickaxeCooldownHUD.isEnabled() }) { PickaxeCooldownHUD.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Cooldown Only", "Hide the ability name and show just the timer", accent,
            { PickaxeCooldownHUD.isCooldownOnly() }) { PickaxeCooldownHUD.setCooldownOnly(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Custom Cooldown", "Use a local timer instead of reading the tab list", accent,
            { PickaxeCooldownHUD.isCustomCooldownEnabled() }) { PickaxeCooldownHUD.setCustomCooldownEnabled(it) }
        y = SettingsUi.inlineTextInput(w, width, y, "Custom Cooldown Seconds", "Used when Custom Cooldown is enabled",
            PickaxeCooldownHUD.getCustomCooldownSeconds().toString()) { value ->
            value.trim().toIntOrNull()?.let { PickaxeCooldownHUD.setCustomCooldownSeconds(it) }
        }
        y = SettingsUi.inlineSlider(w, width, y, "Scale", 0.5f, 2.0f, 0.05f,
            PickaxeCooldownHUD.getScale(), accent, { String.format("%.2fx", it) }) {
            PickaxeCooldownHUD.setScale(it)
        }
        y = SettingsUi.inlineRgb(w, width, y, "Cooldown Label Color",
            getColor = { PickaxeCooldownHUD.getCooldownLabelColor() },
            setColor = { r, g, b -> PickaxeCooldownHUD.setCooldownLabelColor(r, g, b) },
            previewRowCount = 3, preview = ::pickaxePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Cooldown Value Color",
            getColor = { PickaxeCooldownHUD.getCooldownValueColor() },
            setColor = { r, g, b -> PickaxeCooldownHUD.setCooldownValueColor(r, g, b) },
            previewRowCount = 3, preview = ::pickaxePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Ready Label Color",
            getColor = { PickaxeCooldownHUD.getReadyLabelColor() },
            setColor = { r, g, b -> PickaxeCooldownHUD.setReadyLabelColor(r, g, b) },
            previewRowCount = 3, preview = ::pickaxePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Ready Value Color",
            getColor = { PickaxeCooldownHUD.getReadyValueColor() },
            setColor = { r, g, b -> PickaxeCooldownHUD.setReadyValueColor(r, g, b) },
            previewRowCount = 3, preview = ::pickaxePreviewRows)
        y = SettingsUi.inlineSectionHeader(w, y, "Ability Active")
        y = SettingsUi.inlineToggle(w, width, y, "Ability Active Timer",
            "Count down how long the used ability stays active", accent,
            { PickaxeCooldownHUD.isActiveTimerEnabled() }) { PickaxeCooldownHUD.setActiveTimerEnabled(it) }
        y = SettingsUi.inlineRgb(w, width, y, "Active Label Color",
            getColor = { PickaxeCooldownHUD.getActiveLabelColor() },
            setColor = { r, g, b -> PickaxeCooldownHUD.setActiveLabelColor(r, g, b) },
            previewRowCount = 3, preview = ::pickaxePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Active Value Color",
            getColor = { PickaxeCooldownHUD.getActiveValueColor() },
            setColor = { r, g, b -> PickaxeCooldownHUD.setActiveValueColor(r, g, b) },
            previewRowCount = 3, preview = ::pickaxePreviewRows)
        y = SettingsUi.inlineToggle(w, width, y, "Ready Title", "Flash a title when the ability comes off cooldown", accent,
            { PickaxeCooldownHUD.isTitleEnabled() }) { PickaxeCooldownHUD.setTitleEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Title Threshold", 0f, 30f, 1f,
            PickaxeCooldownHUD.getTitleThreshold().toFloat(), accent, { "${it.toInt()}s left" }) {
            PickaxeCooldownHUD.setTitleThreshold(it.toInt())
        }
        return y
    }

    private fun forgePreviewRows(): List<HudRow> = listOf(
        HudRow("HEADER", listOf(HudSegment("Forges:", SettingsUi.rgbOf(ForgeDisplay.getTitleColor())))),
        HudRow("RUNNING", listOf(
            HudSegment("1) ", SettingsUi.rgbOf(ForgeDisplay.getTimeColor())),
            HudSegment("Refined Tungsten", SettingsUi.rgbOf(ForgeDisplay.getItemColor())),
            HudSegment(": 59m", SettingsUi.rgbOf(ForgeDisplay.getTimeColor()))
        )),
        HudRow("READY", listOf(
            HudSegment("2) ", SettingsUi.rgbOf(ForgeDisplay.getTimeColor())),
            HudSegment("Refined Diamond", SettingsUi.rgbOf(ForgeDisplay.getReadyColor())),
            HudSegment(": Ready!", SettingsUi.rgbOf(ForgeDisplay.getReadyColor()))
        ))
    )

    fun forgeDisplay(host: VexelMainScreen, w: Rectangle, width: Float): Float {
        val accent = SettingsUi.ORANGE
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Show forge slots read off the tab list", accent,
            { ForgeDisplay.isEnabled() }) { ForgeDisplay.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Sort By Time Left",
            "Soonest first; off keeps the slot order", accent,
            { ForgeDisplay.isSortByTime() }) { ForgeDisplay.setSortByTime(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Show Empty Slots", "List slots with nothing forging", accent,
            { ForgeDisplay.isShowEmpty() }) { ForgeDisplay.setShowEmpty(it) }
        y = SettingsUi.inlineLink(w, width, y, "Move HUD", "Position it with the other HUDs") {
            val here = Minecraft.getInstance().screen
            Minecraft.getInstance().schedule { Minecraft.getInstance().setScreen(HudPositionScreen(here)) }
        }
        y = SettingsUi.inlineRgb(w, width, y, "Header Color",
            getColor = { ForgeDisplay.getTitleColor() },
            setColor = { r, g, b -> ForgeDisplay.setTitleColor(r, g, b) },
            previewRowCount = 3, preview = ::forgePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Item Color",
            getColor = { ForgeDisplay.getItemColor() },
            setColor = { r, g, b -> ForgeDisplay.setItemColor(r, g, b) },
            previewRowCount = 3, preview = ::forgePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Time Color",
            getColor = { ForgeDisplay.getTimeColor() },
            setColor = { r, g, b -> ForgeDisplay.setTimeColor(r, g, b) },
            previewRowCount = 3, preview = ::forgePreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Ready Color",
            getColor = { ForgeDisplay.getReadyColor() },
            setColor = { r, g, b -> ForgeDisplay.setReadyColor(r, g, b) },
            previewRowCount = 3, preview = ::forgePreviewRows)
        return y
    }

    fun mineshaftAutoParty(host: VexelMainScreen, w: Rectangle, width: Float): Float {
        val accent = SettingsUi.PURPLE2
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled",
            "Party and warp signed-up players when their shaft spawns", accent,
            { MineshaftAutoParty.isEnabled() }) { MineshaftAutoParty.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Disband After Warp",
            "Disband 2s after warping so the next shaft starts from a clean party", accent,
            { MineshaftAutoParty.isDisbandAfterWarp() }) { MineshaftAutoParty.setDisbandAfterWarp(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Disband Timeout",
            MineshaftAutoParty.MIN_DISBAND_SECONDS.toFloat(),
            MineshaftAutoParty.MAX_DISBAND_SECONDS.toFloat(), 1f,
            MineshaftAutoParty.getDisbandSeconds().toFloat(), accent, { "${it.toInt()}s" }) {
            MineshaftAutoParty.setDisbandSeconds(it.toInt())
        }
        y = SettingsUi.inlineToggle(w, width, y, "Auto Accept Invites",
            "Accept party invites from the auto-accept list", accent,
            { PartyAutoAccept.isEnabled() }) { PartyAutoAccept.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Not During Ability",
            "Ignore invites while a pickaxe ability is still running", accent,
            { PartyAutoAccept.isBlockDuringAbility() }) { PartyAutoAccept.setBlockDuringAbility(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Not While In A Shaft",
            "Ignore invites until you have left the mineshaft you are in", accent,
            { PartyAutoAccept.isBlockInShaft() }) { PartyAutoAccept.setBlockInShaft(it) }
        y = SettingsUi.inlineLink(w, width, y, "Edit Sign-ups",
            "${MineshaftAutoParty.players().size} players, ${MineshaftAutoParty.activeSignupCount()} with shafts picked") {
            // Scheduled: we are mid click-dispatch, and the settings screen is still open.
            val here = Minecraft.getInstance().screen
            Minecraft.getInstance().schedule {
                Minecraft.getInstance().setScreen(MineshaftAutoPartyScreen(here))
            }
        }
        return y
    }

    fun misc(w: Rectangle, width: Float): Float {
        val accent = SettingsUi.ORANGE
        var y = 0f
        y = SettingsUi.inlineSlider(w, width, y, "GUI Opacity", 0.3f, 1.0f, 0.05f,
            SettingsUi.guiOpacity, accent, { "${(it * 100).toInt()}%" }) {
            SettingsUi.guiOpacity = it
        }
        y = SettingsUi.inlineToggle(w, width, y, "Mod Chat Messages", "Status chatter in chat; command output never hidden", accent,
            { MqoChat.isLogsEnabled() }) { MqoChat.setLogsEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Crit Particle Drop", "Sneaking lowers crit particles to the registered spot", accent,
            { CritParticleDrop.isEnabled() }) { CritParticleDrop.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Remove Corpse Ding Sound", "Mutes Hypixel's corpse note-block ding", accent,
            { SoundBlocker.isCorpseDingBlocked() }) { SoundBlocker.setCorpseDingBlocked(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Auto-skip /sho load", "Runs /sho skipto 1 after /sho load", accent,
            { MiningqolClient.getConfig()?.autoSkipShoLoad ?: false }) { MiningqolClient.getConfig()?.autoSkipShoLoad = it }
        y = SettingsUi.inlineToggle(w, width, y, "Filet O' Fortune Warning", "Warn when your Filet O' Fortune cake expires", accent,
            { FiletWarning.isEnabled() }) { FiletWarning.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Rolling Miner Cooldown", "Show a 20-second HUD timer after double drops", accent,
            { RollingMinerCooldown.isEnabled() }) { RollingMinerCooldown.setEnabled(it) }
        y = SettingsUi.inlineRgb(w, width, y, "Rolling Cooldown Label Color",
            getColor = { RollingMinerCooldown.getCooldownLabelColor() },
            setColor = { r, g, b -> RollingMinerCooldown.setCooldownLabelColor(r, g, b) },
            previewRowCount = 2, preview = ::rollingPreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Rolling Cooldown Value Color",
            getColor = { RollingMinerCooldown.getCooldownValueColor() },
            setColor = { r, g, b -> RollingMinerCooldown.setCooldownValueColor(r, g, b) },
            previewRowCount = 2, preview = ::rollingPreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Rolling Ready Label Color",
            getColor = { RollingMinerCooldown.getReadyLabelColor() },
            setColor = { r, g, b -> RollingMinerCooldown.setReadyLabelColor(r, g, b) },
            previewRowCount = 2, preview = ::rollingPreviewRows)
        y = SettingsUi.inlineRgb(w, width, y, "Rolling Ready Value Color",
            getColor = { RollingMinerCooldown.getReadyValueColor() },
            setColor = { r, g, b -> RollingMinerCooldown.setReadyValueColor(r, g, b) },
            previewRowCount = 2, preview = ::rollingPreviewRows)
        y = SettingsUi.inlineToggle(w, width, y, "Efficient Miner Overlay", "Heatmap the best clay / red sandstone (Glacite)", accent,
            { EfficientMinerOverlay.isEnabled() }) { EfficientMinerOverlay.setEnabled(it) }
        y = SettingsUi.inlineToggle(w, width, y, "Old Heatmap Colors", "Use the legacy 8-colour heatmap palette", accent,
            { EfficientMinerOverlay.isUsingOldHeatmap() }) { EfficientMinerOverlay.setUseOldHeatmap(it) }
        for (row in ExtraMiscRows.toggles) {
            y = SettingsUi.inlineToggle(w, width, y, row.title, row.description, accent, row.get, row.set)
        }
        return y
    }
}
