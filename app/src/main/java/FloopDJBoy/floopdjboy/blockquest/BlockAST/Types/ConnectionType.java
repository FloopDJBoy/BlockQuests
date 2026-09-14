package FloopDJBoy.floopdjboy.blockquest.BlockAST.Types;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;

public enum ConnectionType {
    /**
     * connection to an {@link Expression} <br>
     * connects to {@code OUTPUT_VALUE}
     */
    INPUT_VALUE,
    /**
     * connection to receive an {@link Expression}<br>
     * connects to {@code INPUT_VALUE}
     */
    OUTPUT_VALUE,
    /**
     * connection to a {@link Statement} <br>
     * connects to {@code PREVIOUS_STATEMENT}
     */
    NEXT_STATEMENT,
    /**
     * connection to a {@link Statement}<br>
     * connects to {@code NEXT_STATEMENT}
     */
    PREVIOUS_STATEMENT,
    /**
     * connection held by a {@link FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput} on a parent block (e.g. IF body).<br>
     * connects to {@code PREVIOUS_STATEMENT} of the first child block in the body.
     */
    STATEMENT_INPUT;
}
