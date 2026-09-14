package FloopDJBoy.floopdjboy.blockquest.BlockAST;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;
import FloopDJBoy.floopdjboy.blockquest.World;

/**
 * SpriteGroup represents a shared script owner for all actors that share the same sprite.
 * All actors with the same spriteResourceId belong to the same SpriteGroup.
 * <p>
 * The group owns the script templates (the AST hat blocks and their ScriptContext templates).
 * Each individual Actor owns its own activeScripts (running ScriptContext instances).
 */
public class SpriteGroup {

    private final int spriteResourceId;
    private final World world;
    private final ArrayList<Actor> actors = new ArrayList<>();
    private final ArrayList<ScriptContext> scriptTemplates = new ArrayList<>();
    private final HashMap<String,Object> localVariables = new HashMap<>();
    private final HashMap<String,VariableDefinition> localVariableDefinitions = new HashMap<>();
    private final HashSet<Actor.Tag> tags = new HashSet<>();
    public boolean addActor(@NonNull Actor actor) {
        if(actor.getSpriteResourceId()==spriteResourceId){
            actors.add(actor);
            actor.addTag(tags);
            return true;
        }else{
            return false;
        }
    }
    public Actor getActor(int i){
        Actor a;
        try{
            a = actors.get(i);

        }catch (IndexOutOfBoundsException e) {
            return null;
        }
        return a;
    }
    public void removeTag(Actor.Tag tag) {
        for (Actor actor : actors) {
            actor.removeTag(tag);
        }
    }
    public boolean hasTag(Actor.Tag tag){
        return tags.contains(tag);
    }
    public void addTag(Actor.Tag tag){
        for(Actor actor : actors){
            actor.addTag(tag);
        }
        tags.add(tag);
    }
    public void removeActor(Actor actor) {
        actors.remove(actor);
    }
    public ArrayList<Actor> getActors() {
        return actors;
    }
    public SpriteGroup(int spriteResourceId, World world) {
        this.spriteResourceId = spriteResourceId;
        this.world = world;
    }

    public int getSpriteResourceId() {
        return spriteResourceId;
    }
    public void removeScript(HatBlock hatBlock) {
        scriptTemplates.removeIf(ctx -> ctx.getHead() == hatBlock);
    }

    /**
     * Add a script to this group using the given hat block as the entry point.
     * A representative actor is needed to create the ScriptContext template.
     * Any actor from this group can serve as the template actor — execution
     * always spawns fresh ScriptContext instances per-actor anyway.
     */
    public void addScript(HatBlock scriptHead) {
        if(!actors.isEmpty()){
            scriptTemplates.add(new ScriptContext(scriptHead, actors.get(0)));
        }
    }

    /**
     * Check if this group already has a script for the given hat block.
     */
    public boolean hasScript(HatBlock h) {
        return scriptTemplates.stream().anyMatch(ctx -> ctx.getHead() == h);
    }

    /**
     * Get all script templates owned by this group.
     */
    public ArrayList<ScriptContext> getScriptTemplates() {
        return scriptTemplates;
    }

    public void setLocalVariable(@NonNull String id,@NonNull VariableDefinition v) {
        localVariableDefinitions.put(id,v);
        localVariables.put(id, v.getDefaultValue());
    }
    public void setVariableValue(@NonNull String id,@NonNull Object newValue){
        if(world.getGlobalVariable(id)!=null){
            world.setGlobalVariableValue(id,newValue);
        }else{
            setLocalVariableValue(id,newValue);
        }
    }

    private void setLocalVariableValue(@NonNull String id,Object newValue) {
        localVariables.put(id,newValue);
    }

    public void addVariable(boolean global, VariableDefinition v){
        if(global){
            world.setGlobalVariable(v.id,v);
        }else{
            setLocalVariable(v.id,v);
        }
    }
    public Object getVariable(@NonNull String id){
        if(world.getGlobalVariable(id)!=null){
            return world.getGlobalVariable(id);
        }else{
            return getLocalVariable(id);
        }
    }
    public ArrayList<VariableDefinition> getAllActiveVariables(){
        ArrayList<VariableDefinition> vars = new ArrayList<>();
        vars.addAll(getAllLocalVariables());
        vars.addAll(world.getAllGlobalVariables());
        return vars;
    }
    public ArrayList<VariableDefinition> getAllLocalVariables(){
        return new ArrayList<>(localVariableDefinitions.values());
    }

    public Object getLocalVariable(String id) {
        return localVariables.get(id);
    }

    public void reset() {
        for(VariableDefinition v : localVariableDefinitions.values()){
            localVariables.put(v.id,v.getDefaultValue());
        }
    }
}
