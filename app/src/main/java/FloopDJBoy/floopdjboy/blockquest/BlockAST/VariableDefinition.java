package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import androidx.annotation.NonNull;

import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.DataTypes;

public final class VariableDefinition {
    public final String id;
    public String name;
    public final Class<?> type;

    public VariableDefinition(String id, String name, Class<?> type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
    public DataTypes.VariableDefinitionData toData() {
        DataTypes.VariableDefinitionData d = new DataTypes.VariableDefinitionData();
        d.id = id;
        d.name = name;
        d.type = type.getName();
        return d;
    }
    public Object getDefaultValue() {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';

        // wrapper classes if you want boxed defaults
        if (type == Boolean.class) return false;
        if (type == Byte.class) return (byte) 0;
        if (type == Short.class) return (short) 0;
        if (type == Integer.class) return 0;
        if (type == Long.class) return 0L;
        if (type == Float.class) return 0f;
        if (type == Double.class) return 0d;
        if (type == Character.class) return '\0';

        if (type == String.class) return "";

        // all other reference types
        return null;
    }
    @NonNull
    @Override
    public String toString() {
        return "VariableDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
