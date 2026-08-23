package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.BlockOverlay
import forfun.miningqol.client.waypoints.OrderedWaypointManager
import forfun.miningqol.client.CommissionHUD
import forfun.miningqol.client.ForgeDisplay
import forfun.miningqol.client.PickaxeCooldownHUD
import forfun.miningqol.client.party.MineshaftAutoParty
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW
import xyz.meowing.knit.api.input.KnitKeyboard
import xyz.meowing.vexel.Vexel.renderer
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.elements.TextInput
import kotlin.math.floor

/**
 * Main settings GUI — prisma-style: a floating sidebar panel (category nav) next
 * to a content panel with a searchable card grid; clicking a card swaps the grid
 * for an inline detail panel. Cheat-only categories are contributed through
 * ExtraCategories (registered by CheatGui, which only exists in the -cheat tree).
 */
class VexelMainScreen : VexelScreen("Sybau Settings") {

    /** Set by feature detail builders that need raw key input (keybind capture). */
    var keyHandler: ((KeyEvent) -> Boolean)? = null

    private var selectedCategory = 0
    private var openFeature: GuiFeature? = null
    private var searchQuery = ""

    private val sidebarWidth = 210f
    private val panelGap = 6f
    private val mainWidth = 844f
    private val panelHeight = 700f
    private val totalWidth = sidebarWidth + panelGap + mainWidth
    private val contentPad = 24f
    private val contentWidth = mainWidth - 2 * contentPad
    private val scrollbarGutter = 14f

    private var mainPanel: Rectangle? = null
    private var gridContainer: Rectangle? = null

    private fun builtInCategories(): List<GuiCategory> = listOf(
        GuiCategory("General", listOf(
            GuiFeature("Misc", "GUI opacity, chat, sounds, overlays", SettingsUi.ORANGE,
                detail = { _, w, width -> FeatureDetails.misc(w, width) }),
            GuiFeature("Block Overlay", "Custom targeted block highlight", SettingsUi.SKY,
                detail = { _, w, width -> FeatureDetails.blockOverlay(w, width) },
                status = { BlockOverlay.isEnabled() }),
            GuiFeature("Keybinds", "Rebind the mod's hotkeys", SettingsUi.SKY,
                detail = { host, w, width -> KeybindsContent.build(host, w, width) }),
            GuiFeature("Command Keybinds", "Bind commands to keys", SettingsUi.PURPLE2,
                detail = { host, w, width -> CommandKeybindContent.build(host, w, width) }),
            GuiFeature("Mineshaft Auto Party", "Warp players into the shafts they want", SettingsUi.PURPLE2,
                detail = { host, w, width -> FeatureDetails.mineshaftAutoParty(host, w, width) },
                status = { MineshaftAutoParty.isEnabled() })
        )),
        GuiCategory("HUDs", listOf(
            GuiFeature("Commission HUD", "On-screen commission tracker", SettingsUi.BLUE,
                detail = { host, w, width -> FeatureDetails.commissionHud(host, w, width) },
                status = { CommissionHUD.isEnabled() }),
            GuiFeature("Pickaxe Cooldown", "Ability cooldown HUD + ready alert", SettingsUi.PURPLE,
                detail = { host, w, width -> FeatureDetails.pickaxeCooldown(host, w, width) },
                status = { PickaxeCooldownHUD.isEnabled() }),
            GuiFeature("Forge Display", "Forge slots and times from the tab list", SettingsUi.ORANGE,
                detail = { host, w, width -> FeatureDetails.forgeDisplay(host, w, width) },
                status = { ForgeDisplay.isEnabled() })
        )),
        GuiCategory("Waypoints", listOf(
            GuiFeature("Ordered Waypoints", "Guided mining routes (/mqo)", SettingsUi.GREEN,
                detail = { host, w, width -> FeatureDetails.orderedWaypoints(host, w, width) },
                status = { OrderedWaypointManager.isEnabledRaw() }),
            GuiFeature("Block Check", "Lobby check + mined-out skipping", SettingsUi.GREEN,
                detail = { _, w, width -> FeatureDetails.waypointBlockCheck(w, width) },
                status = { OrderedWaypointManager.isLobbyCheckEnabled() }),
            GuiFeature("Block Outline", "Outline blocks around the next waypoint", SettingsUi.GREEN,
                detail = { _, w, width -> FeatureDetails.waypointOutline(w, width) },
                status = { OrderedWaypointManager.isBlockOutlineAroundWaypoint() }),
            GuiFeature("Waypoint Colors", "RGBA per waypoint type", SettingsUi.GREEN,
                detail = { _, w, width -> FeatureDetails.waypointColors(w, width) })
        )),
        GuiCategory("ESP", listOf(
            GuiFeature("Shaft ESP", "Littlefoot + mob highlights", SettingsUi.CYAN,
                detail = { _, w, width -> FeatureDetails.shaftEsp(w, width) }),
            GuiFeature("Corpse ESP", "Frozen corpse waypoints", SettingsUi.YELLOW,
                detail = { _, w, width -> FeatureDetails.corpseEsp(w, width) })
        ))
    )

