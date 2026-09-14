package FloopDJBoy.floopdjboy.blockquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.SpriteGroup;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableDefinition;

/**
 * World represents the game world/stage where actors exist and execute scripts.
 * It manages the grid, handles collisions, and orchestrates tick-based execution.
 */
public class World {
    private final int width;
    private final int height;
    private final ArrayList<Actor>[][] grid;
    private BlockExecutionListener blockListener;
    private LossListener lossListener;
    private final Map<Actor, InitialState> initialStates = new HashMap<>();
    private final int[][] TileBackgroundRes;

    // Track all actors in the world (including those not on grid)
    private final ArrayList<Actor> allActors;

    // Global variables (accessible to all scripts)
    private final Map<String, Object> globalVariables;
    private final Map<String, VariableDefinition> globalVariablesDefinition = new HashMap<>();
    private final Map<Integer, SpriteGroup> spriteGroups = new HashMap<>();


    // Pending moves (to handle simultaneous movement and collisions)
    private final ArrayList<PendingMove> pendingMoves;

    // Current tick number
    private int tickCount = 0;

    public void fillBackground(int resId) {
        for(int r=0;r<getHeight();++r){
            for(int c=0;c<getWidth();++c){
                setTileBackgroundRes(c,r,resId);
            }
        }
    }

    public void setGlobalVariableValue(String id,Object newValue) {
        globalVariables.put(id,newValue);
    }

    /** Listener notified when a SpriteGroup becomes empty and is removed. */
    public interface SpriteGroupRemovedListener {
        void onSpriteGroupRemoved(SpriteGroup group);
    }
    private SpriteGroupRemovedListener spriteGroupRemovedListener;

    public void setSpriteGroupRemovedListener(SpriteGroupRemovedListener l) {
        this.spriteGroupRemovedListener = l;
    }


    public void setBlockExecutionListener(BlockExecutionListener l) {
        blockListener = l;
    }

    public Actor getPlayer() {
        for(Actor actor : allActors){
            if(actor.hasTag(Actor.Tag.PLAYER)){
                return actor;
            }
        }
        return null;
    }
    public Actor getPlayer(int x, int y) {
        if(!isInBounds(x,y)) return getPlayer();
        for(Actor actor : grid[x][y]){
            if(actor.hasTag(Actor.Tag.PLAYER)){
                return actor;
            }
        }
        return getPlayer();
    }

    public void setLossListener(LossListener notifyLoss) {
        lossListener = notifyLoss;
    }

    public Map<Integer, SpriteGroup> getSpriteGroups() {
        return spriteGroups;
    }

    public interface BlockExecutionListener {
        void onBlockExecuted(Block block, int tick);
    }
    public interface LossListener {
        void onLoss();
    }

