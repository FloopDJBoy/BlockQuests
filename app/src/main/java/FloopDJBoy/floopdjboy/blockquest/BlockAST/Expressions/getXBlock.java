package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class getXBlock extends Expression<Integer>{

    public getXBlock() {
        super("getXBlock", Integer.class,Category.VARIABLES);
        DummyInput i = new DummyInput(this).addField(new LabelField("x"));
        inputs.add(i);
    }

    @Override
    public Integer evaluate(final ScriptContext ctx,final World world) {
        return ctx.getActor().getX();
    }
}
