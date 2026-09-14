package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.World;

public class OnCollideWith extends HatBlock {
    private final ExpressionInput actorInput;
    public OnCollideWith() {
        super("OnCollideWith", ScriptEvent.ON_COLLISION);
        DummyInput dummyInput = new DummyInput(this);
        actorInput = new ExpressionInput(this,Actor.class);
        inputs.add(dummyInput.addField(new LabelField("On Collide With")));
        inputs.add(actorInput);
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldTrigger(ScriptContext ctx, World world) {
        Actor actor = ctx.getActor();
        if (actor == null) return false;

        if (actorInput.connection != null && actorInput.connection.isConnected()) {
            Actor target = ((Expression<Actor>)(actorInput.connection.getTargetBlock()))
                    .evaluate(ctx, world);
            boolean val = actor.collidedLastTickWith(target) &&
                    actor.getX() == target.getX() &&
                    actor.getY() == target.getY();
            // check collisions from previous tick
            return actor.collidedLastTickWith(target) &&
                    actor.getX() == target.getX() &&
                    actor.getY() == target.getY();
        } else {
            // trigger for any collision from previous tick
            return !actor.collisionsPreviousTick.isEmpty();
        }
    }
}
