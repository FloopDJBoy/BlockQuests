package FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;

public class TextField extends Field<String>{

    @Override
    public String getDefaultValue() {
        return "";
    }
    public TextField(Block sourceBlock, String name) {

        super(sourceBlock, name, String.class, "");
        text = getDefaultValue();
    }
    @Override
    public void setValue(String value) {
        sourceBlock.makeDirty();
        text = value;
        this.value = value;
    }

    @Override
    public boolean isEditable() {
        return true;
    }
}
