package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.World;

public class OnCollide extends HatBlock {
    private final DummyInput dummyInput;
    public OnCollide() {
        super("On Collide", ScriptEvent.ON_COLLISION);
        dummyInput = (DummyInput) new DummyInput(this).addField(new LabelField("On Collide"));
        inputs.add(dummyInput);
    }

    @Override
    public boolean shouldTrigger(ScriptContext ctx, World world) {
        return true;
    }
}
