package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.World;

public class TurnBlock extends Statement{
    private final ExpressionInput input;
    public TurnBlock(){
        super("turn",Category.MOTION);
        input = new ExpressionInput(this,Actor.AbsoluteDirection.class).addField(new LabelField("turn"));
        inputs.add(input);
    }

    @Override
    public ExecResult execute(ScriptContext ctx, World world) {
        Actor actor = ctx.getActor();
        if(!actor.hasTag(Actor.Tag.IMMOVABLE)){
            if(input.getConnectedExpression() !=null){
                Actor.AbsoluteDirection dir = (Actor.AbsoluteDirection) input.getConnectedExpression().evaluate(ctx,world);
                actor.setFacing(dir);
            }
        }
        return ExecResult.DONE;
    }
}
