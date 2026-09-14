package FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;

/**
 * DummyInput is an input type that contains only fields, with no block connections.
 * <p>
 * Use cases:
 * - Displaying labels and text on blocks
 * - Adding inline fields like dropdowns, number inputs, or text inputs
 * - Creating spacing or visual structure within a block
 * <p>
 * DummyInputs do not have connections, so they cannot accept other blocks.
 * They are purely for displaying fields inline with the block.
 * <p>
 * Example usage:
 * <pre>
 *  // Create a block with a label
 *  Statement moveBlock = new MoveStatement("move");
 *  moveBlock.inputs.add(new DummyInput(moveBlock)
 *      .addField(new LabelField("MOVE FORWARD")));
 *
 *  // Create a block with multiple fields
 *  Statement setBlock = new SetStatement("set");
 *  setBlock.inputs.add(new DummyInput(setBlock)
 *      .addField(new LabelField("SET"))
 *      .addField(new DropdownField("variable"))
 *      .addField(new LabelField("TO")));
 * </pre>
 */
public class DummyInput extends Input<DummyInput> {
    
    /**
     * Create a DummyInput for the specified block
     * @param sourceBlock The block this input belongs to
     */
    public DummyInput(Block sourceBlock) {
        super(sourceBlock, InputType.DUMMY);
        // DummyInputs never have connections
        this.connection = null;
    }
    @Override
    public void connect(Block block) {
        throw new UnsupportedOperationException("DummyInputs cannot accept other blocks");
    }
    /**
     * Get a string representation of this input
     * @return String describing this dummy input
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DummyInput[fields=");
        for (int i = 0; i < fields.size(); i++) {
            sb.append(fields.get(i).name);
            if (i < fields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
