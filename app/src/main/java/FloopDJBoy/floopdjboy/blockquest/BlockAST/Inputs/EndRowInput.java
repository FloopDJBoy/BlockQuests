package FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs;

import androidx.annotation.NonNull;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;
/**
 * EndRowInput is a special input type that signals the end of a row in block layout.
 * <p>
 * When the renderer encounters an EndRowInput, it moves to the next line,
 * allowing you to create multi-row blocks.
 * <p>
 * Use cases:
 * - Creating blocks with multiple rows of inputs
 * - Placing statement inputs on their own row
 * - Controlling block layout and visual structure
 * <p>
 * EndRowInputs do not have connections or fields - they are purely layout markers.
 * <p>
 * Example usage:
 * <pre>
 * // Create an IF block with condition on first row and body on second row
 * Statement ifBlock = new IfStatement("if");
 * 
 * // First row: "IF" label and condition input
 * ifBlock.inputs.add(new DummyInput(ifBlock)
 *     .addField(new LabelField("IF")));
 * ifBlock.inputs.add(new ExpressionInput(ifBlock));
 * 
 * // End the row
 * ifBlock.inputs.add(new EndRowInput(ifBlock));
 * 
 * // Second row: statement body
 * ifBlock.inputs.add(new StatementInput(ifBlock));
 * </pre>
 */
public class EndRowInput extends Input<EndRowInput> {
    
    /**
     * Create an EndRowInput for the specified block
     * @param sourceBlock The block this input belongs to
     */
    public EndRowInput(Block sourceBlock) {
        super(sourceBlock, InputType.END_ROW);
        // EndRowInputs never have connections
        this.connection = null;
    }
    @Override
    public void connect(Block block) {
        throw new UnsupportedOperationException("EndRowInput cannot accept other blocks");
    }

    /**
     * EndRowInputs cannot have fields
     * @throws UnsupportedOperationException always
     */
    @Override
    public EndRowInput addField(FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.Field<?> field) {
        throw new UnsupportedOperationException("EndRowInput cannot have fields");
    }
    
    /**
     * EndRowInputs cannot have fields
     * @throws UnsupportedOperationException always
     */
    @Override
    public EndRowInput addFieldAt(FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.Field<?> field, int index) {
        throw new UnsupportedOperationException("EndRowInput cannot have fields");
    }
    
    /**
     * Get a string representation of this input
     * @return String describing this end row marker
     */
    @NonNull
    @Override
    public String toString() {
        return "EndRowInput[]";
    }
}
