package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.MathBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class EqualBlock extends Expression<Boolean> {
    private final ExpressionInput first, second;

    public EqualBlock() {
        super("EqualBlock", Boolean.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField(this, "=="));
        first = new ExpressionInput(this, Object.class);
        second = new ExpressionInput(this, Object.class);
        inputs.add(first);
        inputs.add(input);
        inputs.add(second);
    }

    @Override
    public Boolean evaluate(ScriptContext ctx, World world) {
        Object a = first.getConnectedExpression().evaluate(ctx, world);
        Object b = second.getConnectedExpression().evaluate(ctx, world);
        return a != null && a.equals(b);
    }
}
