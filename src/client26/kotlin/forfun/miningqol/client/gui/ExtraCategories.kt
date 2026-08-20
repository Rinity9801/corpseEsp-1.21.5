package forfun.miningqol.client.gui

import xyz.meowing.vexel.components.core.Rectangle

/**
 * One feature card in the settings GUI. Either builds an inline detail panel
 * (detail != null; returns the y below its last control) or opens an external
 * screen when its card is clicked (open != null). [status] backs the ON/OFF
 * pill on the card, when the feature has a single meaningful enabled state.
 */
class GuiFeature(
    @JvmField val title: String,
    @JvmField val description: String,
    @JvmField val accent: Int,
    @JvmField val detail: ((host: VexelMainScreen, wrapper: Rectangle, width: Float) -> Float)? = null,
    @JvmField val open: (() -> Unit)? = null,
    @JvmField val status: (() -> Boolean)? = null
)

class GuiCategory(
    @JvmField val name: String,
    @JvmField val features: List<GuiFeature>
)

/**
 * Extra sidebar categories contributed at runtime — the cheat source tree
 * registers its categories here (via CheatGui) so the shared VexelMainScreen
 * never references cheat classes.
 */
object ExtraCategories {
    @JvmField
    val categories = mutableListOf<GuiCategory>()

    @JvmStatic
    fun add(category: GuiCategory) {
        categories.add(category)
    }
}

/**
 * Extra rows for the Misc feature, contributed at runtime by the cheat
 * source tree.
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
 * Extra rows for the Ordered Waypoints feature, contributed at runtime by the
 * cheat source tree — e.g. the etherwarp auto-click toggle.
 */
object ExtraWaypointRows {
    @JvmField
    val toggles = mutableListOf<ExtraMiscRows.Toggle>()

    @JvmStatic
    fun addToggle(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        toggles.add(ExtraMiscRows.Toggle(title, description, get, set))
    }
}

/**
 * Extra rows for the Corpse/Shaft ESP features, contributed at runtime — used by the
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
