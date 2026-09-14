package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.FlowControlStatments;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

public class IfBlock extends Statement {
    private final ExpressionInput boolInput;
    private final StatementInput bodyInput;
    public IfBlock() {
        super("IfBlock",Category.CONTROL);
        DummyInput header = new DummyInput(this)
                .addField(new LabelField("If"));

        boolInput = new ExpressionInput(this, Boolean.class);

        inputs.add(header);
        inputs.add(boolInput);

        bodyInput = new StatementInput(this);
        inputs.add(bodyInput);
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {

        ScriptContext.FlowControlFrame frame = ctx.peekFlowControlFrame();

        // First entry
        if (frame == null || frame.flowControlBlock != this) {
            frame = ctx.pushFlowControlFrame(this);

            boolean condition = false;

            if (boolInput.connection != null &&
                    boolInput.connection.isConnected()) {

                Boolean value =
                        ((Expression<Boolean>) boolInput.connection.getTargetBlock())
                                .evaluate(ctx, world);

                condition = value != null && value;
            }

            frame.initialized = true;

            // Condition false -> skip body
            if (!condition) {
                ctx.popFlowControlFrame();
                return ExecResult.DONE;
            }

            // Condition true -> jump into body
            if (bodyInput.connection != null &&
                    bodyInput.connection.isConnected()) {

                Statement first =
                        (Statement) bodyInput.connection.getTargetBlock();

                ctx.jumpTo(first);
                return ExecResult.RUNNING;
            }

            // No body attached
            ctx.popFlowControlFrame();
            return ExecResult.DONE;
        }

        // Returned from body -> finished
        ctx.popFlowControlFrame();
        return ExecResult.DONE;
    }
}
