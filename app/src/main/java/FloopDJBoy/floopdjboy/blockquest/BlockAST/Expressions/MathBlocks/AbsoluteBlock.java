package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.MathBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class AbsoluteBlock extends Expression<Integer> {
    private final ExpressionInput inputExpr;

    public AbsoluteBlock() {
        super("AbsoluteBlock", Integer.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField(this, "abs"));
        inputExpr = new ExpressionInput(this, Integer.class);
        inputs.add(input);
        inputs.add(inputExpr);
    }

    @Override
    public Integer evaluate(ScriptContext ctx, World world) {
        return Math.abs((int) inputExpr.getConnectedValueOrDefault(ctx,world));
    }
}
