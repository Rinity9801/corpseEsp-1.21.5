package forfun.miningqol.client.hotm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * HOTM tree layout (top of GUI = HOTM 10, bottom = HOTM 1).
 * Wide rows alternate with narrow connector rows (cols 2,4,6).
 */
public enum HotmNode {
    // Row 0 - HOTM 10 (top, wide)
    T10_ABILITY_L(0, 1, Type.ABILITY, "T10 Ability L"),
    T10_PERK_1(0, 2, Type.PERK, "T10 Perk 1"),
    T10_PERK_2(0, 3, Type.PERK, "T10 Perk 2"),
    T10_PERK_3(0, 4, Type.PERK, "T10 Perk 3"),
    T10_PERK_4(0, 5, Type.PERK, "T10 Perk 4"),
    T10_PERK_5(0, 6, Type.PERK, "T10 Perk 5"),
    T10_ABILITY_R(0, 7, Type.ABILITY, "T10 Ability R"),

    // Row 1 - connector
    T9_CONN_L(1, 2, Type.PERK, "T9 Connector L"),
    T9_CONN_M(1, 4, Type.PERK, "T9 Connector M"),
    T9_CONN_R(1, 6, Type.PERK, "T9 Connector R"),

    // Row 2 - HOTM 8 (wide)
    T8_PERK_1(2, 1, Type.PERK, "T8 Perk 1"),
    T8_PERK_2(2, 2, Type.PERK, "T8 Perk 2"),
    T8_PERK_3(2, 3, Type.PERK, "T8 Perk 3"),
    T8_PERK_4(2, 4, Type.PERK, "T8 Perk 4"),
    T8_PERK_5(2, 5, Type.PERK, "T8 Perk 5"),
    T8_PERK_6(2, 6, Type.PERK, "T8 Perk 6"),
    T8_PERK_7(2, 7, Type.PERK, "T8 Perk 7"),

    // Row 3 - connector
    T7_CONN_L(3, 2, Type.PERK, "T7 Connector L"),
    T7_CONN_M(3, 4, Type.PERK, "T7 Connector M"),
    T7_CONN_R(3, 6, Type.PERK, "T7 Connector R"),

    // Row 4 - HOTM 6 (wide, with abilities on edges)
    T6_ABILITY_L(4, 1, Type.ABILITY, "T6 Ability L"),
    T6_PERK_2(4, 2, Type.PERK, "T6 Perk 2"),
    T6_PERK_3(4, 3, Type.PERK, "T6 Perk 3"),
    T6_PERK_4(4, 4, Type.PERK, "T6 Perk 4"),
    T6_PERK_5(4, 5, Type.PERK, "T6 Perk 5"),
    T6_PERK_6(4, 6, Type.PERK, "T6 Perk 6"),
    T6_ABILITY_R(4, 7, Type.ABILITY, "T6 Ability R"),

    // Row 5 - connector (Core of the Mountain area)
    T5_CONN_L(5, 2, Type.PERK, "T5 Connector L"),
    T5_CORE(5, 4, Type.PERK, "Core of the Mountain", true),
    T5_CONN_R(5, 6, Type.PERK, "T5 Connector R"),

    // Row 6 - HOTM 4 (wide)
    T4_PERK_1(6, 1, Type.PERK, "T4 Perk 1"),
    T4_PERK_2(6, 2, Type.PERK, "T4 Perk 2"),
    T4_PERK_3(6, 3, Type.PERK, "T4 Perk 3"),
    T4_PERK_4(6, 4, Type.PERK, "T4 Perk 4"),
    T4_PERK_5(6, 5, Type.PERK, "T4 Perk 5"),
    T4_PERK_6(6, 6, Type.PERK, "T4 Perk 6"),
    T4_PERK_7(6, 7, Type.PERK, "T4 Perk 7"),

    // Row 7 - connector
    T3_CONN_L(7, 2, Type.PERK, "T3 Connector L"),
    T3_CONN_M(7, 4, Type.PERK, "T3 Connector M"),
    T3_CONN_R(7, 6, Type.PERK, "T3 Connector R"),

    // Row 8 - HOTM 2 (with abilities on edges)
    T2_ABILITY_L(8, 2, Type.ABILITY, "T2 Ability L"),
    T2_PERK_1(8, 3, Type.PERK, "T2 Perk 1"),
    T2_PERK_2(8, 4, Type.PERK, "T2 Perk 2"),
    T2_PERK_3(8, 5, Type.PERK, "T2 Perk 3"),
    T2_ABILITY_R(8, 6, Type.ABILITY, "T2 Ability R"),

    // Row 9 - HOTM 1 (bottom, single node)
    MINING_SPEED(9, 4, Type.PERK, "Mining Speed");

    public enum Type { PERK, ABILITY }

    public enum State {
        NOT_CLICKED,  // 0 tokens
        DISABLED,     // 1 token - tree passes through but effect is off
        LEVEL_1,      // 1 token - perk only
        MAXED,        // 1 token - perk only
        CHOSEN;       // 1 token - ability only

        public int getTokenCost() {
            return this == NOT_CLICKED ? 0 : 1;
        }

        public boolean isValidFor(Type type) {
            return switch (this) {
                case NOT_CLICKED, DISABLED -> true;
                case LEVEL_1, MAXED -> type == Type.PERK;
                case CHOSEN -> type == Type.ABILITY;
            };
        }
    }

    private final int row;
    private final int col;
    private final Type type;
    private final String displayName;
    private final boolean alwaysEnabled;

    HotmNode(int row, int col, Type type, String displayName) {
        this(row, col, type, displayName, false);
    }

    HotmNode(int row, int col, Type type, String displayName, boolean alwaysEnabled) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.displayName = displayName;
        this.alwaysEnabled = alwaysEnabled;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public Type getType() { return type; }
    public String getDisplayName() { return displayName; }
    public boolean isAlwaysEnabled() { return alwaysEnabled; }

    public Item getItemForState(State state) {
        return switch (state) {
            case NOT_CLICKED -> type == Type.PERK ? Items.COAL : Items.COAL_BLOCK;
            case DISABLED -> Items.REDSTONE_BLOCK;
            case LEVEL_1 -> Items.EMERALD;
            case MAXED -> Items.DIAMOND;
            case CHOSEN -> Items.EMERALD_BLOCK;
        };
    }

    /**
     * Returns the slot index in the chest GUI for this node.
     * Row 0 is at the top of the GUI, row 9 at the bottom.
     * Returns -1 if not visible at the current scroll position.
     */
    public int getSlotIndex(int scrollOffset) {
        int visRow = row - scrollOffset;
        if (visRow < 0 || visRow > 4) return -1;
        return visRow * 9 + col;
    }

    public static final int TREE_ROWS = 10;
    public static final int MAX_SCROLL = TREE_ROWS - 5;
}
