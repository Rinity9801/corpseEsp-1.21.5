package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.party.CorpseType
import forfun.miningqol.client.party.MineshaftAutoParty
import forfun.miningqol.client.party.PartyAutoAccept
import forfun.miningqol.client.party.ShaftType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW
import xyz.meowing.knit.api.input.KnitKeyboard
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.base.enums.Pos
import xyz.meowing.vexel.components.base.enums.Size
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.TextInput

/**
 * Sign-up editor for the mineshaft auto-party: add a player on the left, then tick the
 * shafts they want on the right. Opened from the settings GUI's feature card.
 */
class MineshaftAutoPartyScreen(private val parent: Screen?) : VexelScreen("Mineshaft Auto Party") {

    private val totalWidth = 1060f
    private val totalHeight = 700f
    private val pad = 24f
    private val listWidth = 250f
    private val detailX = pad + listWidth + 16f
    private val detailWidth = totalWidth - detailX - pad
    private val contentY = 106f
    private val contentHeight = totalHeight - contentY - pad

    private val accent = SettingsUi.PURPLE

    private val playersHeight = 330f
    private val acceptY = contentY + playersHeight + 12f
    private val acceptHeight = totalHeight - acceptY - pad

    private var selected: String? = null
    private var pendingName = ""
    private var pendingAccept = ""

    // Held so Enter can tell the two name fields apart.
    private var playerInput: TextInput? = null
    private var acceptInput: TextInput? = null

    override fun afterInitialization() {
        if (selected == null) selected = MineshaftAutoParty.players().firstOrNull()
        buildUi()
    }

