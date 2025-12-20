package forfun.miningqol.client.gui

import forfun.miningqol.client.MiningqolClient
import forfun.miningqol.client.update.UpdateChecker
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.elements.Button
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.animations.*
import java.awt.Desktop
import java.net.URI

class UpdateScreen : VexelScreen("MiningQOL Update Available") {

    private lateinit var mainPanel: Rectangle
    private lateinit var statusText: Text
    private lateinit var downloadButton: Button
    private lateinit var laterButton: Button
    private lateinit var doneButton: Button
    private var isDownloading = false
    private var downloadComplete = false

    override fun afterInitialization() {
        // Semi-transparent overlay
        Rectangle(
            backgroundColor = 0x80000000.toInt(),
            borderColor = 0x00000000,
            borderRadius = 0f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 100f, Size.ParentPerc)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(window)
            .fadeIn(300, EasingType.EASE_OUT)

        // Main panel
        mainPanel = Rectangle(
            backgroundColor = 0xF0121212.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 1f
        )
            .setSizing(450f, Size.Pixels, 320f, Size.Pixels)
            .childOf(window)
            .apply {
                dropShadow = true
                shadowBlur = 40f
                shadowSpread = 2f
                shadowColor = 0xA0000000.toInt()
            }

        mainPanel.xPositionConstraint = Pos.ScreenPixels
        mainPanel.yPositionConstraint = Pos.ScreenPixels
        mainPanel.xConstraint = (mainPanel.screenWidth - 450f) / 2f
        mainPanel.yConstraint = (mainPanel.screenHeight - 320f) / 2f
        mainPanel.fadeIn(400, EasingType.EASE_OUT)

        // Title bar
        Rectangle(
            backgroundColor = 0xFF1A1A1A.toInt(),
            borderColor = 0xFF2A2A2A.toInt(),
            borderRadius = 16f,
            borderThickness = 0f
        )
            .setSizing(100f, Size.ParentPerc, 60f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusBottomLeft = 0f
                borderRadiusBottomRight = 0f
            }

        // Update icon (green accent bar)
        Rectangle(
            backgroundColor = 0xFF4CAF50.toInt(),
            borderRadius = 16f
        )
            .setSizing(5f, Size.Pixels, 60f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .childOf(mainPanel)
            .apply {
                borderRadiusTopRight = 0f
                borderRadiusBottomRight = 0f
                borderRadiusBottomLeft = 0f
            }

        // Title
        Text("Update Available!", 0xFF4CAF50.toInt(), 24f, true)
            .setPositioning(0f, Pos.ParentCenter, 18f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Version info
        val currentVersion = UpdateChecker.getCurrentVersion()
        val latestVersion = UpdateChecker.getLatestVersion() ?: "Unknown"

        Text("Current: v$currentVersion  →  Latest: v$latestVersion", 0xFFAAAAAA.toInt(), 14f, false)
            .setPositioning(0f, Pos.ParentCenter, 75f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Release notes header
        Text("What's New:", 0xFFFFFFFF.toInt(), 14f, true)
            .setPositioning(25f, Pos.ParentPixels, 105f, Pos.ParentPixels)
            .childOf(mainPanel)

        // Release notes - display as bullet points
        val releaseNotes = UpdateChecker.getReleaseNotes() ?: "No release notes available."
        val lines = releaseNotes.split("\n", "\r\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(4) // Max 4 lines
            .map { line ->
                // Truncate long lines
                val cleanLine = line.removePrefix("-").removePrefix("*").trim()
                if (cleanLine.length > 50) cleanLine.substring(0, 47) + "..." else cleanLine
            }

        var yOffset = 125f
        for (line in lines) {
            Text("• $line", 0xFF888888.toInt(), 11f, false)
                .setPositioning(25f, Pos.ParentPixels, yOffset, Pos.ParentPixels)
                .childOf(mainPanel)
            yOffset += 14f
        }

        // Status text
        statusText = Text("", 0xFFFFD700.toInt(), 12f, false)
            .setPositioning(0f, Pos.ParentCenter, 195f, Pos.ParentPixels)
            .childOf(mainPanel) as Text

        // Download button
        downloadButton = Button("Download Update", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(180f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(25f, Pos.ParentPixels, 220f, Pos.ParentPixels)
            .backgroundColor(0xFF4CAF50.toInt())
            .borderColor(0xFF45A049.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF45A049.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF388E3C.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                if (downloadComplete) {
                    close()
                } else if (!isDownloading) {
                    downloadUpdate()
                }
                true
            }
            .childOf(mainPanel) as Button

        // Open in Browser button
        Button("Open in Browser", 0xFFFFFFFF.toInt(), fontSize = 14f)
            .setSizing(180f, Size.Pixels, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 220f, Pos.ParentPixels)
            .alignRight()
            .setOffset(-25f, 0f)
            .backgroundColor(0xFF2196F3.toInt())
            .borderColor(0xFF1976D2.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF1976D2.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1565C0.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                openInBrowser()
                true
            }
            .childOf(mainPanel)

        // Later button (hidden after download)
        laterButton = Button("Remind Me Later", 0xFFAAAAAA.toInt(), fontSize = 13f)
            .setSizing(140f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -15f)
            .backgroundColor(0xFF2A2A2A.toInt())
            .borderColor(0xFF404040.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF353535.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF1A1A1A.toInt(), 0xFFAAAAAA.toInt())
            .onClick { _, _, _ ->
                close()
                true
            }
            .childOf(mainPanel) as Button

        // Done button (shown after download completes)
        doneButton = Button("Done", 0xFFFFFFFF.toInt(), fontSize = 13f)
            .setSizing(140f, Size.Pixels, 36f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .alignBottom()
            .setOffset(0f, -15f)
            .backgroundColor(0xFF4CAF50.toInt())
            .borderColor(0xFF45A049.toInt())
            .borderRadius(8f)
            .borderThickness(1f)
            .hoverColors(0xFF45A049.toInt(), 0xFFFFFFFF.toInt())
            .pressedColors(0xFF388E3C.toInt(), 0xFFFFFFFF.toInt())
            .onClick { _, _, _ ->
                close()
                true
            }
            .childOf(mainPanel) as Button
        doneButton.visible = false

        // Don't Show Again button
        Button("Don't Show Again", 0xFF666666.toInt(), fontSize = 11f)
            .setSizing(120f, Size.Pixels, 28f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
            .alignRight()
            .alignBottom()
            .setOffset(-15f, -18f)
            .backgroundColor(0xFF1A1A1A.toInt())
            .borderColor(0xFF333333.toInt())
            .borderRadius(6f)
            .borderThickness(1f)
            .hoverColors(0xFF252525.toInt(), 0xFFAAAAAA.toInt())
            .pressedColors(0xFF151515.toInt(), 0xFF888888.toInt())
            .onClick { _, _, _ ->
                dismissUpdate()
                close()
                true
            }
            .childOf(mainPanel)
    }

    private fun dismissUpdate() {
        val config = MiningqolClient.getConfig()
        config.dismissedUpdateVersion = UpdateChecker.getLatestVersion() ?: ""
        config.save()
    }

    private fun downloadUpdate() {
        isDownloading = true
        statusText.text = "Downloading..."
        downloadButton.text = "Downloading..."

        val modsFolder = FabricLoader.getInstance().gameDir.resolve("mods")

        UpdateChecker.downloadUpdate(modsFolder).thenAccept { success ->
            MinecraftClient.getInstance().execute {
                if (success) {
                    statusText.text = "Updated! Restart the game to apply."
                    statusText.color(0xFF4CAF50.toInt())
                    downloadButton.visible = false
                    downloadComplete = true
                    isDownloading = false
                    // Hide Later button and show Done button
                    laterButton.visible = false
                    doneButton.visible = true
                    dismissUpdate() // Don't show again after successful download
                } else {
                    statusText.text = "Download failed. Try browser instead."
                    statusText.color(0xFFFF6B6B.toInt())
                    downloadButton.text = "Download Update"
                    isDownloading = false
                }
            }
        }
    }

    private fun openInBrowser() {
        try {
            val url = UpdateChecker.getDownloadUrl() ?: "https://github.com/Rinity9801/MiningQOL/releases/latest"
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            statusText.text = "Failed to open browser"
            statusText.color(0xFFFF6B6B.toInt())
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == KnitKeys.KEY_ESCAPE.code) {
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun shouldCloseOnEsc(): Boolean = false
}
