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

/**
 * Extra rows for the Misc settings screen, contributed at runtime by the cheat
 * source tree (same idea as ExtraCategories).
 */
object ExtraMiscRows {
    class Toggle(
        @JvmField val title: String,
        @JvmField val description: String,
        @JvmField val get: () -> Boolean,
        @JvmField val set: (Boolean) -> Unit
    )

    @JvmField
    val toggles = mutableListOf<Toggle>()

    @JvmStatic
    fun addToggle(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        toggles.add(Toggle(title, description, get, set))
    }
}

/**
 * Extra rows for the Corpse/Shaft ESP screens, contributed at runtime — used by the
 * local-only ESP feed module so the released screens never reference it.
 */
object ExtraEspRows {
    @JvmField
    val corpse = mutableListOf<ExtraMiscRows.Toggle>()

    @JvmField
    val shaft = mutableListOf<ExtraMiscRows.Toggle>()

    @JvmStatic
    fun addCorpse(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        corpse.add(ExtraMiscRows.Toggle(title, description, get, set))
    }

    @JvmStatic
    fun addShaft(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        shaft.add(ExtraMiscRows.Toggle(title, description, get, set))
    }
}
