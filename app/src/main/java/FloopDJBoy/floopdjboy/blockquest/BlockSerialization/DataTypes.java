package FloopDJBoy.floopdjboy.blockquest.BlockSerialization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataTypes {
    public static class BlockData{
        String id;
        //getClass().getName()
        String type;
        ArrayList<InputData> inputs = new ArrayList<>();
        //id of next block
        String next;
        //ui cords relative to workspace
        public float x;
        public float y;
        public String variableId;

    }
    public static class CachedCategory {
        public int category;
        public List<String> blocks;

        public CachedCategory() {
            blocks = new ArrayList<>();
        }

        public CachedCategory(int category) {
            this.category = category;
            this.blocks = new ArrayList<>();
        }
    }
    public static class InputData {
        //InputType enum as ordinal
        int type;
        //if not dummy or end row and is connected.
        String block;
        //string is the name. from field.getName()
        Map<String,FieldData> fields;
    }
    public static class FieldData{
        //name of field
        String name;
        //getClass().getName()
        String type;
        //getClass().getName() of the value
        String valueType;
        //the value stored in text.
        String value;
    }
    public static class ActorData{
        String id;
        int x;
        int y;
        int sprite;
        //ordinal of the tags
        ArrayList<Integer> tags = new ArrayList<>();
        //ordinal of the direction
        int direction;

    }
    public static class SpriteGroupData{
        int sprite;
        HashMap<String,ActorData> actors;
        HashMap<String,BlockData> blocks;
        List<String> rootBlocks;
        public List<VariableDefinitionData> localVariableDefinitions = new ArrayList<>();

    }
    public static class VariableDefinitionData {
        public String id;
        public String name;
        public String type; // store Class.getName()
    }
    public static class WorldData{
        HashMap<Integer,SpriteGroupData> spriteGroups;
        int width;
        int height;
        public List<VariableDefinitionData> globalVariableDefinitions = new ArrayList<>();

    }
}
