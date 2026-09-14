package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.LogicBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class NotBlock extends Expression<Boolean> {
    private final ExpressionInput inputExpr;

    public NotBlock() {
        super("NotBlock", Boolean.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField(this, "not"));
        inputExpr = new ExpressionInput(this, Boolean.class);
        inputs.add(input);
        inputs.add(inputExpr);
    }

    @Override
    public Boolean evaluate(ScriptContext ctx, World world) {
        return !(boolean) inputExpr.getConnectedValueOrDefault(ctx, world);
    }
}
