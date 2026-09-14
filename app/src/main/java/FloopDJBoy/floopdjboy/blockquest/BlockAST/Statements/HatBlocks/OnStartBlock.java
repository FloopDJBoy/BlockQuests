package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.World;

public class OnStartBlock extends HatBlock{
    public OnStartBlock() {
        super("On Start", ScriptEvent.ON_START);
        DummyInput dummyInput = new DummyInput(this).addField(new LabelField("On Start"));
        inputs.add(dummyInput);
    }


    @Override
    public boolean shouldTrigger(ScriptContext ctx, World world) {
        return true;
    }
}
