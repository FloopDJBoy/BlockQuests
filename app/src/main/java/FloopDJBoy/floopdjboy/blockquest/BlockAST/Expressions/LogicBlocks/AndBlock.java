package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.LogicBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class AndBlock extends Expression<Boolean> {
    private final ExpressionInput first, second;

    public AndBlock() {
        super("AndBlock", Boolean.class, Category.OPERATORS);
        DummyInput input = new DummyInput(this).addField(new LabelField(this, "and"));
        first = new ExpressionInput(this, Boolean.class);
        second = new ExpressionInput(this, Boolean.class);
        inputs.add(first);
        inputs.add(input);
        inputs.add(second);
    }

    @Override
    public Boolean evaluate(final ScriptContext ctx,final World world) {
        return (boolean) first.getConnectedValueOrDefault(ctx,world)
                && (boolean) second.getConnectedValueOrDefault(ctx,world);
    }
}