    private fun buildUi() {
        val root = ScaledUiRoot(uiScaleFor(totalWidth, totalHeight), totalWidth, totalHeight)
            .childOf(window)

        val panel = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.PANEL_BG),
            borderColor = SettingsUi.edge(SettingsUi.PANEL_BORDER),
            borderRadius = 16f,
            borderThickness = SettingsUi.EDGE_WIDTH
        )
            .setSizing(totalWidth, Size.Pixels, totalHeight, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(root)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xB0000000.toInt()
            }

        Text("Mineshaft Auto Party", SettingsUi.TEXT_PRIMARY, 20f, true)
            .setPositioning(0f, Pos.ParentCenter, 20f, Pos.ParentPixels)
            .childOf(panel)

        buildAddRow(panel)
        buildPlayerList(panel)
        buildAcceptList(panel)
        buildDetail(panel)
    }

    private fun buildAddRow(panel: Rectangle) {
        val input = TextInput(initialValue = pendingName, placeholder = "Add a player", fontSize = 13f)
            .setSizing(430f, Size.Pixels, 34f, Size.Pixels)
            .setPositioning(pad, Pos.ParentPixels, 56f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
            .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
            .borderRadius(9f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .childOf(panel)
        input.onValueChange { value -> pendingName = value as String }
        playerInput = input

        Button("Add", SettingsUi.TEXT_PRIMARY, fontSize = 13f)
            .setSizing(110f, Size.Pixels, 34f, Size.Pixels)
            .setPositioning(pad + 442f, Pos.ParentPixels, 56f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.tint(SettingsUi.GREEN, 0.14f))
            .borderColor(SettingsUi.edge(SettingsUi.GREEN, 0.65f))
            .borderRadius(9f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .hoverColors(SettingsUi.tint(SettingsUi.GREEN, 0.24f), SettingsUi.TEXT_PRIMARY)
            .onClick { _ ->
                addPending()
                true
            }
            .childOf(panel)

        Button("Done", SettingsUi.TEXT_PRIMARY, fontSize = 13f)
            .setSizing(120f, Size.Pixels, 34f, Size.Pixels)
            .setPositioning(-pad, Pos.ParentPixels, 56f, Pos.ParentPixels)
            .alignRight()
            .backgroundColor(SettingsUi.alpha(SettingsUi.CARD_BG))
            .borderColor(SettingsUi.edge(accent, 0.8f))
            .borderRadius(9f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .hoverColors(SettingsUi.alpha(SettingsUi.CARD_HOVER), SettingsUi.TEXT_PRIMARY)
            .onClick { _ ->
                close()
                true
            }
            .childOf(panel)
    }

    private fun buildPlayerList(panel: Rectangle) {
        val column = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.CARD_BG),
            borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
            borderRadius = 12f,
            borderThickness = SettingsUi.EDGE_WIDTH
        )
            .setSizing(listWidth, Size.Pixels, playersHeight, Size.Pixels)
            .setPositioning(pad, Pos.ParentPixels, contentY, Pos.ParentPixels)
            .childOf(panel)

        Text("Players", SettingsUi.TEXT_SECONDARY, 14f, true)
            .setPositioning(0f, Pos.ParentCenter, 14f, Pos.ParentPixels)
            .childOf(column)

        val scroll = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000, scrollable = true)
            .setSizing(listWidth - 20f, Size.Pixels, playersHeight - 48f, Size.Pixels)
            .setPositioning(10f, Pos.ParentPixels, 40f, Pos.ParentPixels)
            .childOf(column)

        val players = MineshaftAutoParty.players()
        if (players.isEmpty()) {
            Text("No players yet", SettingsUi.TEXT_DIM, 12f, false)
                .setPositioning(10f, Pos.ParentPixels, 8f, Pos.ParentPixels)
                .childOf(scroll)
            return
        }

        val wrapper = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000)
            .setSizing(100f, Size.Percent, players.size * 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scroll)

        players.forEachIndexed { index, name ->
            val isSelected = name == selected
            val count = MineshaftAutoParty.selectionCount(name)
            val row = Rectangle(
                backgroundColor = if (isSelected) SettingsUi.alpha(SettingsUi.NAV_SELECTED)
                                  else SettingsUi.alpha(SettingsUi.TRACK),
                borderColor = if (isSelected) SettingsUi.edge(accent) else SettingsUi.edge(SettingsUi.CARD_BORDER),
                borderRadius = 9f,
                borderThickness = SettingsUi.EDGE_WIDTH,
                hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
            )
                .setSizing(listWidth - 34f, Size.Pixels, 32f, Size.Pixels)
                .setPositioning(0f, Pos.ParentPixels, index * 40f, Pos.ParentPixels)
                .childOf(wrapper)

            Text(name, if (isSelected) SettingsUi.TEXT_PRIMARY else SettingsUi.TEXT_SECONDARY, 13f, false)
                .setPositioning(12f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .childOf(row)
            Text(
                if (count == 0) "none" else "$count",
                if (count == 0) SettingsUi.TEXT_DIM else accent, 11f, false
            )
                .setPositioning(-12f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .childOf(row)

            row.onClick { _ ->
                selected = name
                rebuild()
                true
            }
        }
    }

    private fun buildAcceptList(panel: Rectangle) {
        val card = Rectangle(
            backgroundColor = SettingsUi.alpha(SettingsUi.CARD_BG),
            borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
            borderRadius = 12f,
            borderThickness = SettingsUi.EDGE_WIDTH
        )
            .setSizing(listWidth, Size.Pixels, acceptHeight, Size.Pixels)
            .setPositioning(pad, Pos.ParentPixels, acceptY, Pos.ParentPixels)
            .childOf(panel)

        Text("Auto Accept", SettingsUi.TEXT_SECONDARY, 14f, true)
            .setPositioning(0f, Pos.ParentCenter, 12f, Pos.ParentPixels)
            .childOf(card)
        Text(
            if (PartyAutoAccept.isEnabled()) "ON" else "OFF",
            if (PartyAutoAccept.isEnabled()) SettingsUi.GREEN else SettingsUi.TEXT_DIM, 10f, false
        )
            .setPositioning(-12f, Pos.ParentPixels, 14f, Pos.ParentPixels)
            .alignRight()
            .childOf(card)

        val input = TextInput(initialValue = pendingAccept, placeholder = "Player name", fontSize = 12f)
            .setSizing(160f, Size.Pixels, 26f, Size.Pixels)
            .setPositioning(10f, Pos.ParentPixels, 36f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
            .borderColor(SettingsUi.edge(SettingsUi.CARD_BORDER))
            .borderRadius(8f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .childOf(card)
        input.onValueChange { value -> pendingAccept = value as String }
        acceptInput = input

        Button("Add", SettingsUi.TEXT_PRIMARY, fontSize = 11f)
            .setSizing(54f, Size.Pixels, 26f, Size.Pixels)
            .setPositioning(176f, Pos.ParentPixels, 36f, Pos.ParentPixels)
            .backgroundColor(SettingsUi.tint(SettingsUi.GREEN, 0.14f))
            .borderColor(SettingsUi.edge(SettingsUi.GREEN, 0.65f))
            .borderRadius(8f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .hoverColors(SettingsUi.tint(SettingsUi.GREEN, 0.24f), SettingsUi.TEXT_PRIMARY)
            .onClick { _ ->
                addPendingAccept()
                true
            }
            .childOf(card)

        val scroll = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000, scrollable = true)
            .setSizing(listWidth - 20f, Size.Pixels, acceptHeight - 82f, Size.Pixels)
            .setPositioning(10f, Pos.ParentPixels, 72f, Pos.ParentPixels)
            .childOf(card)

        val names = PartyAutoAccept.names()
        if (names.isEmpty()) {
            Text("Nobody yet", SettingsUi.TEXT_DIM, 11f, false)
                .setPositioning(4f, Pos.ParentPixels, 6f, Pos.ParentPixels)
                .childOf(scroll)
            return
        }

        val wrapper = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000)
            .setSizing(100f, Size.Percent, names.size * 30f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(scroll)

        names.forEachIndexed { index, name ->
            val row = Rectangle(
                backgroundColor = SettingsUi.alpha(SettingsUi.TRACK),
                borderColor = SettingsUi.edge(SettingsUi.CARD_BORDER),
                borderRadius = 8f,
                borderThickness = SettingsUi.EDGE_WIDTH
            )
                .setSizing(listWidth - 34f, Size.Pixels, 26f, Size.Pixels)
                .setPositioning(0f, Pos.ParentPixels, index * 30f, Pos.ParentPixels)
                .childOf(wrapper)

            Text(name, SettingsUi.TEXT_SECONDARY, 12f, false)
                .setPositioning(10f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .childOf(row)

            Button("x", SettingsUi.RED, fontSize = 12f)
                .setSizing(22f, Size.Pixels, 20f, Size.Pixels)
                .setPositioning(-4f, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .alignRight()
                .backgroundColor(0x00000000)
                .borderColor(0x00000000)
                .borderRadius(6f)
                .borderThickness(0f)
                .hoverColors(SettingsUi.alpha(SettingsUi.CARD_HOVER), SettingsUi.RED)
                .onClick { _ ->
                    PartyAutoAccept.remove(name)
                    save()
                    rebuild()
                    true
                }
                .childOf(row)
        }
    }

    private fun addPendingAccept() {
        if (PartyAutoAccept.add(pendingAccept) != null) {
            pendingAccept = ""
            save()
        }
        rebuild()
    }

    private fun buildDetail(panel: Rectangle) {
        val detail = Rectangle(backgroundColor = 0x00000000, borderColor = 0x00000000)
            .setSizing(detailWidth, Size.Pixels, contentHeight, Size.Pixels)
            .setPositioning(detailX, Pos.ParentPixels, contentY, Pos.ParentPixels)
            .childOf(panel)

        val name = selected
        if (name == null) {
            Text("Add a player above to start assigning shafts.", SettingsUi.TEXT_DIM, 13f, false)
                .setPositioning(2f, Pos.ParentPixels, 4f, Pos.ParentPixels)
                .childOf(detail)
            return
        }

        Text(name, SettingsUi.CYAN, 16f, true)
            .setPositioning(2f, Pos.ParentPixels, 2f, Pos.ParentPixels)
            .childOf(detail)

        Button("Delete Player", SettingsUi.RED, fontSize = 12f)
            .setSizing(130f, Size.Pixels, 30f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignRight()
            .backgroundColor(SettingsUi.alpha(SettingsUi.TRACK))
            .borderColor(SettingsUi.edge(SettingsUi.RED, 0.6f))
            .borderRadius(9f)
            .borderThickness(SettingsUi.EDGE_WIDTH)
            .hoverColors(SettingsUi.alpha(SettingsUi.CARD_HOVER), SettingsUi.RED)
            .onClick { _ ->
                MineshaftAutoParty.removePlayer(name)
                selected = MineshaftAutoParty.players().firstOrNull()
                save()
                rebuild()
                true
            }
            .childOf(detail)

        Text("Corpses", SettingsUi.CYAN, 13f, true)
            .setPositioning(2f, Pos.ParentPixels, 40f, Pos.ParentPixels)
            .childOf(detail)
        Text(
            "Unlooted corpses on the tab list. A pick is a floor: 2+ takes any shaft with two or more.",
            SettingsUi.TEXT_MUTED, 11f, false
        )
            .setPositioning(74f, Pos.ParentPixels, 42f, Pos.ParentPixels)
            .childOf(detail)

        val corpseTop = 62f
        val corpseCellWidth = 56f
        val corpseGap = 8f
        CorpseType.values().forEachIndexed { rowIndex, corpse ->
            val rowY = corpseTop + rowIndex * 34f
            Text(corpse.displayName(), SettingsUi.TEXT_SECONDARY, 12f, false)
                .setPositioning(2f, Pos.ParentPixels, rowY + 9f, Pos.ParentPixels)
                .childOf(detail)

            for (count in 1..CorpseType.MAX_COUNT) {
                val on = MineshaftAutoParty.isCorpseSelected(name, corpse, count)
                val cell = Rectangle(
                    backgroundColor = if (on) SettingsUi.tint(SettingsUi.SKY, 0.22f)
                                      else SettingsUi.alpha(SettingsUi.TRACK),
                    borderColor = if (on) SettingsUi.edge(SettingsUi.SKY, 0.9f)
                                  else SettingsUi.edge(SettingsUi.CARD_BORDER),
                    borderRadius = 8f,
                    borderThickness = SettingsUi.EDGE_WIDTH,
                    hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
                )
                    .setSizing(corpseCellWidth, Size.Pixels, 28f, Size.Pixels)
                    .setPositioning(100f + (count - 1) * (corpseCellWidth + corpseGap), Pos.ParentPixels,
                        rowY, Pos.ParentPixels)
                    .childOf(detail)

                Text("$count+", if (on) SettingsUi.TEXT_PRIMARY else SettingsUi.TEXT_SECONDARY, 12f, false)
                    .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
                    .childOf(cell)

                cell.onClick { _ ->
                    MineshaftAutoParty.toggleCorpse(name, corpse, count)
                    save()
                    rebuild()
                    true
                }
            }
        }

        val mobsY = corpseTop + CorpseType.values().size * 34f + 6f
        Text("Mobs", SettingsUi.CYAN, 13f, true)
            .setPositioning(2f, Pos.ParentPixels, mobsY + 8f, Pos.ParentPixels)
            .childOf(detail)

        val mobOn = MineshaftAutoParty.isLittlefootMob(name)
        val mobCell = Rectangle(
            backgroundColor = if (mobOn) SettingsUi.tint(SettingsUi.GREEN, 0.22f)
                              else SettingsUi.alpha(SettingsUi.TRACK),
            borderColor = if (mobOn) SettingsUi.edge(SettingsUi.GREEN, 0.9f)
                          else SettingsUi.edge(SettingsUi.CARD_BORDER),
            borderRadius = 8f,
            borderThickness = SettingsUi.EDGE_WIDTH,
            hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
        )
            .setSizing(148f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(100f, Pos.ParentPixels, mobsY, Pos.ParentPixels)
            .childOf(detail)
        Text("Littlefoot (ESP)", if (mobOn) SettingsUi.TEXT_PRIMARY else SettingsUi.TEXT_SECONDARY, 12f, false)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
            .childOf(mobCell)
        mobCell.onClick { _ ->
            MineshaftAutoParty.toggleLittlefootMob(name)
            save()
            rebuild()
            true
        }
        Text(
            "Any shaft where the ESP sees the mob. Needs Littlefoot ESP on.",
            SettingsUi.TEXT_MUTED, 11f, false
        )
            .setPositioning(258f, Pos.ParentPixels, mobsY + 9f, Pos.ParentPixels)
            .childOf(detail)

        val shaftLabelY = mobsY + 40f
        Text("Shaft Types", SettingsUi.CYAN, 13f, true)
            .setPositioning(2f, Pos.ParentPixels, shaftLabelY, Pos.ParentPixels)
            .childOf(detail)
        Text(
            "Any covers every mineshaft, which is what you want for a pickaxe ability reset.",
            SettingsUi.TEXT_MUTED, 11f, false
        )
            .setPositioning(96f, Pos.ParentPixels, shaftLabelY + 2f, Pos.ParentPixels)
            .childOf(detail)

        val columns = 5
        val gap = 9f
        val cellWidth = (detailWidth - (columns - 1) * gap) / columns
        val gridTop = shaftLabelY + 28f

        ShaftType.values().forEachIndexed { index, type ->
            val column = index % columns
            val row = index / columns
            val on = MineshaftAutoParty.isSelected(name, type)
            val highlight = if (type == ShaftType.ANY) SettingsUi.YELLOW else SettingsUi.GREEN

            val cell = Rectangle(
                backgroundColor = if (on) SettingsUi.tint(highlight, 0.22f) else SettingsUi.alpha(SettingsUi.TRACK),
                borderColor = if (on) SettingsUi.edge(highlight, 0.9f) else SettingsUi.edge(SettingsUi.CARD_BORDER),
                borderRadius = 8f,
                borderThickness = SettingsUi.EDGE_WIDTH,
                hoverColor = SettingsUi.alpha(SettingsUi.CARD_HOVER)
            )
                .setSizing(cellWidth, Size.Pixels, 32f, Size.Pixels)
                .setPositioning(column * (cellWidth + gap), Pos.ParentPixels, gridTop + row * 40f, Pos.ParentPixels)
                .childOf(detail)

            Text(
                type.displayName(),
                if (on) SettingsUi.TEXT_PRIMARY else SettingsUi.TEXT_SECONDARY, 12f, false
            )
                .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
                .childOf(cell)

            cell.onClick { _ ->
                MineshaftAutoParty.toggleType(name, type)
                save()
                rebuild()
                true
            }
        }
    }

    private fun addPending() {
        val added = MineshaftAutoParty.addPlayer(pendingName)
        if (added != null) {
            selected = added
            pendingName = ""
            save()
        }
        rebuild()
    }

    private fun rebuild() {
        // Deferred a tick, like VexelMainScreen: afterInitialization does not re-run on
        // setScreen, and tearing the element tree down inside a click dispatch is unsafe.
        playerInput = null
        acceptInput = null
        Minecraft.getInstance().schedule(Runnable {
            window.cleanup()
            buildUi()
        })
    }

    private fun save() {
        MiningqolClient.saveConfig()
    }

    private fun close() {
        save()
        val back = parent
        Minecraft.getInstance().schedule(Runnable { Minecraft.getInstance().setScreen(back) })
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        val focused = focusedTextInput()
        if (focused != null) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                if (focused === acceptInput) addPendingAccept() else addPending()
                return true
            }
            val shortcutDown = KnitKeyboard.isCtrlKeyPressed || KnitKeyboard.isSuperKeyPressed
            if (shortcutDown) {
                when (input.key()) {
                    GLFW.GLFW_KEY_A -> focused.selectAll()
                    GLFW.GLFW_KEY_C -> focused.copySelection()
                    GLFW.GLFW_KEY_V -> focused.paste()
                    GLFW.GLFW_KEY_X -> focused.cutSelection()
                    else -> if (!focused.keyTyped(input.key(), input.scancode(), ' ')) {
                        return super.keyPressed(input)
                    }
                }
                return true
            }
            if (focused.keyTyped(input.key(), input.scancode(), ' ')) return true
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close()
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
        save()
        super.onClose()
    }
}
