package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.MathBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class PowerBlock extends Expression<Integer> {
    private final ExpressionInput base, exponent;

    public PowerBlock() {
        super("PowerBlock", Integer.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField(this, "^"));
        base = new ExpressionInput(this, Integer.class);
        exponent = new ExpressionInput(this, Integer.class);
        inputs.add(base);
        inputs.add(input);
        inputs.add(exponent);
    }

    @Override
    public Integer evaluate(ScriptContext ctx, World world) {
        return (int) Math.pow(
                (int) base.getConnectedValueOrDefault(ctx,world),
                (int) exponent.getConnectedValueOrDefault(ctx,world)
        );
    }
}
