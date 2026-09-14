package FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields;

import android.util.Log;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;

public abstract class Field<T>{
    private final Class<T> type;
    //ui name of the field
    public final String name;
    protected Block sourceBlock=null;

    public Block getSourceBlock() {
        return sourceBlock;
    }

    protected T value;
    //the text rendered on the field
    protected String text="";
    public Field(Block sourceBlock, String name, Class<T> type) {
        assert type.isAssignableFrom(getDefaultValue().getClass());
        this.sourceBlock = sourceBlock;
        this.name = name;
        this.type = type;
        value = getDefaultValue();
    }
    public Field(Block sourceBlock, String name, Class<T> type, T value) {
        this(sourceBlock,name,type);
        this.value = value;
    }
    public Field(String name, Class<T> type) {
        this(null,name,type);
    }
    public Class<T> getType() {
        return type;
    }

    /**
     * To edit values for editable fields programmatically you may trust those values
     * @param value
     */
    public void setValue(T value) {
        sourceBlock.makeDirty();
        if(isEditable()){
            this.value = value;
        }else{
            Log.d("error","attempt to set value of non-editable field "+ name);
        }
    }

    /**
     * For editable values this is the value sent by the user
     * @param value the string value of the user input. it is up to the implementation to handle input serialization
     */
    public void setValue(String value) {
        sourceBlock.makeDirty();
    }

    public T getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }
    public abstract T getDefaultValue();
    public void setBlock(Block block) {
        assert block != null;
        this.sourceBlock = block;
    }
    //by default fields are not editable
    public boolean isEditable() {
        return false;
    }
}
