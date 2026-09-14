package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.LogicBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class FalseBlock extends Expression<Boolean> {


    public FalseBlock() {
        super("FalseBlock", Boolean.class, Category.OPERATORS);
        DummyInput input = (DummyInput) new DummyInput(this).addField(new LabelField("false"));
        inputs.add(input);
    }

    @Override
    public Boolean evaluate(final ScriptContext ctx,final World world) {
        return false;
    }
}
