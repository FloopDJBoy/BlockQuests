package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class KillDynamicActorBlock extends Statement{
    private final ExpressionInput actorInput;
    public KillDynamicActorBlock() {
        super("killDynamicActor",Category.CONTROL);
        DummyInput dummyInput = new DummyInput(this);
        actorInput = new ExpressionInput(this,Actor.class);
        inputs.add(dummyInput.addField(new LabelField("kill")));
        inputs.add(actorInput);

    }
    @SuppressWarnings("unchecked")
    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        if(actorInput.connection != null && actorInput.connection.isConnected()){
            Actor actor = ((Expression<Actor>)(actorInput.connection.getTargetBlock())).evaluate(ctx,world);
            if(actor!=null){
                world.killActor(actor);
            }

        }

        return ExecResult.DONE;
    }
}
