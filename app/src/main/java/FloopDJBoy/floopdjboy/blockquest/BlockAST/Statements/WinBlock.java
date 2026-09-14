package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class WinBlock extends Statement{
    public WinBlock() {
        super("WinBlock",Category.LOOKS);
        DummyInput di = new DummyInput(this);
        inputs.add(di.addField(new LabelField("Win")));
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        return ExecResult.DONE;
    }
}
