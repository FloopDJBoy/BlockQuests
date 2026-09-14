package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.FlowControlStatments;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

public class RepeatBlock extends Statement {

    private final ExpressionInput countInput;
    private final StatementInput bodyInput;

    public RepeatBlock() {
        super("Repeat", Category.CONTROL);

        DummyInput header = new DummyInput(this)
                .addField(new LabelField("repeat"));

        countInput = new ExpressionInput(this, Number.class);

        inputs.add(header);
        inputs.add(countInput);

        bodyInput = new StatementInput(this);
        inputs.add(bodyInput);
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {

        ScriptContext.FlowControlFrame frame = ctx.peekFlowControlFrame();

        // First time entering loop
        if (frame == null || frame.flowControlBlock != this) {
            frame = ctx.pushFlowControlFrame(this);

            if(countInput.connection != null && countInput.connection.isConnected()){
                Integer count = ((Expression<Integer>)(countInput.connection.getTargetBlock())).evaluate(ctx,world);
                if(count == null || count<=0){
                    return ExecResult.ERROR;
                }
                frame.limit = count;
                frame.counter = 0;
                frame.initialized = true;

            }
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

            frame.counter++;
            ctx.jumpTo(first);

            return ExecResult.RUNNING;
        }

        frame.counter++;
        return ExecResult.RUNNING;
    }
}