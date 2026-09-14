package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.graphics.Color;

//a class to help draw blocks with different shapes
public class Constants {
    // Base units
    public static final float GRID_UNIT = 10;

    // Teeth and hats
    public static final float JAGGED_TEETH_HEIGHT = 0;
    public static final float JAGGED_TEETH_WIDTH = 22;
    public static final float START_HAT_WIDTH = 96;
    public static final float START_HAT_HEIGHT = 15;
    public static final boolean ADD_START_HATS = false;

    // Padding constants
    public static final float NO_PADDING = 0;
    public static final float SMALL_PADDING = GRID_UNIT*1.5f;
    public static final float MEDIUM_PADDING = GRID_UNIT*2;
    public static final float MEDIUM_LARGE_PADDING = (float) (3 * GRID_UNIT);
    public static final float LARGE_PADDING = 4 * GRID_UNIT;


    // Shape constants
    public static final float BETWEEN_FIELDS_PADDING = SMALL_PADDING;
    public static final float CORNER_RADIUS = GRID_UNIT;
    public static final float NOTCH_WIDTH = (float) (5 * GRID_UNIT);
    public static final float NOTCH_HEIGHT = (float) (1.5*GRID_UNIT);
    public static final float NOTCH_OFFSET_LEFT = 3 * GRID_UNIT;
    public static final float STATEMENT_INPUT_NOTCH_OFFSET = NOTCH_OFFSET_LEFT;
    public static final float STATEMENT_BOTTOM_SPACER = -NOTCH_HEIGHT;
    public static final float STATEMENT_INPUT_PADDING_LEFT = 4 * GRID_UNIT;
    public static final float EMPTY_EXPRESSION_INPUT_WIDTH = 4f * GRID_UNIT;
    public static final float BETWEEN_STATEMENT_PADDING_Y = 4; // added from new

    // Tabs (added)
    public static final float TAB_HEIGHT = 15;
    public static final float TAB_OFFSET_FROM_TOP = 5;
    public static final double TAB_VERTICAL_OVERLAP = 2.5;
    public static final float TAB_WIDTH = 8;

    // Block geometry
    public static final float MIN_BLOCK_WIDTH = 10 * GRID_UNIT;
    public static final float MIN_BLOCK_HEIGHT = 2 * GRID_UNIT;
    public static final float EMPTY_BLOCK_SPACER_HEIGHT = 16;
    public static final float EMPTY_STATEMENT_INPUT_HEIGHT = 6 * GRID_UNIT;
    public static final float EMPTY_INLINE_INPUT_PADDING = 1 * GRID_UNIT;
    public static final float EMPTY_INLINE_INPUT_HEIGHT = 8 * GRID_UNIT;
    public static final float EXTERNAL_VALUE_INPUT_PADDING = 2;
    public static final float SPACER_DEFAULT_HEIGHT = 15;

    // Boolean Expression constants
    public static final float BOOLEAN_EXPRESSION_HEIGHT = 4 * GRID_UNIT;
    public static final float BOOLEAN_EXPRESSION_SIDE_WIDTH = BOOLEAN_EXPRESSION_HEIGHT/2;


    // Layout heights
    public static final float TOP_ROW_MIN_HEIGHT = 5;
    public static final float TOP_ROW_PRECEDES_STATEMENT_MIN_HEIGHT = LARGE_PADDING;
    public static final float BOTTOM_ROW_MIN_HEIGHT = 5;
    public static final float BOTTOM_ROW_AFTER_STATEMENT_MIN_HEIGHT = 6 * GRID_UNIT;

    // Dummy input
    public static final float DUMMY_INPUT_MIN_HEIGHT = 8 * GRID_UNIT;
    public static final float DUMMY_INPUT_SHADOW_MIN_HEIGHT = 6 * GRID_UNIT;

    // Fonts and text
    public static final float FIELD_TEXT_FONT_SIZE = (float) (3 * GRID_UNIT);
    public static final String FIELD_TEXT_FONT_WEIGHT = "normal";
    public static final String FIELD_TEXT_FONT_FAMILY = "sans-serif";
    public static final int FIELD_TEXT_HEIGHT = -1; // dynamic
    public static final int FIELD_TEXT_BASELINE = -1; // dynamic

    // Field border rectangles
    public static final float FIELD_BORDER_RECT_RADIUS = CORNER_RADIUS;
    public static final float FIELD_BORDER_RECT_HEIGHT = 8 * GRID_UNIT;
    public static final float FIELD_BORDER_RECT_X_PADDING = 2 * GRID_UNIT;
    public static final float FIELD_ROUND_HEIGHT = 3.2f * GRID_UNIT;
    public static final float FIELD_ROUND_MIN_WIDTH = 6 * GRID_UNIT;
    public static final float FIELD_BORDER_RECT_Y_PADDING = (float) (1.625 * GRID_UNIT);
    public static final float FIELD_DROPDOWN_BORDER_RECT_HEIGHT = 8 * GRID_UNIT;
    public static final float FIELD_DROPDOWN_SVG_ARROW_PADDING = FIELD_BORDER_RECT_X_PADDING;

    // Colours and checkboxes
    public static final float FIELD_COLOUR_DEFAULT_WIDTH = 6 * GRID_UNIT;
    public static final float FIELD_COLOUR_DEFAULT_HEIGHT = 8 * GRID_UNIT;
    public static final float FIELD_CHECKBOX_X_OFFSET =  GRID_UNIT;

    // Misc layout
    public static final float CURSOR_WS_WIDTH = 20 * GRID_UNIT;
    public static final float STATEMENT_INPUT_SPACER_MIN_WIDTH = 30 * GRID_UNIT;
    public static final float MAX_DYNAMIC_CONNECTION_SHAPE_WIDTH = 12 * GRID_UNIT;
    public static final float EMPTY_INLINE_INPUT_WIDTH = 2 * GRID_UNIT;
    public static final float STATEMENT_INPUT_INDENT = 5*GRID_UNIT;

    public static class CategoryColors {
        public static final int Motion      = Color.parseColor("#4C97FF"); // Blue – motion blocks
        public static final int Looks       = Color.parseColor("#9966FF"); // Purple – looks blocks
        public static final int Sound       = Color.parseColor("#D65CD6"); // Pink – sound blocks
        public static final int Events      = Color.parseColor("#FFD500"); // Yellow – events blocks
        public static final int Control     = Color.parseColor("#FFAB19"); // Orange – control blocks
        public static final int Sensing     = Color.parseColor("#4CBFE6"); // Light blue – sensing blocks
        public static final int Operators   = Color.parseColor("#40BF4A"); // Green – operators blocks
        public static final int Variables   = Color.parseColor("#FF8C1A"); // Dark orange – variables blocks
        public static final int Lists       = Color.parseColor("#FF661D"); // Red/orange – list blocks
        public static final int MyBlocks    = Color.parseColor("#FF6680"); // Pink/purple – custom blocks
        public static final int Extensions  = Color.parseColor("#0FBD8C"); // Teal – extension/pen blocks
    }


    // Shape enum
    public enum SHAPE {
        HEXAGON,
        ROUND,
        SQUARE,
        PUZZLE,
        NOTCH
    }
    private Constants() {
        // Prevent instantiation
    }
}