package FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs;

import androidx.annotation.NonNull;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableBlock;
import FloopDJBoy.floopdjboy.blockquest.World;

/**
 * ExpressionInput represents a socket that can receive an Expression block.
 * It is typed, meaning it only accepts expressions that return a specific type.
 */
public class ExpressionInput extends Input<ExpressionInput> {
    private final Class<?> expectedType;

    /**
     * Create an ExpressionInput for the specified block with an expected return type.
     * @param sourceBlock The block this input belongs to
     * @param expectedType The type of expression this input accepts (e.g., Boolean.class)
     */
    public ExpressionInput(Block sourceBlock, Class<?> expectedType) {
        super(sourceBlock, InputType.EXPRESSION);
        this.expectedType = expectedType;
        this.connection = sourceBlock.makeConnection(ConnectionType.INPUT_VALUE);
    }
    @Override
    public void connect(Block expression) {
        // Validate the block is not null
        if (expression == null) {
            throw new IllegalArgumentException("Cannot connect null block to ExpressionInput");
        }

        // Validate the block is an Expression
        if (!(expression instanceof Expression)) {
            throw new IllegalArgumentException("ExpressionInput can only connect to Expression blocks");
        }

        Expression<?> expr = (Expression<?>) expression;

        // Validate type safety - the expression's return type must match the expected type
        if (!getExpectedType().isAssignableFrom(expr.getReturnType())) {
            throw new IllegalArgumentException(
                    "Type mismatch: ExpressionInput expects " + getExpectedType().getSimpleName() +
                            " but expression returns " + expr.getReturnType().getSimpleName()
            );
        }

        // Validate the expression has an output connection
        if (expr.outputConnection == null) {
            expr.outputConnection = expr.makeConnection(ConnectionType.OUTPUT_VALUE);
        }

        // Validate connections are compatible
        if (!connection.canConnect(connection, expr.outputConnection)) {
            throw new IllegalStateException("Incompatible connections");
        }

        // Clean disconnects - if either side is already connected, disconnect first
        if (connection.isConnected()) {
            connection.disconnect();
        }
        if (expr.outputConnection.isConnected()) {
            expr.outputConnection.disconnect();
        }

        // Establish the connection
        connection.connect(expr.outputConnection);

        // Set parent ownership - the expression's parent is the block that owns this input
        expression.setParent(getSourceBlock());
    }
    /**
     * Get the type of expression this input expects.
     * @return The expected return type class
     */
    public Class<?> getExpectedType() {
        if(getConnectedExpression() instanceof VariableBlock){
            return getConnectedExpression().getReturnType();
        }else{
            return expectedType;
        }
    }
    public Expression<?> getConnectedExpression() {
        Block target = connection.getTargetBlock();
        return (Expression<?>) target;
    }
    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';

        // wrapper classes if you want boxed defaults
        if (type == Boolean.class) return false;
        if (type == Byte.class) return (byte) 0;
        if (type == Short.class) return (short) 0;
        if (type == Integer.class) return 0;
        if (type == Long.class) return 0L;
        if (type == Float.class) return 0f;
        if (type == Double.class) return 0d;
        if (type == Character.class) return '\0';

        if (type == String.class) return "";

        // all other reference types
        return null;
    }
    public Object getConnectedValueOrDefault(ScriptContext ctx, World world) {
        Expression<?> expr = getConnectedExpression();
        if(expr==null){
            return getDefaultValue(getExpectedType());
        }else{
            return expr.evaluate(ctx,world);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ExpressionInput[expectedType=" + getExpectedType().getSimpleName() + "]";
    }
}
