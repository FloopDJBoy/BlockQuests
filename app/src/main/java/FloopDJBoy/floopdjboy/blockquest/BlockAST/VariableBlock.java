package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.DummyInput;
import FloopDJBoy.floopdjboy.blockquest.World;

public class VariableBlock extends Expression<Object> {
    private VariableDefinition variableDef;
    DummyInput label;
    public VariableBlock() {
        super("VariableBlock", Object.class, Category.VARIABLES);
        label = new DummyInput(this);
        inputs.add(label);
    }

    public void configure(VariableDefinition def) {
        this.variableDef = def;
        label.addField( new LabelField(def.name));
    }

    public VariableDefinition getVariableDef() {
        return variableDef;
    }

    @Override
    public Class<?> getReturnType() {
        if (variableDef == null) {
            throw new IllegalStateException("VariableBlock used before configure()");
        }
        return variableDef.type;
    }

    @Override
    public Object evaluate(ScriptContext ctx, World world) {
        if (variableDef == null) {
            throw new IllegalStateException("VariableBlock used before configure()");
        }

        Object value = ctx.getActor().getSpriteGroup().getVariable(variableDef.id);

        if (value == null) {
            throw new IllegalStateException("Variable '" + variableDef.id + "' is null");
        }

        if (!variableDef.type.isAssignableFrom(value.getClass())) {
            throw new IllegalStateException(
                    "Variable '" + variableDef.id + "' type mismatch. Expected "
                            + variableDef.type.getName() + " but got "
                            + value.getClass().getName()
            );
        }
        return value;
    }
}