    private fun categories(): List<GuiCategory> = builtInCategories() + ExtraCategories.categories

    override fun afterInitialization() {
        buildUi()
    }

    private fun buildUi() {
        val cats = categories()
        if (selectedCategory >= cats.size) selectedCategory = 0
        val root = ScaledUiRoot(uiScaleFor(totalWidth, panelHeight), totalWidth, panelHeight)
            .childOf(window)
        buildSidebar(root, cats)
        buildMain(root, cats[selectedCategory])
    }

    private fun buildSidebar(root: ScaledUiRoot, cats: List<GuiCategory>) {
        val sidebar = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.PANEL_BG),
            borderColor = SettingsUi.edge(SettingsUi.PANEL_BORDER),
            borderRadius = 16f,
            borderThickness = SettingsUi.EDGE_WIDTH
        )
            .setSizing(sidebarWidth, Size.Pixels, panelHeight, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(root)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xB0000000.toInt()
            }
        Text("Sybau", SettingsUi.TEXT_PRIMARY, 20f, true)
            .setPositioning(18f, Pos.ParentPixels, 22f, Pos.ParentPixels)
            .childOf(sidebar)

        val version = FabricLoader.getInstance().getModContainer("sybau")
            .map { "v" + it.metadata.version.friendlyString + " · 26.1.2" }
            .orElse("dev")
        Text(version, SettingsUi.TEXT_MUTED, 11f, false)
            .setPositioning(18f, Pos.ParentPixels, 48f, Pos.ParentPixels)
            .childOf(sidebar)

        var y = 84f
        cats.forEachIndexed { index, category ->
            val selected = index == selectedCategory && openFeature == null ||
                index == selectedCategory && openFeature != null
            val accent = category.features.firstOrNull()?.accent ?: SettingsUi.BLUE

            val item = Rectangle(
                backgroundColor = if (selected) SettingsUi.alpha(SettingsUi.NAV_SELECTED) else 0x00000000,
                borderColor = 0x00000000,
                borderRadius = 10f,
                borderThickness = 0f,
                hoverColor = if (selected) SettingsUi.alpha(SettingsUi.NAV_SELECTED)
                             else SettingsUi.alpha(SettingsUi.NAV_HOVER)
            )
                .setSizing(sidebarWidth - 20f, Size.Pixels, 36f, Size.Pixels)
                .setPositioning(10f, Pos.ParentPixels, y, Pos.ParentPixels)
                .childOf(sidebar)

            // Selection accent bar — shown/hidden via color so hover states can't re-show it
            Rectangle(backgroundColor = if (selected) accent else 0x00000000,
                borderColor = 0x00000000, borderRadius = 1.5f)
                .setSizing(3f, Size.Pixels, 18f, Size.Pixels)
                .setPositioning(0f, Pos.ParentPixels, 9f, Pos.ParentPixels)
                .ignoreMouseEvents()
                .childOf(item)

            val label = Text(category.name,
                if (selected) SettingsUi.TEXT_PRIMARY else SettingsUi.TEXT_MUTED, 14f, selected)
                .setPositioning(16f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .childOf(item)

            item.onClick { _ ->
                if (index != selectedCategory || openFeature != null) {
                    saveConfig()
                    selectedCategory = index
                    openFeature = null
                    scheduleRebuild()
                }
                true
            }
            item.onMouseEnter { if (!selected) label.textColor = SettingsUi.TEXT_SECONDARY }
            item.onMouseExit { if (!selected) label.textColor = SettingsUi.TEXT_MUTED }

            y += 40f
        }

        val moveHuds = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.CARD_BG),
            borderColor = 0xFFFFFFFF.toInt(),
            borderRadius = 9f,
            borderThickness = SettingsUi.EDGE_WIDTH,
            hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
        )
            .setSizing(sidebarWidth - 20f, Size.Pixels, 34f, Size.Pixels)
            .setPositioning(10f, Pos.ParentPixels, -42f, Pos.ParentPixels)
            .alignBottom()
            .childOf(sidebar)
        Text("Move HUDs", SettingsUi.TEXT_SECONDARY, 13f, true)
            .setPositioning(16f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(moveHuds)
        moveHuds.onClick { _ ->
            saveConfig()
            Minecraft.getInstance().setScreen(HudPositionScreen(this))
            true
        }
    }

    private fun buildMain(root: ScaledUiRoot, category: GuiCategory) {
        val panel = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.PANEL_BG),
            borderColor = SettingsUi.edge(SettingsUi.PANEL_BORDER),
            borderRadius = 16f,
            borderThickness = SettingsUi.EDGE_WIDTH
        )
            .setSizing(mainWidth, Size.Pixels, panelHeight, Size.Pixels)
            .setPositioning(sidebarWidth + panelGap, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(root)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xB0000000.toInt()
            }
        mainPanel = panel

        val feature = openFeature
        if (feature == null) {
            Text(category.name, SettingsUi.TEXT_PRIMARY, 24f, true)
                .setPositioning(contentPad, Pos.ParentPixels, 24f, Pos.ParentPixels)
                .childOf(panel)

            // Fill everything between the category title and the right edge.
            // 24f bold averages ~13.5px per glyph — close enough to hug the title.
            val searchX = contentPad + category.name.length * 13.5f + 24f
            val search = TextInput(initialValue = searchQuery, placeholder = "Search...", fontSize = 13f)
                .setSizing(mainWidth - searchX - contentPad, Size.Pixels, 30f, Size.Pixels)
                .setPositioning(searchX, Pos.ParentPixels, 26f, Pos.ParentPixels)
                .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
                .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
                .borderRadius(8f)
                .borderThickness(SettingsUi.EDGE_WIDTH)
                .childOf(panel)
            search.onValueChange { value ->
                searchQuery = value as String
                // Rebuild only the grid so the search box keeps focus; deferred a tick
                // so the element tree isn't mutated inside this input's event dispatch.
                Minecraft.getInstance().schedule(Runnable { rebuildGrid(categories()[selectedCategory]) })
            }

            buildGrid(panel, category)
        } else {
            buildDetail(panel, feature)
        }
    }

    private fun buildGrid(panel: Rectangle, category: GuiCategory) {
        val container = Rectangle(
            backgroundColor = 0x00000000, borderColor = 0x00000000, scrollable = true
        )
            .setSizing(contentWidth, Size.Pixels, panelHeight - 72f - 20f, Size.Pixels)
            .setPositioning(contentPad, Pos.ParentPixels, 72f, Pos.ParentPixels)
            .childOf(panel)
        gridContainer = container

        val query = searchQuery.trim()
        val features = if (query.isEmpty()) category.features
            else category.features.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }

        if (features.isEmpty()) {
            Text("No features match \"$query\"", SettingsUi.TEXT_MUTED, 14f, false)
                .setPositioning(4f, Pos.ParentPixels, 8f, Pos.ParentPixels)
                .childOf(container)
            return
        }

        val gap = 14f
        val gridContentWidth = contentWidth - scrollbarGutter
        val cardWidth = floor((gridContentWidth - 2 * gap) / 3f)
        val cardHeight = 96f

        features.forEachIndexed { index, feature ->
            val col = index % 3
            val row = index / 3
            val card = Rectangle(
                backgroundColor = SettingsUi.alpha(SettingsUi.CARD_BG),
                borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
                borderRadius = 12f,
                borderThickness = SettingsUi.EDGE_WIDTH,
                hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
            )
                .setSizing(cardWidth, Size.Pixels, cardHeight, Size.Pixels)
                .setPositioning(col * (cardWidth + gap), Pos.ParentPixels, row * (cardHeight + gap), Pos.ParentPixels)
                .childOf(container)

            Text(feature.title, SettingsUi.TEXT_PRIMARY, 15f, true)
                .setPositioning(14f, Pos.ParentPixels, 14f, Pos.ParentPixels)
                .childOf(card)

            Text(feature.description, SettingsUi.TEXT_MUTED, 11f, false)
                .setPositioning(14f, Pos.ParentPixels, 38f, Pos.ParentPixels)
                .childOf(card)

            val status = feature.status
            if (status != null) {
                val on = status()
                Rectangle(backgroundColor = if (on) feature.accent else SettingsUi.TEXT_DIM,
                    borderColor = 0x00000000, borderRadius = 3f)
                    .setSizing(6f, Size.Pixels, 6f, Size.Pixels)
                    .setPositioning(14f, Pos.ParentPixels, cardHeight - 26f, Pos.ParentPixels)
                    .ignoreMouseEvents()
                    .childOf(card)
                Text(if (on) "ON" else "OFF", if (on) feature.accent else SettingsUi.TEXT_DIM, 11f, true)
                    .setPositioning(26f, Pos.ParentPixels, cardHeight - 30f, Pos.ParentPixels)
                    .childOf(card)
            }
            if (feature.open != null) {
                Text("↗", SettingsUi.TEXT_DIM, 14f, false)
                    .setPositioning(-14f, Pos.ParentPixels, 12f, Pos.ParentPixels)
                    .alignRight()
                    .childOf(card)
            }

            card.onClick { _ ->
                if (feature.open != null) {
                    feature.open.invoke()
                } else if (feature.detail != null) {
                    openFeature = feature
                    scheduleRebuild()
                }
                true
            }
        }
    }

    private fun rebuildGrid(category: GuiCategory) {
        val panel = mainPanel ?: return
        gridContainer?.let { panel.children.remove(it); it.destroy() }
        buildGrid(panel, category)
    }

    private fun buildDetail(panel: Rectangle, feature: GuiFeature) {
        val back = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.CARD_BG),
            borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
            borderRadius = 9f,
            borderThickness = SettingsUi.EDGE_WIDTH,
            hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
        )
            .setSizing(72f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(contentPad, Pos.ParentPixels, 26f, Pos.ParentPixels)
            .childOf(panel)
        Text("‹ Back", SettingsUi.TEXT_SECONDARY, 13f, false)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(back)
        back.onClick { _ ->
            closeDetail()
            true
        }

        Text(feature.title, SettingsUi.TEXT_PRIMARY, 22f, true)
            .setPositioning(contentPad, Pos.ParentPixels, 70f, Pos.ParentPixels)
            .childOf(panel)
        Rectangle(backgroundColor = feature.accent, borderColor = 0x00000000, borderRadius = 1.5f)
            .setSizing(36f, Size.Pixels, 3f, Size.Pixels)
            .setPositioning(contentPad, Pos.ParentPixels, 98f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(panel)
        Text(feature.description, SettingsUi.TEXT_MUTED, 12f, false)
            .setPositioning(contentPad + 50f, Pos.ParentPixels, 96f, Pos.ParentPixels)
            .childOf(panel)

        val scroll = Rectangle(
            backgroundColor = 0x00000000, borderColor = 0x00000000, scrollable = true
        )
            .setSizing(contentWidth, Size.Pixels, panelHeight - 116f - 20f, Size.Pixels)
            .setPositioning(contentPad, Pos.ParentPixels, 116f, Pos.ParentPixels)
            .childOf(panel)

        val wrapper = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000)
            .setSizing(100f, Size.Percent, 100f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scroll)

        val finalY = feature.detail?.invoke(this, wrapper, contentWidth - scrollbarGutter) ?: 0f
        wrapper.setSizing(100f, Size.Percent, finalY + 8f, Size.Pixels)
    }

    private fun closeDetail() {
        saveConfig()
        openFeature = null
        scheduleRebuild()
    }

    /** Rebuilds the whole GUI, keeping the open feature — for detail panels whose height changed. */
    fun refreshDetail() {
        scheduleRebuild()
    }

    private fun scheduleRebuild() {
        keyHandler = null
        ColorEditor.forget()
        // Deferred a tick: KnitScreen won't re-run afterInitialization on setScreen(this),
        // and tearing the element tree down inside a click dispatch is unsafe.
        Minecraft.getInstance().schedule(Runnable {
            window.cleanup()
            buildUi()
        })
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && ColorEditor.close()) return true
        keyHandler?.let { if (it(input)) return true }
        focusedTextInput()?.let { textInput ->
            val shortcutDown = KnitKeyboard.isCtrlKeyPressed || KnitKeyboard.isSuperKeyPressed
            if (shortcutDown) {
                when (input.key()) {
                    GLFW.GLFW_KEY_A -> textInput.selectAll()
                    GLFW.GLFW_KEY_C -> textInput.copySelection()
                    GLFW.GLFW_KEY_V -> textInput.paste()
                    GLFW.GLFW_KEY_X -> textInput.cutSelection()
                    else -> if (!textInput.keyTyped(input.key(), input.scancode(), '\u0000')) {
                        return@let
                    }
                }
                return true
            }
            if (textInput.keyTyped(input.key(), input.scancode(), '\u0000')) return true
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && openFeature != null) {
            closeDetail()
            return true
        }
        return super.keyPressed(input)
    }

    private fun focusedTextInput(): TextInput? = findFocusedTextInput(window.children)

    private fun findFocusedTextInput(elements: List<VexelElement<*>>): TextInput? {
        for (element in elements.asReversed()) {
            if (element is TextInput && element.isFocused) return element
            findFocusedTextInput(element.children)?.let { return it }
        }
        return null
    }

    override fun onClose() {
        keyHandler = null
        ColorEditor.forget()
        saveConfig()
        super.onClose()
    }

    private fun saveConfig() {
        MiningqolClient.getConfig()?.loadFromGame()
        MiningqolClient.getConfig()?.save()
    }
}
