package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.NumberField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class NumberInputExpression extends Expression<Integer>{
    private final DummyInput input;

    public NumberInputExpression() {
        super("NumberInputExpression",Integer.class,Category.OPERATORS);
        input = (DummyInput) new DummyInput(this).addField(new NumberField(this,"value"));
        inputs.add(input);
    }

    @Override
    public Integer evaluate(ScriptContext ctx, World world) {
        return (Integer) input.fields.get(0).getValue();
    }
}
