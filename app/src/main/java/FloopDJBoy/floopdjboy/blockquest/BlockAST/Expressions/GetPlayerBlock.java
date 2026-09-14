package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class GetPlayerBlock extends Expression<Actor> {
    public GetPlayerBlock() {
        super("getPlayer",Actor.class,Category.VARIABLES);
        DummyInput dummyInput = new DummyInput(this);
        inputs.add(dummyInput.addField(new LabelField("player")));
    }

    @Override
    public Actor evaluate(final ScriptContext ctx,final World world) {
        Actor actor = ctx.getActor();
        return world.getPlayer(actor.getX(),actor.getY());
    }
}
