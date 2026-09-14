package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class MoveBlock extends Statement {
    public MoveBlock(){
        super("move",Category.MOTION);
        DummyInput dummyInput = new DummyInput(this).addField(new LabelField("move"));
        inputs.add(dummyInput);
    }
    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        Actor actor = ctx.getActor();
        if(!actor.hasTag(Actor.Tag.IMMOVABLE)){
            int[] pos = actor.getPositionInFront();
            world.moveActor(actor,pos[0],pos[1]);
        }
        return ExecResult.DONE;
    }
}
