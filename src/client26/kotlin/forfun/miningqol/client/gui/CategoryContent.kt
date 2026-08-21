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
import forfun.miningqol.client.ShaftESP
import forfun.miningqol.client.SoundBlocker
import forfun.miningqol.client.waypoints.OrderedWaypointManager
import net.minecraft.client.Minecraft
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
        y = SettingsUi.inlineRgb(w, width, y, "Outline Color",
            { BlockOverlay.getOutlineColor() },
            { r, g, b -> BlockOverlay.setOutlineColor(r, g, b) })
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
        y = SettingsUi.inlineToggle(w, width, y, "Commission Stats", "Total completed + comms/hour (/commtrack reset)", accent,
            { CommTracker.isStatsEnabled() }) { CommTracker.setStatsEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Scale", 0.5f, 2.0f, 0.05f,
            CommissionHUD.getScale(), accent, { String.format("%.2fx", it) }) {
            CommissionHUD.setScale(it)
        }
        y = SettingsUi.inlineLink(w, width, y, "Move HUD", "Drag the panel, scroll to resize") {
            Minecraft.getInstance().setScreen(CommissionHudPositionScreen(host))
        }
        y = SettingsUi.inlineLink(w, width, y, "Move Stats HUD", "The comms-completed panel is placed separately") {
            Minecraft.getInstance().setScreen(CommStatsHudPositionScreen(host))
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

    fun pickaxeCooldown(host: VexelMainScreen, w: Rectangle, width: Float): Float {
        val accent = SettingsUi.PURPLE
        var y = 0f
        y = SettingsUi.inlineToggle(w, width, y, "Enabled", "Show the cooldown HUD", accent,
            { PickaxeCooldownHUD.isEnabled() }) { PickaxeCooldownHUD.setEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Scale", 0.5f, 2.0f, 0.05f,
            PickaxeCooldownHUD.getScale(), accent, { String.format("%.2fx", it) }) {
            PickaxeCooldownHUD.setScale(it)
        }
        y = SettingsUi.inlineToggle(w, width, y, "Ready Title", "Flash a title when the ability comes off cooldown", accent,
            { PickaxeCooldownHUD.isTitleEnabled() }) { PickaxeCooldownHUD.setTitleEnabled(it) }
        y = SettingsUi.inlineSlider(w, width, y, "Title Threshold", 0f, 30f, 1f,
            PickaxeCooldownHUD.getTitleThreshold().toFloat(), accent, { "${it.toInt()}s left" }) {
            PickaxeCooldownHUD.setTitleThreshold(it.toInt())
        }
        y = SettingsUi.inlineLink(w, width, y, "Move HUD", "Drag the cooldown HUD wherever you want it") {
            Minecraft.getInstance().setScreen(PickaxeCooldownPositionScreen(host))
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
