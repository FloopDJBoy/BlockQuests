package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class RightExpression extends Expression<Actor.AbsoluteDirection> {
    public RightExpression() {
        super("RightExpression",Actor.AbsoluteDirection.class,Category.VARIABLES);
        inputs.add(new DummyInput(this).addField(new LabelField("right")));
    }

    @Override
    public Actor.AbsoluteDirection evaluate(ScriptContext ctx, World world) {
        return ctx.getActor().getFacing().turnRight();
    }
}
