package FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields;

import android.util.Log;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;

/**
 * LabelField is a non-editable text field used for displaying static text on blocks.
 * Common uses include:
 * - Block operation names (e.g., "MOVE", "TURN", "IF")
 * - Connecting words (e.g., "TO", "BY", "AND")
 * - Instructions or hints
 * 
 * LabelFields are purely visual and cannot be edited by the user.
 */
public class LabelField extends Field<String> {
    
    /**
     * Create a LabelField with a source block and text
     * @param sourceBlock The block this field belongs to
     * @param text The text to display
     */
    public LabelField(Block sourceBlock, String text) {

        super(sourceBlock, text, String.class, text);
        this.text = text;
    }
    
    /**
     * Create a LabelField with just text (block will be set later)
     * @param text The text to display
     */
    public LabelField(String text) {
        super(text, String.class);
        this.value = text;
        this.text = text;
    }
    @Override
    public void setValue(String value) {
        Log.e("error","attempt to set value of non-editable field "+ name);
    }
    
    /**
     * LabelFields are never editable
     * @return false always
     */
    @Override
    public boolean isEditable() {
        return false;
    }
    
    /**
     * Get the display text
     * @return The label text
     */



    @Override
    public String getDefaultValue() {
        return "";
    }
}
