package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.MathBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class DivideBlock extends Expression<Integer> {
    private final ExpressionInput first, second;

    public DivideBlock() {
        super("DivideBlock", Integer.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField(this, "/"));
        first = new ExpressionInput(this, Integer.class);
        second = new ExpressionInput(this, Integer.class);
        inputs.add(first);
        inputs.add(input);
        inputs.add(second);
    }

    @Override
    public Integer evaluate(final ScriptContext ctx,final World world) {
        int b = (int) second.getConnectedValueOrDefault(ctx,world);
        if (b == 0) return 0; // the show must go on?
        return (int) first.getConnectedValueOrDefault(ctx,world) / b;
    }
}
