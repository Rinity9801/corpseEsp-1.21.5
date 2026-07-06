package forfun.miningqol.client.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.input.KeyInput
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*

class VexelMainScreen : VexelScreen("MiningQOL Settings") {
    private lateinit var overlay: Rectangle
    private lateinit var mainPanel: Rectangle
    private lateinit var searchInput: TextInput
    private val allCards = mutableListOf<Pair<Rectangle, CardInfo>>()

    data class CardInfo(
        val title: String,
        val description: String,
        val color: Int,
        val keywords: List<String>
    )

    override fun afterInitialization() {
        // Semi-transparent dark overlay background
        overlay = Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)
            .fadeIn(400, EasingType.EASE_OUT)

        // Main panel - darker and more modern
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(750f, Size.Pixels, 840f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        // Center the panel manually using ScreenPixels for reliable positioning
        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels

        // Calculate center position (must be done after setting size)
        mainPanel.xConstraint = (mainPanel.screenWidth - 750f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 840f) / 2f

        // Fade in animation
        mainPanel.fadeIn(500, EasingType.EASE_OUT)

        // Title bar background
        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 80f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }
            .fadeIn(600, EasingType.EASE_OUT)

        // Title
        Text("MiningQOL", 0xFFFFFFFF.toInt(), 38f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(700, EasingType.EASE_OUT)

        // Subtitle
        Text("Enhanced mining experience for Hypixel Skyblock", 0xFF888888.toInt(), 14f, false)
            .setPositioning(0f, Pos.ParentCenter, 55f, Pos.ParentPixels)
            .childOf(mainPanel)
            .fadeIn(800, EasingType.EASE_OUT)

        // Search bar
        searchInput = TextInput(
            "",
            "Search settings...",
            fontSize = 15f,
            textColor = 0xFFFFFFFF.toInt()
        )
            .setSizing(680f, Size.Pixels, 38f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 85f, Pos.ParentPixels)
            .backgroundColor(0xFF1E1E1E.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .childOf(mainPanel)
            .apply {
                onValueChange { query ->
                    filterCards((query as String).lowercase())
                }
            }

        searchInput.visible = false
        Thread {
            Thread.sleep(850L)
            MinecraftClient.getInstance().execute {
                searchInput.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()

        // Category cards container with grid layout
        val cardStartY = 140f
        val cardSpacing = 12f
        val cardHeight = 70f
        val leftX = 25f
        val rightX = 390f
        val cardWidth = 335f

        // Left column cards with searchable keywords
        val leftCards = mutableListOf(
            CardInfo(
                "Mining Profit",
                "Track earnings and optimize gains",
                0xFF5B7CFF.toInt(),
                listOf("profit", "tracker", "gemstone", "rough", "npc", "bazaar", "gem tier", "pristine", "chance", "earnings", "money", "coins")
            ),
            CardInfo(
                "Efficient Miner",
                "Overlay for max mining efficiency",
                0xFFFF7C5B.toInt(),
                listOf("efficient", "miner", "overlay", "heatmap", "beacon", "perk", "highlight", "players")
            ),
            CardInfo(
                "Shaft ESP",
                "Highlight mobs and corpses in Mineshafts",
                0xFF5BFF7C.toInt(),
                listOf("shaft", "esp", "littlefoot", "mobs", "corpse", "lapis", "tungsten", "umber", "vanguard", "mineshaft", "highlight", "bodies", "wolves")
            ),
            CardInfo(
                "Pickaxe Cooldown",
                "HUD for ability cooldown tracking",
                0xFF5BFFFF.toInt(),
                listOf("pickaxe", "cooldown", "ability", "hud", "display", "almost ready", "title", "threshold", "mining speed")
            ),
            CardInfo(
                "Ordered Waypoints",
                "Create and follow waypoint routes",
                0xFF55FF55.toInt(),
                listOf("ordered", "waypoints", "route", "path", "navigation", "mqo", "waypoint", "trace", "line")
            )
        )
        //? if isCheat {
        leftCards.add(CardInfo(
            "In Shaft Click",
            "Auto-click with cold-aware drill switching",
            0xFF44AAFF.toInt(),
            listOf("cold", "clicker", "auto", "click", "drill", "swap", "threshold", "glacite", "mining", "temperature", "shaft")
        ))
        //?}

        val rightCards = mutableListOf(
            CardInfo(
                "Name Hider",
                "Hide or customize your name display",
                0xFFFF5BFF.toInt(),
                listOf("name", "hider", "replacement", "gradient", "color", "hide", "username", "disguise", "anonymize")
            ),
        )
        //? if isCheat {
        rightCards.add(CardInfo(
            "Auto Clicker",
            "Automated clicking for mining",
            0xFFFFA05B.toInt(),
            listOf("auto", "clicker", "coalclick", "hud", "tab", "cooldown", "rod", "swap", "drill", "mining slot", "maniac miner", "second drill", "automated")
        ))
        //?}
        rightCards.add(CardInfo(
            "Command Keybinds",
            "Bind commands to keys",
            0xFFA05BFF.toInt(),
            listOf("command", "keybind", "key", "bind", "hotkey", "shortcut", "macro", "keyboard")
        ))
        rightCards.add(CardInfo(
            "Misc",
            "Miscellaneous features and utilities",
            0xFF888888.toInt(),
            listOf("misc", "miscellaneous", "auto-skip", "sho", "load", "glass", "pane", "sync", "gemstone", "connection", "utilities")
        ))
        rightCards.add(CardInfo(
            "Commission HUD",
            "Customize overlay layout, scale and styling",
            0xFF60A5FA.toInt(),
            listOf("commission", "hud", "overlay", "background", "scale", "layout", "position", "2x2", "1x4")
        ))
        //? if isCheat {
        rightCards.add(CardInfo(
            "Comm Claim",
            "Auto-claim commissions with armor swap",
            0xFFFFD700.toInt(),
            listOf("comm", "claim", "commission", "wardrobe", "armor", "bat person", "divan", "pigeon", "royal pigeon", "refined")
        ))
        rightCards.add(CardInfo(
            "Shaft Clicker",
            "Auto-click that pauses in Mineshafts",
            0xFF5BFF7C.toInt(),
            listOf("shaft", "clicker", "auto", "click", "mineshaft", "pause", "mining")
        ))
        rightCards.add(CardInfo(
            "HOTM Presets",
            "Select and apply HOTM tree presets",
            0xFF55FFAA.toInt(),
            listOf("hotm", "heart", "mountain", "perk", "ability", "token", "mining", "tree", "preset", "apply")
        ))
        //?}

        val leftScreens = mutableListOf<() -> net.minecraft.client.gui.screen.Screen>(
            { MiningProfitCategoryScreen(this) },
            { MinerOverlayCategoryScreen(this) },
            { ShaftESPCategoryScreen(this) },
            { PickaxeCooldownCategoryScreen(this) },
            { OrderedWaypointsCategoryScreen(this) }
        )
        //? if isCheat {
        leftScreens.add { InShaftClickCategoryScreen(this) }
        //?}

        val rightScreens = mutableListOf<() -> net.minecraft.client.gui.screen.Screen>(
            { NameHiderCategoryScreen(this) },
        )
        //? if isCheat {
        rightScreens.add { AutoClickerCategoryScreen(this) }
        //?}
        rightScreens.add { CommandKeybindCategoryScreen(this) }
        rightScreens.add { MiscCategoryScreen(this) }
        rightScreens.add { CommissionHudCategoryScreen(this) }
        //? if isCheat {
        rightScreens.add { CommClaimCategoryScreen(this) }
        rightScreens.add { ShaftClickerCategoryScreen(this) }
        //?}

        // Create left column cards
        leftCards.forEachIndexed { index, cardInfo ->
            val delay = 200L + (index * 100L)
            val card = createCategoryCard(
                cardInfo.title, cardInfo.description, cardInfo.color,
                leftX, cardStartY + (index * (cardHeight + cardSpacing)),
                cardWidth, cardHeight,
                mainPanel,
                delay
            ) {
                MinecraftClient.getInstance().setScreen(leftScreens[index]())
            }
            allCards.add(card to cardInfo)
        }

        // Create right column cards
        rightCards.forEachIndexed { index, cardInfo ->
            val delay = 200L + (index * 100L)
            val card = createCategoryCard(
                cardInfo.title, cardInfo.description, cardInfo.color,
                rightX, cardStartY + (index * (cardHeight + cardSpacing)),
                cardWidth, cardHeight,
                mainPanel,
                delay
            ) {
                if (index < rightScreens.size) {
                    MinecraftClient.getInstance().setScreen(rightScreens[index]())
                }
                //? if isCheat {
                else {
                    // HOTM Presets - opens chest GUI
                    forfun.miningqol.client.hotm.HotmPresetScreen.open()
                }
                //?}
            }
            allCards.add(card to cardInfo)
        }

        // Close button at bottom
        Button("Close", 0xFFFFFFFF.toInt(), fontSize = 15f)
            .setSizing(140f, Size.Pixels, 42f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -25f)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                closeWithAnimation()
                true
            }
            .childOf(mainPanel)
            .fadeIn(1000, EasingType.EASE_OUT)
    }

    private fun filterCards(query: String) {
        allCards.forEach { (card, info) ->
            val matches = query.isEmpty() ||
                         info.title.lowercase().contains(query) ||
                         info.description.lowercase().contains(query) ||
                         info.keywords.any { it.lowercase().contains(query) }
            card.visible = matches
        }
    }

    private fun createCategoryCard(
        title: String,
        description: String,
        accentColor: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        parent: Rectangle,
        animDelay: Long,
        onClick: () -> Unit
    ): Rectangle {
        val card = Rectangle(
            backgroundColor = 0xF01E1E1E.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 12f,
            borderThickness = 1f,
            hoverColor = 0xF0252525.toInt()
        )
            .setSizing(width, Size.Pixels, height, Size.Pixels)
            .setPositioning(x, Pos.ParentPixels, y, Pos.ParentPixels)
            .onClick { mouseX, mouseY, button ->
                onClick()
                true
            }
            .childOf(parent)
            .apply {
                dropShadow = true
                shadowBlur = 20f
                shadowSpread = 1f
                shadowColor = 0x60000000.toInt()
            }

        // Fade in card with delay
        card.visible = false
        Thread {
            Thread.sleep(animDelay)
            MinecraftClient.getInstance().execute {
                card.fadeIn(400, EasingType.EASE_OUT)
            }
        }.start()

        // Glowing accent bar on left
        Rectangle(
            backgroundColor = accentColor,
            borderRadius = 12f
        )
            .setSizing(5f, Size.Pixels, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .ignoreMouseEvents()
            .childOf(card)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
            }

        // Title
        Text(title, 0xFFFFFFFF.toInt(), 19f, true)
            .setPositioning(20f, Pos.ParentPixels, 16f, Pos.ParentPixels)
            .childOf(card)

        // Description
        Text(description, 0xFF888888.toInt(), 14f, false)
            .setPositioning(20f, Pos.ParentPixels, 42f, Pos.ParentPixels)
            .childOf(card)

        // Hover animation
        var isHovered = false
        card.onMouseEnter { _, _ ->
            if (!isHovered) {
                isHovered = true
                card.animateSize(width + 8f, height + 4f, 200, EasingType.EASE_OUT)
            }
        }
        card.onMouseExit { _, _ ->
            if (isHovered) {
                isHovered = false
                card.animateSize(width, height, 200, EasingType.EASE_IN)
            }
        }

        return card
    }

    private fun closeWithAnimation() {
        close()
    }

    override fun keyPressed(input: KeyInput?): Boolean {
        if (input?.key() == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true  // Consume the event to prevent pause menu
        }
        return super.keyPressed(input)
    }

    //? if is1_21_11 {
    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int): Boolean {
        // Handle ESC key to trigger close animation
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
            return true
        } else {
            // Let parent handle other keys
            return super.onKeyType(typedChar, keyCode, scanCode)
        }
    }
    //?} else {
    /*override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        // Handle ESC key to trigger close animation
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            closeWithAnimation()
        } else {
            // Let parent handle other keys
            super.onKeyType(typedChar, keyCode, scanCode)
        }
    }
    *///?}

    override fun shouldCloseOnEsc(): Boolean = false
}
