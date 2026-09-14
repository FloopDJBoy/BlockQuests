package FloopDJBoy.floopdjboy.blockquest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Registry;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.SpriteGroup;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;

/**
 * Actor represents an object in the world that can execute scripts.
 * Each actor has a position, facing direction, tags, and a collection of scripts
 * that can be triggered by different events.
 */
public class Actor {
    private final AbsoluteDirection initialFacing;
    private boolean alive = true;

    // ========== Animation State ==========

    /** Tile position before the last move (for interpolation) */
    private int prevX;

    public int getPrevX() {
        return prevX;
    }

    public int getPrevY() {
        return prevY;
    }

    private int prevY;

    /** System time (ms) when the current move animation started. -1 = not animating. */
    private long moveStartTime = -1;

    /** Whether this actor moved during the last tick */
    private boolean moved = false;

    public HashSet<Tag> getTags() {
        return tags;
    }

    /**
     * Called by World when a move is successfully applied for this actor.
     * Records the previous position and starts the animation clock.
     */
    public void onMoved(int fromX, int fromY) {
        prevX = fromX;
        prevY = fromY;
        moveStartTime = System.currentTimeMillis();
        moved = true;
    }

    public long getMoveStartTime() {
        return moveStartTime;
    }

    public boolean hasMoved() {
        return moved;
    }

    /** Called at the start of each tick to reset the moved flag */
    public void resetMoveFlag() {
        moved = false;
    }

    // ========== End Animation State ==========

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isAlive() {
        return alive;
    }
    public void kill() {
        alive = false;
    }
    public void revive() {
        alive = true;
    }

    /**
     * Check if this actor has a script for a given event
     */
    public boolean hasScript(HatBlock h) {
        return spriteGroup.hasScript(h);
    }
    public int[] GetPosition(){
        return new int[]{x,y};
    }
    public void finishTick() {
        collisionsPreviousTick.clear();
        collisionsPreviousTick.addAll(collisionsThisTick); // store for next tick
        collisionsLastTick.clear();
        collisionsLastTick.addAll(collisionsThisTick);
        collisionsThisTick.clear();
    }

    public void removeScript(HatBlock hatBlock) {
        spriteGroup.removeScript(hatBlock);
    }


    /**
     * Tags that define actor behavior and properties
     */
    public enum Tag {
        PLAYER,     // Can be scripted by player in edit mode
        SOLID,      // Cannot have another solid object on it
        IMMOVABLE   // Cannot move via MOVE or TURN commands
    }

    /**
     * Absolute directions in the world grid
     */
    public enum AbsoluteDirection {
        UP, LEFT, RIGHT, DOWN;

        /**
         * Get the x,y offset for moving in this direction
         * @return [dx, dy] where UP is -y, DOWN is +y, RIGHT is +x, LEFT is -x
         */
        public int[] getOffsetForFacing() {
            return switch (this) {
                case UP    -> new int[]{0, -1};
                case DOWN  -> new int[]{0,  1};
                case RIGHT -> new int[]{1,  0};
                case LEFT  -> new int[]{-1, 0};
            };
        }

        public AbsoluteDirection turnLeft() {
            return switch (this) {
                case UP    -> LEFT;
                case LEFT  -> DOWN;
                case DOWN  -> RIGHT;
                case RIGHT -> UP;
            };
        }

        public AbsoluteDirection turnRight() {
            return switch (this) {
                case UP    -> RIGHT;
                case RIGHT -> DOWN;
                case DOWN  -> LEFT;
                case LEFT  -> UP;
            };
        }

        /**
         * Returns the frame index (0-based) for this direction in the sprite strip.
         * Strip order: right=0, up=1, left=2, down=3
         */
        public int getSpriteFrameIndex() {
            return switch (this) {
                case RIGHT -> 0;
                case UP    -> 1;
                case LEFT  -> 2;
                case DOWN  -> 3;
            };
        }
    }


    // World reference
    private final World world;

    // Position and orientation
    private int x, y;

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    private AbsoluteDirection facing;

    // Identity
    private final String id;
    private final HashSet<Tag> tags;

    // Scripts attached to this actor, organized by event type
    private final SpriteGroup spriteGroup;

    private final ArrayList<ScriptContext> activeScripts = new ArrayList<>();



    // Variables local to this actor (for future use)

    private final HashSet<Actor> collisionsThisTick = new HashSet<>();
    private final HashSet<Actor> collisionsLastTick = new HashSet<>();

    private int spriteResourceId;
    public final HashSet<Actor> collisionsPreviousTick = new HashSet<>();

    public boolean collidedLastTickWith(Actor other) {
        return collisionsPreviousTick.contains(other);
    }

