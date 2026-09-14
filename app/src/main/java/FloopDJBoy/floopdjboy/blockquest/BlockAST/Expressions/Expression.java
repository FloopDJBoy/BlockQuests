package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import androidx.annotation.CheckResult;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Connection;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.World;

public abstract class Expression<T> extends Block {
    private final Class<T> returnType;
    public Connection outputConnection;
    public Expression(String name, Class<T> returnType, Category category) {
        super(name,category);
        this.returnType = returnType;
    }
    public Class<T> getStaticReturnType() {
        return returnType;
    }

    public Class<?> getReturnType() {
        return returnType;
    }
    @Override
    public Connection makeConnection(ConnectionType type) {
        assert type != ConnectionType.NEXT_STATEMENT && type != ConnectionType.PREVIOUS_STATEMENT;
        return super.makeConnection(type);
    }

    /**
     * Evaluates this expression in the given context and world, producing a value.
     *
     * @param ctx   the {@link ScriptContext} where the expression is being evaluated
     * @param world the {@link World} in which the expression operates
     *
     * @return the computed value of this expression, of type {@code T}<br>
     * <br>
     * <b>Notes:</b><br>
     * - Evaluation is instantaneous and does not consume a tick.<br>
     * - Expressions are typically used as inputs for statements or conditions. <br>
     * - Expressions may contain other {@code Expression} objects, but never {@code Statement} objects.<br>
     *      (e.g., {@code isFacing<Boolean>(Expression<Actor.AbsoluteDirection>)} forms a boolean expression) <br>
     * - An Expression may not have any side effect upon the world or script
     */
    @CheckResult
    public abstract T evaluate(final ScriptContext ctx,final World world);
}
