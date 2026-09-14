package FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs;

import java.util.ArrayList;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Connection;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.Field;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;

public abstract class Input<T extends Input<T>> {
    //connection to where the input is coming from
    public Connection connection=null;
    public ArrayList<Field<?>> fields = new ArrayList<>();
    public final InputType type;
    protected final Block sourceBlock;
    public Input(Block sourceBlock, InputType type) {
        this.sourceBlock = sourceBlock;
        this.type = type;
    }
    public T addField(Field<?> field) {
        field.setBlock(sourceBlock);
        fields.add(field);
        return (T) this;
    }
    public abstract void connect(Block block);
    public T addFieldAt(Field<?> field,int index){
        if(index<0 || index>fields.size()){
            throw new RuntimeException("Index out of bounds");
        }
        field.setBlock(sourceBlock);
        fields.add(index,field);
        return (T)this;
    }
    public Block getSourceBlock() {
        return sourceBlock;
    }

}
