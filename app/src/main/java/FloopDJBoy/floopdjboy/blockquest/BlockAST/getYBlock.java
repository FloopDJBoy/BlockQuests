package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.World;

public class getYBlock extends Expression<Integer> {

    public getYBlock() {
        super("getYBlock", Integer.class,Category.VARIABLES);
        DummyInput i = new DummyInput(this).addField(new LabelField("y"));
        inputs.add(i);
    }

    @Override
    public Integer evaluate(ScriptContext ctx, World world) {
        return ctx.getActor().getY();
    }
}
