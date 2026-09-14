package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.FlowControlStatments;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

public class IfElseBlock extends Statement {

    private final ExpressionInput conditionInput;
    private final StatementInput thenInput;
    private final StatementInput elseInput;


    public IfElseBlock() {
        super("If Else", Category.CONTROL);

        DummyInput header = new DummyInput(this)
                .addField(new LabelField("if"));

        conditionInput = new ExpressionInput(this, Boolean.class);

        inputs.add(header);
        inputs.add(conditionInput);

        thenInput = new StatementInput(this);
        DummyInput elseHeader = new DummyInput(this).addField(new LabelField("else"));
        elseInput = new StatementInput(this);

        inputs.add(thenInput);
        inputs.add(elseHeader);
        inputs.add(elseInput);
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {

        ScriptContext.FlowControlFrame frame = ctx.peekFlowControlFrame();

        // First entry
        if (frame == null || frame.flowControlBlock != this) {
            frame = ctx.pushFlowControlFrame(this);

            boolean condition = false;

            if (conditionInput.connection != null &&
                    conditionInput.connection.isConnected()) {

                Boolean value =
                        ((Expression<Boolean>) conditionInput.connection.getTargetBlock())
                                .evaluate(ctx, world);

                condition = value != null && value;
            }

            frame.initialized = true;

            StatementInput branch = condition ? thenInput : elseInput;

            if (branch.connection != null &&
                    branch.connection.isConnected()) {

                Statement first =
                        (Statement) branch.connection.getTargetBlock();

                ctx.jumpTo(first);
                return ExecResult.RUNNING;
            }

            // Selected branch is empty
            ctx.popFlowControlFrame();
            return ExecResult.DONE;
        }

        // Returned from selected branch
        ctx.popFlowControlFrame();
        return ExecResult.DONE;
    }
}
