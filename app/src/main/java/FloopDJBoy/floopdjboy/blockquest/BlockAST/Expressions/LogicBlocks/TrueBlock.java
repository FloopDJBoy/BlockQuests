package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.LogicBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class TrueBlock extends Expression<Boolean> {


    public TrueBlock() {
        super("TrueBlock", Boolean.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField("true"));
        inputs.add(input);
    }

    @Override
    public Boolean evaluate(ScriptContext ctx, World world) {
        return true;
    }
}
