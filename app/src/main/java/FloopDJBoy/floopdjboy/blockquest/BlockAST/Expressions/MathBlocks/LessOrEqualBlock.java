package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.MathBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class LessOrEqualBlock extends Expression<Boolean> {
    private final ExpressionInput first, second;

    public LessOrEqualBlock() {
        super("LessOrEqualBlock", Boolean.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField(this, "<="));
        first = new ExpressionInput(this, Integer.class);
        second = new ExpressionInput(this, Integer.class);
        inputs.add(first);
        inputs.add(input);
        inputs.add(second);
    }

    @Override
    public Boolean evaluate(ScriptContext ctx, World world) {
        return (int) first.getConnectedValueOrDefault(ctx,world)
                <= (int) second.getConnectedValueOrDefault(ctx,world);
    }
}