    public int getSpriteResourceId() {
        return spriteResourceId;
    }

    public void setSpriteResourceId(int spriteResourceId) {
        this.spriteResourceId = spriteResourceId;
    }
    public void reset(World.InitialState s){
        //for now just reset the dir
        this.facing = initialFacing;
        activeScripts.clear();
        prevX = x;
        prevY = y;
        x = s.x;
        y = s.y;
        moveStartTime = -1;
        moved = false;
    }
    public Actor(String id,int x, int y, World world, AbsoluteDirection facing, int spriteResourceId,Tag... tags) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.initialFacing = facing;
        this.world = world;
        this.facing = facing;
        this.tags = new HashSet<>();
        this.spriteResourceId = spriteResourceId;
        // Initialize event script lists
        this.id = id;
        Registry.getInstance().register(Actor.class,this,id);
        for (Tag tag : tags) {
            addTag(tag);
        }
        world.setActor(x, y, this);
        spriteGroup = world.getSpriteGroups().get(spriteResourceId);

    }
    public Actor(int x, int y, World world, AbsoluteDirection facing, int spriteResourceId) {
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
        this.initialFacing = facing;
        this.world = world;
        this.facing = facing;
        this.tags = new HashSet<>();
        this.spriteResourceId = spriteResourceId;
        // Initialize event script lists
        world.setActor(x, y, this);
        spriteGroup = world.getSpriteGroups().get(spriteResourceId);
        id = Registry.getInstance().register(Actor.class, this);
    }

    public Actor(int x, int y, World world, AbsoluteDirection facing, int spriteResourceId,Iterable<Tag> tags) {
        this(x, y, world, facing, spriteResourceId);
        for (Tag tag : tags) {
            addTag(tag);
        }
    }

    public Actor(int x, int y, World world, AbsoluteDirection facing, int spriteResourceId,Tag... tags) {
        this(x, y, world, facing, spriteResourceId);
        for (Tag tag : tags) {
            addTag(tag);
        }
    }

    // ========== Script Management ==========

    /**
     * Attach a script to this actor for a specific event
     * @param scriptHead The first block in the script chain
     */
    public void addScript(HatBlock scriptHead) {
        spriteGroup.addScript(scriptHead);
    }


    public SpriteGroup getSpriteGroup() {
        return spriteGroup;
    }

    /**
     * Execute all scripts for a given event.
     * This is called by the World during its tick cycle.
     *
     * @param event The event that occurred
     */
    public void executeScripts(ScriptEvent event) {
        if(!alive) return;
        for (ScriptContext template : spriteGroup.getScriptTemplates()) {
            HatBlock hat = template.getHead();

            if (hat.getEventType().equals(event)) {
                activeScripts.add(new ScriptContext(template.getHead(), this));
            }
        }
    }
    public void tickActiveScripts(World world) {
        if(!alive) return;
        Iterator<ScriptContext> it = activeScripts.iterator();

        while (it.hasNext()) {
            ScriptContext ctx = it.next();
            boolean running = ctx.tick(world);

            if (!running)
                it.remove();
        }
    }


    // ========== Movement and Position ==========

    /**
     * Turn the actor to face a new direction
     * @param newFacing The direction to face
     */
    public void turn(AbsoluteDirection newFacing) {
        if (!hasTag(Tag.IMMOVABLE)) {
            this.facing = newFacing;
        }
    }

    // ========== Collision Handling ==========

    public void registerCollision(Actor other) {
        collisionsThisTick.add(other);
    }

    public boolean isColliding() {
        return !collisionsThisTick.isEmpty();
    }

    public boolean justCollided() {
        for (Actor a : collisionsThisTick)
            if (!collisionsLastTick.contains(a))
                return true;
        return false;
    }

    public boolean stoppedColliding() {
        for (Actor a : collisionsLastTick)
            if (!collisionsThisTick.contains(a))
                return true;
        return false;
    }
    // ========== Tags ==========

    public void addTag(Tag tag) {
        tags.add(tag);
    }
    public void addTag(Iterable<Tag> tags){
        for(Tag t : tags){
            addTag(t);
        }
    }

    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    // ========== Getters and Setters ==========

    public AbsoluteDirection getFacing() {
        return facing;
    }

    public void setFacing(AbsoluteDirection facing) {
        this.facing = facing;
    }

    public String getId() {
        return id;
    }

    public World getWorld() {
        return world;
    }

    /**
     * Get position in front of this actor
     * @return [x, y] coordinates
     */
    public int[] getPositionInFront() {
        int[] offset = facing.getOffsetForFacing();
        return new int[]{x + offset[0], y + offset[1]};
    }
}