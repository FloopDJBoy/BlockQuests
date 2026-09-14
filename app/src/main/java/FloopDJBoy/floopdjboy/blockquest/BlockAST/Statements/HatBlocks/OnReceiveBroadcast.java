package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.TextField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.World;

public class OnReceiveBroadcast extends HatBlock{
    private final TextField tx;
    public OnReceiveBroadcast() {
        super("OnReceiveBroadcast",null);
        tx = new TextField(this,"what");
        DummyInput dummyInput = new DummyInput(this).addField(new LabelField("on receive broadcast")).addField(tx);
        inputs.add(dummyInput);
    }
    @Override
    public ScriptEvent getEventType() {
        return new ScriptEvent(tx.getValue());
    }
    @Override
    public boolean shouldTrigger(ScriptContext ctx,World world) {
        return true;
    }
}
