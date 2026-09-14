package FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableBlock;
import FloopDJBoy.floopdjboy.blockquest.World;

public class setVariableBlock extends Statement {
    ExpressionInput var, val;
    public setVariableBlock() {
        super("setVariableBlock", Category.OPERATORS);
        var = new ExpressionInput(this, Object.class).addField(new LabelField("set"));
        val = new ExpressionInput(this, Object.class).addField(new LabelField("to"));
        inputs.add(var);
        inputs.add(val);
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        if(var.getConnectedExpression() instanceof VariableBlock varBlock){
            if(val.getConnectedExpression().getReturnType()==varBlock.getReturnType()){
                ctx.getActor().getSpriteGroup().setVariableValue(varBlock.getVariableDef().id,val.getConnectedExpression().evaluate(ctx,world));
            }
        }
        return ExecResult.DONE;
    }
}
