package FloopDJBoy.floopdjboy.blockquest.BlockSerialization;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.Field;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.Input;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Registry;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.SpriteGroup;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableDefinition;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.BlockView;
import FloopDJBoy.floopdjboy.blockquest.MainActivity;
import FloopDJBoy.floopdjboy.blockquest.Workspace;
import FloopDJBoy.floopdjboy.blockquest.World;


public class Serializer {
    public static DataTypes.SpriteGroupData serializeSpriteGroup(SpriteGroup group) {
        HashMap<SpriteGroup, ArrayList<Block>> roots = BlockView.getGlobalBlockRootsData();
        HashMap<SpriteGroup, ArrayList<Block>> blocks = BlockView.getGlobalBlockData();

        DataTypes.SpriteGroupData groupData = new DataTypes.SpriteGroupData();
        groupData.sprite = group.getSpriteResourceId();
        groupData.actors = new HashMap<>();
        groupData.blocks = new HashMap<>();
        groupData.rootBlocks = new ArrayList<>();
        groupData.localVariableDefinitions = new ArrayList<>();
        for(VariableDefinition def : group.getAllLocalVariables()){
            groupData.localVariableDefinitions.add(def.toData());
        }
        // actors
        for (Actor actor : group.getActors()) {
            DataTypes.ActorData actorData = new DataTypes.ActorData();
            actorData.id = actor.getId();
            actorData.x = actor.getX();
            actorData.y = actor.getY();
            actorData.direction = actor.getFacing().ordinal();
            actorData.sprite = actor.getSpriteResourceId();

            for (Actor.Tag tag : actor.getTags()) {
                actorData.tags.add(tag.ordinal());
            }

            groupData.actors.put(actor.getId(), actorData);
        }

        // blocks
        ArrayList<Block> groupBlocks = blocks.get(group);
        if (groupBlocks != null) {
            for (Block block : groupBlocks) {
                groupData.blocks.put(block.getId(), getBlockData(block));
            }
        }

        // roots
        ArrayList<Block> rootBlocks = roots.get(group);
        if (rootBlocks != null) {
            for (Block block : rootBlocks) {
                groupData.rootBlocks.add(block.getId());
            }
        }

        return groupData;
    }
    public static DataTypes.WorldData SerializeRaw(World world) {
        DataTypes.WorldData data = new DataTypes.WorldData();
        data.spriteGroups = new HashMap<>();
        data.width = world.getWidth();
        data.height = world.getHeight();
        data.globalVariableDefinitions = new ArrayList<>();
        for (SpriteGroup group : world.getSpriteGroups().values()) {
            data.spriteGroups.put(
                    group.getSpriteResourceId(),
                    serializeSpriteGroup(group)
            );
        }
        for(VariableDefinition def : world.getAllGlobalVariables()){
            data.globalVariableDefinitions.add(def.toData());
        }

        return data;
    }
    public static String Serialize(World world){
        world.reset();
        DataTypes.WorldData data = SerializeRaw(world);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(data);
    }
    public static void Deserialize(String jsonData, MainActivity activity){
        DeserializeData data = Deserialize(jsonData);
        activity.setWorld(data.world,data.roots,data.blocks);
    }
    public static SpriteGroup deserializeSpriteGroup(
            DataTypes.SpriteGroupData groupData,
            World world,
            Map<String, DataTypes.BlockData> allBlocks,
            HashMap<String, Block> createdBlocks,
            HashMap<SpriteGroup, ArrayList<Block>> roots,
            HashMap<String, VariableDefinition> variableRegistry
    ) throws Exception {

        // actors first
        for (DataTypes.ActorData actorData : groupData.actors.values()) {
            new Actor(
                    actorData.id,
                    actorData.x,
                    actorData.y,
                    world,
                    Actor.AbsoluteDirection.values()[actorData.direction],
                    actorData.sprite,
                    actorData.tags.stream()
                            .map(i -> Actor.Tag.values()[i])
                            .toArray(Actor.Tag[]::new)
            );
        }

        SpriteGroup group = world.getSpriteGroups().get(groupData.sprite);
        group.getAllLocalVariables().clear();

        for (DataTypes.VariableDefinitionData v : groupData.localVariableDefinitions) {
            VariableDefinition def = new VariableDefinition(
                    v.id,
                    v.name,
                    Class.forName(v.type)
            );

            group.setLocalVariable(def.id,def);
            variableRegistry.put(def.id, def);
        }

        // roots
        ArrayList<Block> rootList = new ArrayList<>();

        for (String rootId : groupData.rootBlocks) {
            DataTypes.BlockData blockData = groupData.blocks.get(rootId);
            if (blockData == null) continue;

            Block b = deserializeBlock(blockData, allBlocks, createdBlocks,variableRegistry);
            rootList.add(b);

            if (b instanceof HatBlock h) {
                group.addScript(h);
            }
        }

        if (!rootList.isEmpty()) {
            roots.put(group, rootList);
        }

        return group;
    }
    private static DeserializeData Deserialize(String jsonData) {
        try{
            DataTypes.WorldData data =
                    new Gson().fromJson(jsonData, DataTypes.WorldData.class);

            return deserializeRaw(data);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static World DeserializeRaw(DataTypes.WorldData data, MainActivity activity){
        try{
            DeserializeData deserializeData = deserializeRaw(data);
            activity.setWorld(deserializeData.world,deserializeData.roots,deserializeData.blocks);
            return deserializeData.world;
        }catch (ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }
    private static DeserializeData deserializeRaw(DataTypes.WorldData data) throws ClassNotFoundException {
        Registry.getInstance().clear();
        BlockView.clear();

        HashMap<String, DataTypes.BlockData> allBlocks = new HashMap<>();
        HashMap<String, Block> createdBlocks = new HashMap<>();
        HashMap<SpriteGroup, ArrayList<Block>> roots = new HashMap<>();
        HashMap<String, VariableDefinition> variableRegistry = new HashMap<>();
        World world = new World(data.width, data.height);
        for (DataTypes.VariableDefinitionData v : data.globalVariableDefinitions) {
            VariableDefinition def = new VariableDefinition(
                    v.id,
                    v.name,
                    Class.forName(v.type)
            );

            world.setGlobalVariable(def.id,def);
            variableRegistry.put(def.id, def);
        }
        // collect all blocks
        for (DataTypes.SpriteGroupData group : data.spriteGroups.values()) {
            allBlocks.putAll(group.blocks);
        }

        // deserialize groups
        for (DataTypes.SpriteGroupData groupData : data.spriteGroups.values()) {
            try {
                deserializeSpriteGroup(
                        groupData,
                        world,
                        allBlocks,
                        createdBlocks,
                        roots,
                        variableRegistry
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new DeserializeData(world, roots, allBlocks);
    }
    private static Block deserializeBlock(
            @NonNull DataTypes.BlockData blockData,
            Map<String, DataTypes.BlockData> allBlocks,
            HashMap<String, Block> createdBlocks,
            HashMap<String, VariableDefinition> variableRegistry
    ) throws Exception {
        if (createdBlocks.containsKey(blockData.id)) {
            return createdBlocks.get(blockData.id);
        }
        //reflection is used as I don't want to go change all current blocks to add one func
        Class<?> clazz = Class.forName(blockData.type);
        Block block = (Block) clazz.getConstructor().newInstance();
        Registry.getInstance().remove(clazz, block.getId());
        java.lang.reflect.Field idField = Block.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(block, blockData.id);
        createdBlocks.put(blockData.id, block);
        if (block instanceof VariableBlock vb) {
            VariableDefinition def = variableRegistry.get(blockData.variableId);

            if (def == null) {
                throw new IllegalStateException(
                        "Missing VariableDefinition for id: " + blockData.variableId
                );
            }
            vb.configure(def);
        }
        for (int i = 0; i < blockData.inputs.size(); i++) {
            DataTypes.InputData inputData = blockData.inputs.get(i);
            Input<?> input = block.inputs.get(i);

            if (inputData.block != null) {
                DataTypes.BlockData targetData = allBlocks.get(inputData.block);
                Block targetBlock = deserializeBlock(targetData, allBlocks, createdBlocks,variableRegistry);
                if (input instanceof ExpressionInput exprInput) {
                    exprInput.connect(targetBlock);
                } else if (input instanceof StatementInput stmt) {
                    stmt.connect(targetBlock);
                }
            }
            for(Field<?> f : input.fields){
                if(inputData.fields.containsKey(f.getName())){
                    DataTypes.FieldData fieldData = inputData.fields.get(f.getName());
                    if(f.isEditable()){
                        f.setValue(fieldData.value);
                    }
                }
            }
        }
        if(blockData.next!=null){
            DataTypes.BlockData nextData = allBlocks.get(blockData.next);
            Block nextBlock = deserializeBlock(nextData, allBlocks, createdBlocks,variableRegistry);
            if(block instanceof Statement stmt){
                stmt.connect(nextBlock, ConnectionType.NEXT_STATEMENT);
            }
        }
        return block;
    }
    private static DataTypes.BlockData getBlockData(Block block){
        if(block==null){
            return null;
        }
        DataTypes.BlockData blockData = new DataTypes.BlockData();
        blockData.id = block.getId();
        blockData.next = null;
        blockData.type = block.getClass().getName();
        BlockView bv = BlockView.getBlockViewByBlock(block);
        blockData.x = bv.getX();
        blockData.y = bv.getY();
        if (block instanceof VariableBlock vb) {
            blockData.variableId = vb.getVariableDef().id;
        }
        for (Input<?> input : block.inputs) {
            DataTypes.InputData inputData = new DataTypes.InputData();
            inputData.type = input.type.ordinal();
            inputData.block = null;
            if(input.type != InputType.DUMMY && input.type != InputType.END_ROW){
                if(input.connection != null && input.connection.isConnected()){
                    inputData.block = input.connection.getTargetBlock().getId();
                }
            }
            inputData.fields = new HashMap<>();
            for(Field<?> f : input.fields) {
                DataTypes.FieldData fieldData = new DataTypes.FieldData();
                fieldData.name = f.getName();
                fieldData.type = f.getClass().getName();
                fieldData.valueType = f.getType().getName();
                fieldData.value = f.getText();
                inputData.fields.put(f.getName(), fieldData);
            }
            blockData.inputs.add(inputData);
        }
        if(block instanceof Statement s){
            if(s.hasNext()) {
                blockData.next = s.nextConnection.getTargetBlock().getId();
            }
        }
        return blockData;
    }
    public static BlockView makeBlockViewStack(Block root, SpriteGroup group, HashMap<String, DataTypes.BlockData> blocks, Workspace workspace, Context ctx){
        DataTypes.BlockData data = blocks.get(root.getId());
        BlockView bv = new BlockView(
                ctx,
                root,
                root.getCategory().getColor(),
                data.x, data.y,
                group,
                workspace
        );
        workspace.addView(bv);
        for(Input input : root.inputs){
            if(input instanceof ExpressionInput exprInput){
                if(exprInput.connection != null && exprInput.connection.isConnected()){
                    BlockView connected = makeBlockViewStack(exprInput.connection.getTargetBlock(),group,blocks,workspace,ctx);
                    connected.connectToInput(bv,exprInput);
                }
            }else if(input instanceof StatementInput statementInput){
                if(statementInput.connection != null && statementInput.connection.isConnected()){
                    BlockView connected = makeBlockViewStack(statementInput.connection.getTargetBlock(),group,blocks,workspace,ctx);
                    connected.connectToStatementInput(bv,statementInput);
                }
            }
        }
        if(root instanceof Statement s){
            if(s.hasNext()){
                BlockView connected = makeBlockViewStack(s.nextConnection.getTargetBlock(),group,blocks,workspace,ctx);
                bv.connectNext(connected);
            }
        }
        return bv;
    }
    static class DeserializeData{
        World world;
        HashMap<SpriteGroup, ArrayList<Block>> roots;
        HashMap<String, DataTypes.BlockData> blocks;

        public DeserializeData(World world, HashMap<SpriteGroup, ArrayList<Block>> roots, HashMap<String, DataTypes.BlockData> blocks) {
            this.world = world;
            this.roots = roots;
            this.blocks = blocks;
        }
    }
}
