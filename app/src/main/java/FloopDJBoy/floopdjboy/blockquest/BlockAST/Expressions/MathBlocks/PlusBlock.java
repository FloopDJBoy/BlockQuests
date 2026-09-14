package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.MathBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class PlusBlock extends Expression<Integer> {
    private final ExpressionInput first,second;

    public PlusBlock() {
        super("PlusBlock",Integer.class,Category.OPERATORS);
        first = new ExpressionInput(this,Integer.class);
        second = (ExpressionInput) new ExpressionInput(this,Integer.class).addField(new LabelField("+"));
        inputs.add(first);
        inputs.add(second);
    }

    @Override
    public Integer evaluate(ScriptContext ctx, World world) {
        return ((int)first.getConnectedValueOrDefault(ctx,world)) + ((int)second.getConnectedValueOrDefault(ctx,world));
    }
}
