package FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;

public class NumberField extends Field<Integer>{

    @Override
    public Integer getDefaultValue() {
        return 0;
    }
    public NumberField(Block sourceBlock, String name,Integer value) {

        super(sourceBlock, name, Integer.class, value);
    }
    public NumberField(Block sourceBlock, String name) {

        super(sourceBlock, name, Integer.class, 0);
        text = "0";
    }
    @Override
    public void setValue(String value) {
        sourceBlock.makeDirty();
        text = value;
        try {
            this.value = Integer.parseInt(value.trim()); // trim removes \n, spaces, etc.
        } catch (NumberFormatException e) {
            this.value = 0;
        }
    }
    @Override
    public boolean isEditable() {
        return true;
    }
}
