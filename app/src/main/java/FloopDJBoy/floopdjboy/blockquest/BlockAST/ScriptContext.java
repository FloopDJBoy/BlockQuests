package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import java.util.Stack;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.World;

/**
 * ScriptContext represents a single running instance of a script.
 * Each script instance tracks its own execution state, including which block
 * is currently executing and the flow control stack for nested blocks.
 */
public class ScriptContext {

    /**
     * The first block in the script chain
     */
    private final HatBlock scriptHead;

    /**
     * The actor this script is running on
     */
    private final Actor actor;

    /**
     * The current block being executed (null if script is finished)
     */
    private Statement currentBlock;

    /**
     * Stack to track flow control blocks (if, repeat, etc.)
     * When a flow control block returns RUNNING, we push it here
     * so we know to continue executing it on the next tick
     */
    private final Stack<FlowControlFrame> flowControlStack;

    /**
     * Total number of blocks executed (for infinite loop detection)
     */
    private int executionCount = 0;

    /**
     * Maximum blocks that can execute before assuming infinite loop
     */
    private static final int MAX_EXECUTION_COUNT = 10000;

    /**
     * Whether this script has finished executing
     */
    private boolean isFinished = false;

    /**
     * Whether this script encountered an error
     */
    private boolean hasError = false;

    public ScriptContext(HatBlock scriptHead, Actor actor) {
        this.scriptHead = scriptHead;
        this.actor = actor;
        this.currentBlock = scriptHead;
        this.flowControlStack = new Stack<>();
    }

    /**
     * Execute one tick of this script.
     * This will execute one block and advance the instruction pointer.
     *
     * @param world The world in which the script is executing
     * @return true if the script is still running, false if finished or error
     */
    public boolean tick(World world) {
        if (isFinished || hasError) {
            return false;
        }
        //Log.d("ScriptContext", "Ticking script");
        if(currentBlock instanceof HatBlock head){
            if(!head.shouldTrigger(this,world)){
                return false;
            }
            advanceToNextBlock();// skip hat blocks
        }
        if (currentBlock == null) {
            isFinished = true;
            return false;
        }
        // Check for infinite loop
        executionCount++;
        if (executionCount > MAX_EXECUTION_COUNT) {
            hasError = true;
            return false;
        }

        // Execute the current block
        Statement.ExecResult result = currentBlock.execute(this, world);
        world.notifyBlockExecuted(currentBlock);
        switch (result) {
            case DONE:
                // Block completed, advance to next block
                advanceToNextBlock();
                break;

            case RUNNING:
                // Block is still executing (flow control block)
                // Don't advance - we'll execute this same block next tick
                // The flow control block itself manages its internal state
                break;

            case ERROR:
                // Block encountered an error
                hasError = true;
                return false;
        }

        return !isFinished;
    }

    /**
     * Advance to the next block in the script.
     * Handles both linear progression and flow control stack unwinding.
     */
    private void advanceToNextBlock() {
        // Check if current block has a next connection
        if (currentBlock.hasNext()) {
            Block nextBlock = currentBlock.nextConnection.getTargetBlock();
            if (nextBlock instanceof Statement) {
                currentBlock = (Statement) nextBlock;
                return;
            }
        }
        // No next block in current chain
        // Check if we're inside a flow control block
        if (!flowControlStack.isEmpty()) {
            // Pop back to the flow control block
            FlowControlFrame frame = flowControlStack.peek();
            currentBlock = frame.flowControlBlock;
            // The flow control block will decide what to do next when executed
        } else {
            // Script is finished
            currentBlock = null;
            isFinished = true;
        }
    }

    /**
     * Push a flow control frame onto the stack.
     * Called by flow control blocks (if, repeat) when they start executing their body.
     *
     * @param block The flow control block (if, repeat, etc.)
     * @return FlowControlFrame for the pushed block
     */
    public FlowControlFrame pushFlowControlFrame(Statement block) {
        FlowControlFrame frame = new FlowControlFrame(block);
        flowControlStack.push(frame);
        return frame;
    }

    /**
     * Pop a flow control frame from the stack.
     * Called by flow control blocks when they finish executing.
     */
    public void popFlowControlFrame() {
        if (!flowControlStack.isEmpty()) {
            flowControlStack.pop();
        }
    }

    /**
     * Jump to a specific block in the script.
     * Used by flow control blocks to enter their body statements.
     *
     * @param block The block to jump to
     */
    public void jumpTo(Statement block) {
        currentBlock = block;
    }

    /**
     * Get the actor this script is running on
     */
    public Actor getActor() {
        return actor;
    }

    /**
     * Check if this script has finished executing
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Check if this script encountered an error
     */
    public boolean hasError() {
        return hasError;
    }

    /**
     * Get the current block being executed
     */
    public Statement getCurrentBlock() {
        return currentBlock;
    }

    /**
     * Reset the script to start from the beginning.
     * Used for event-triggered scripts that can run multiple times.
     */
    public void reset() {
        currentBlock = scriptHead;
        flowControlStack.clear();
        executionCount = 0;
        isFinished = false;
        hasError = false;
    }

    public HatBlock getHead() {
        return scriptHead;
    }

    public FlowControlFrame peekFlowControlFrame() {
        if (flowControlStack.isEmpty()) {
            return null;
        }
        return flowControlStack.peek();
    }

    /**
     * Frame to track flow control block state
     */
    public static class FlowControlFrame {
        public final Statement flowControlBlock;

        public int counter;
        public int limit;
        public boolean initialized;

        FlowControlFrame(Statement block) {
            this.flowControlBlock = block;
        }
    }
}