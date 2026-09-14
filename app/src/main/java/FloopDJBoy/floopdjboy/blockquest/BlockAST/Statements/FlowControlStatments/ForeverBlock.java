package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.FlowControlStatments;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

public class ForeverBlock extends Statement {
    private final StatementInput bodyInput;
    public ForeverBlock() {
        super("Forever", Category.CONTROL);
        DummyInput header = (DummyInput) new DummyInput(this)
                .addField(new LabelField("Forever"));
        inputs.add(header);
        bodyInput = new StatementInput(this);
        inputs.add(bodyInput);
    }


    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        ScriptContext.FlowControlFrame frame = ctx.peekFlowControlFrame();

        // First time entering loop
        if (frame == null || frame.flowControlBlock != this) {
            frame = ctx.pushFlowControlFrame(this);
            frame.limit = 1;
            frame.counter = 0;
            frame.initialized = true;
        }

        // Loop finished
        if (frame.counter >= frame.limit) {
            ctx.popFlowControlFrame();
            return ExecResult.DONE;
        }

        // Run body
        if (bodyInput.connection.isConnected()) {
            Statement first =
                    (Statement) bodyInput.connection.getTargetBlock();

            ctx.jumpTo(first);
        }

        return ExecResult.RUNNING;
    }
}
