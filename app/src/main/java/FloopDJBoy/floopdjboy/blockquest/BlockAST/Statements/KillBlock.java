package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class KillBlock extends Statement{
    public KillBlock() {
        super("kill",Category.CONTROL);
        DummyInput dummyInput = (DummyInput) new DummyInput(this);
        inputs.add(dummyInput.addField(new LabelField("kill")));
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        Actor actor = ctx.getActor();
        world.killActor(actor);
        return ExecResult.DONE;
    }
}
