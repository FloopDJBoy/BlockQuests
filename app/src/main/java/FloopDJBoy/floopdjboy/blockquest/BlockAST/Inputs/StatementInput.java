package FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs;

import androidx.annotation.NonNull;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;

/**
 * StatementInput represents a body slot on a flow-control block (e.g. the body of an IF or REPEAT block).
 * <p>
 * The input holds a {@link ConnectionType#STATEMENT_INPUT} connection on the parent block's side.
 * The first child {@link Statement} placed in the body connects its
 * {@link Statement#previousConnection} ({@link ConnectionType#PREVIOUS_STATEMENT}) to this input's connection.
 * <p>
 * Subsequent statements in the body chain to each other via the normal
 * {@link ConnectionType#NEXT_STATEMENT} / {@link ConnectionType#PREVIOUS_STATEMENT} pairing.
 * <p>
 * Example usage:
 * <pre>
 * // Create an IF block with a condition and a statement body
 * Statement ifBlock = new IfStatement("if");
 *
 * // First row: "IF" label and boolean condition socket
 * ifBlock.inputs.add(new DummyInput(ifBlock)
 *     .addField(new LabelField("IF")));
 * ifBlock.inputs.add(new ExpressionInput(ifBlock, Boolean.class));
 *
 * // End the first row
 * ifBlock.inputs.add(new EndRowInput(ifBlock));
 *
 * // Second row: statement body
 * ifBlock.inputs.add(new StatementInput(ifBlock));
 * </pre>
 */
public class StatementInput extends Input<StatementInput> {

    /**
     * Create a StatementInput for the specified block.
     * @param sourceBlock The block this input belongs to (must be a {@link Statement})
     */
    public StatementInput(Block sourceBlock) {
        super(sourceBlock, InputType.STATEMENT);
        this.connection = sourceBlock.makeConnection(ConnectionType.STATEMENT_INPUT);
    }

    /**
     * Connect the first {@link Statement} of a body chain into this input.
     *
     * @param block The first statement to place in the body; must be a {@link Statement}
     * @throws IllegalArgumentException if {@code block} is null or not a {@link Statement}
     * @throws IllegalStateException    if the statement has no {@code previousConnection}
     *                                  and one cannot be created
     */
    @Override
    public void connect(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("Cannot connect null block to StatementInput");
        }
        if (!(block instanceof Statement stmt)) {
            throw new IllegalArgumentException("StatementInput can only connect to Statement blocks");
        }

        // Ensure the child has a previousConnection to connect to
        if (stmt.previousConnection == null) {
            stmt.previousConnection = stmt.makeConnection(ConnectionType.PREVIOUS_STATEMENT);
        }

        if (!connection.canConnect(connection, stmt.previousConnection)) {
            throw new IllegalStateException("Incompatible connections");
        }

        // Clean disconnects
        if (connection.isConnected()) {
            connection.disconnect();
        }
        if (stmt.previousConnection.isConnected()) {
            stmt.previousConnection.disconnect();
        }

        // Establish the connection
        connection.connect(stmt.previousConnection);

        // Set parent ownership
        block.setParent(getSourceBlock());
    }

    @NonNull
    @Override
    public String toString() {
        return "StatementInput[connected=" + connection.isConnected() + "]";
    }
}
