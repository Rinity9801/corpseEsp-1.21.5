package forfun.miningqol.client.gui

import net.minecraft.client.gui.screens.Screen

/**
 * Extra main-menu categories contributed at runtime — the cheat source tree
 * registers its cards here (via CheatGui) so the shared VexelMainScreen never
 * references cheat classes.
 */
object ExtraCategories {
    class Entry(
        @JvmField val title: String,
        @JvmField val description: String,
        @JvmField val accent: Int,
        @JvmField val open: (Screen) -> Unit
    )

    @JvmField
    val entries = mutableListOf<Entry>()

    @JvmStatic
    fun add(title: String, description: String, accent: Int, open: (Screen) -> Unit) {
        entries.add(Entry(title, description, accent, open))
    }
}