    public void notifyBlockExecuted(Block block) {
        if (blockListener != null)
            blockListener.onBlockExecuted(block, tickCount);
    }
    public void notifyLoss() {
        if (lossListener != null)
            lossListener.onLoss();
    }


    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new ArrayList[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new ArrayList<>();
            }
        }
        this.TileBackgroundRes = new int[width][height];
        this.allActors = new ArrayList<>();
        this.globalVariables = new HashMap<>();
        this.pendingMoves = new ArrayList<>();
    }
    public int getTileBackgroundRes(int x, int y){
        return TileBackgroundRes[x][y];
    }
    public void setTileBackgroundRes(int x, int y, int res){
        TileBackgroundRes[x][y]=res;
    }
    public World(int width) {
        this(width, (width*3)/4);
    }

    // ========== Tick System ==========

    /**
     * Execute one game tick.<br>
     * This is the main game loop that:<br>
     * 1. Executes ON_TICK scripts for all actors<br>
     * 2. Applies pending moves<br>
     * 3. Detects collisions<br>
     * 4. Executes ON_COLLISION scripts<br>
     * 5. Cleans up<br>
     */
    public void tick() {
        tickCount++;

        // Reset move flags before executing scripts
        for (Actor actor : allActors) {
            actor.resetMoveFlag();
        }

        for (Actor actor : allActors) {
            actor.executeScripts(ScriptEvent.ON_TICK);
        }

        for (Actor actor : allActors) {
            actor.tickActiveScripts(this);
        }

        applyPendingMoves();

        detectAndHandleCollisions();

        for (Actor actor : allActors) {
            actor.finishTick();
        }
        if(getPlayer()==null){
            notifyLoss();
        }

        pendingMoves.clear();
    }
    public void broadcast(String name) {

        ScriptEvent event = new ScriptEvent(name);

        for (Actor actor : allActors) {
            actor.executeScripts(event);
        }
    }



    /**
     * Apply all pending moves and detect conflicts
     */
    private void applyPendingMoves() {

        for (PendingMove move : pendingMoves) {

            Actor actor = move.actor;
            int newX = move.newX;
            int newY = move.newY;

            if (!isInBounds(newX, newY) || !actor.isAlive())
                continue;

            boolean blocked = false;

            for (Actor other : grid[newX][newY]) {
                if (other == actor || !other.isAlive() || !actor.isAlive()) continue;

                actor.registerCollision(other);
                other.registerCollision(actor);

                if (other.hasTag(Actor.Tag.SOLID))
                    blocked = true;
            }

            if (blocked) continue;

            // Record previous position before moving
            int fromX = actor.getX();
            int fromY = actor.getY();

            // move actor
            grid[actor.getX()][actor.getY()].remove(actor);

            actor.setX(newX);
            actor.setY(newY);

            grid[newX][newY].add(actor);

            // Notify actor it moved (starts animation)
            actor.onMoved(fromX, fromY);
        }
    }
    public void killActor(Actor actor) {
        if (!actor.isAlive()) return;

        grid[actor.getX()][actor.getY()].remove(actor);
        actor.kill();
    }


    /**
     * Detect collisions and execute ON_COLLISION scripts
     */
    private void detectAndHandleCollisions() {
        for (Actor actor : allActors) {

            for (Actor other : grid[actor.getX()][actor.getY()]) {
                if (other == actor) continue;

                actor.registerCollision(other);
            }
        }
        for (Actor actor : allActors) {

            if (actor.justCollided())
                actor.executeScripts(ScriptEvent.ON_COLLISION);

            if (actor.isColliding())
                actor.executeScripts(ScriptEvent.WHILE_COLLIDING);
        }
    }


    // ========== Actor Management ==========

    /**
     * Add an actor to the world at the specified position.
     * This is called by the Actor constructor.
     */
    public void setActor(int x, int y, Actor actor) {
        if (!isInBounds(x, y)) throw new IllegalArgumentException("Out of bounds");
        ArrayList<Actor> cell = grid[x][y];
        if (!cell.contains(actor)) cell.add(actor);
        if (!allActors.contains(actor)){
            allActors.add(actor);
            if(!spriteGroups.containsKey(actor.getSpriteResourceId())){
                spriteGroups.put(actor.getSpriteResourceId(),new SpriteGroup(actor.getSpriteResourceId(),this));
                for(Actor.Tag t : actor.getTags()) {
                    spriteGroups.get(actor.getSpriteResourceId()).addTag(t);
                }
            }
            Objects.requireNonNull(spriteGroups.get(actor.getSpriteResourceId())).addActor(actor);
        }

    }

    /**
     * Get the actor at the specified position
     */
    public ArrayList<Actor> getActors(int x, int y) {
        if (!isInBounds(x, y)) return new ArrayList<>();
        return new ArrayList<>(grid[x][y]);
    }

    /**
     * Permanently remove an actor from the world and clean up its SpriteGroup.
     * If the SpriteGroup becomes empty it is removed from the map and
     * the SpriteGroupRemovedListener is notified so the caller can clean up
     * associated BlockViews in the Workspace.
     */
    public void removeActor(Actor actor) {
        if (isInBounds(actor.getX(), actor.getY()))
            grid[actor.getX()][actor.getY()].remove(actor);

        allActors.remove(actor);
        initialStates.remove(actor);

        // Clean up SpriteGroup
        SpriteGroup group = spriteGroups.get(actor.getSpriteResourceId());
        if (group != null) {
            group.removeActor(actor);
            if (group.getActors().isEmpty()) {
                spriteGroups.remove(actor.getSpriteResourceId());
                if (spriteGroupRemovedListener != null) {
                    spriteGroupRemovedListener.onSpriteGroupRemoved(group);
                }
            }
        }
    }

    /**
     * Queue a move for an actor.
     * The move will be applied at the end of the current tick.
     * Returns true if move was queued, false if invalid.
     */
    public boolean moveActor(Actor actor, int newX, int newY) {
        if (!isInBounds(newX, newY)) {
            return false;
        }

        // Queue the move
        pendingMoves.add(new PendingMove(actor, newX, newY));
        return true;
    }

    // ========== Collision and Bounds Checking ==========

    /**
     * Check if a position is within world bounds
     */
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Check if a position contains a solid actor
     */
    public boolean isSolid(int x, int y) {
        if (!isInBounds(x, y)) return true;

        for (Actor a : grid[x][y])
            if (a.hasTag(Actor.Tag.SOLID))
                return true;

        return false;
    }

    /**
     * Check if a position is empty
     */
    public boolean isEmpty(int x, int y) {
        if (!isInBounds(x, y)) return false;
        return grid[x][y].isEmpty();
    }


    // ========== Global Variables ==========

    /**
     * Set a global variable (accessible to all scripts)
     */
    public void setGlobalVariable(String id, VariableDefinition v) {
        globalVariables.put(id,v.getDefaultValue());
        globalVariablesDefinition.put(id,v);
    }
    public ArrayList<VariableDefinition> getAllGlobalVariables(){
        return new ArrayList<>(globalVariablesDefinition.values());
    }

    /**
     * Get a global variable
     */
    public Object getGlobalVariable(String id) {
        return globalVariables.get(id);
    }

    /**
     * Check if a global variable exists
     */
    public boolean hasGlobalVariable(String name) {
        return globalVariables.containsKey(name);
    }

    // ========== Getters ==========

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTickCount() {
        return tickCount;
    }

    public ArrayList<Actor> getAllActors() {
        return new ArrayList<>(allActors); // Return a copy
    }

    /**
     * Start the game - execute ON_START scripts for all actors and reset back to initial state
     */
    public void start() {
        initialStates.clear();
        for (Actor actor : allActors) {
            initialStates.put(actor, new InitialState(actor.getX(), actor.getY()));
            actor.executeScripts(ScriptEvent.ON_START);
        }
    }
    public void reset() {

        tickCount = 0;
        pendingMoves.clear();

        // clear grid
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y].clear();

        // restore actor positions
        for (Actor actor : allActors) {

            InitialState s = initialStates.get(actor);
            actor.revive();
            if (s == null) continue;

            actor.reset(s);
            grid[s.x][s.y].add(actor);
            actor.finishTick();
        }
        //reset var values
        for(VariableDefinition v : globalVariablesDefinition.values()){
            globalVariables.put(v.id,v.getDefaultValue());
        }
        //reset sprite groups
        for(SpriteGroup s : spriteGroups.values()){
            s.reset();
        }
    }



    // ========== Helper Classes ==========
    public static class InitialState {
        final int x;
        final int y;

        InitialState(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Represents a pending move to be applied at the end of a tick
     */
    private static class PendingMove {
        final Actor actor;
        final int newX;
        final int newY;

        PendingMove(Actor actor, int newX, int newY) {
            this.actor = actor;
            this.newX = newX;
            this.newY = newY;
        }
    }
}