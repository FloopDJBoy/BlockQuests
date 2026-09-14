package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.TextField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

public class BroadcastBlock extends Statement {
    private final TextField tx;
    public BroadcastBlock() {
        super("Broadcast",Category.EVENTS);
        tx = new TextField(this,"what");
        DummyInput dummyInput = new DummyInput(this).addField(new LabelField("broadcast")).addField(tx);
        inputs.add(dummyInput);
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        if(tx.getValue()!=null && !tx.getValue().isEmpty()){
            world.broadcast(tx.getValue());
        }
        return ExecResult.DONE;
    }
}
