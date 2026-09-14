package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptEvent;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

/**
 * HatBlock represents an event-triggered entry point for a script.
 * These blocks cannot be connected to other blocks from above (no previous connection).
 * They define WHEN a script should run.
 * <p>
 * Examples:
 * - "When game starts"
 * - "When this actor is clicked"
 * - "When I receive [message]"
 * - "When [condition] is true"
 */
public abstract class HatBlock extends Statement {

    /**
     * The event type this hat block responds to
     */
    private final ScriptEvent eventType;

    public HatBlock(String name, ScriptEvent eventType) {
        super(name,Category.EVENTS);
        this.eventType = eventType;
        // Hat blocks never have a previous connection - they're entry points
        this.previousConnection = null;
    }

    /**
     * Get the event type this hat block listens for
     */
    public ScriptEvent getEventType() {
        return eventType;
    }

    /**
     * Hat blocks themselves don't execute - they just mark the entry point.
     * When executed, they immediately move to the next block.
     */
    @Override
    public ExecResult execute(final ScriptContext ctx,final World world) {
        // Hat blocks are just markers, immediately proceed to next block
        return ExecResult.DONE;
    }

    /**
     * Check if this hat block's condition is met (for conditional hats)
     * Override this for event hats that have conditions
     *
     * @return true if the script should run
     */
    public abstract boolean shouldTrigger(final ScriptContext ctx,final World world);
}