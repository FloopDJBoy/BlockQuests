package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class LeftExpression extends Expression<Actor.AbsoluteDirection> {
    public LeftExpression() {
        super("LeftBlock",Actor.AbsoluteDirection.class,Category.VARIABLES);
        inputs.add(new DummyInput(this).addField(new LabelField("left")));
    }

    @Override
    public Actor.AbsoluteDirection evaluate(final ScriptContext ctx,final World world) {
        return ctx.getActor().getFacing().turnLeft();
    }
}
