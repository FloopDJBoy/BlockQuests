package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.Input;

public class DragProjectionState {
    private static Input activeInput = null;
    private static Block projectedBlock = null;
    private static Block targetNextBlock = null;

    public static void setProjection(Input input, Block block) {
        activeInput = input;
        projectedBlock = block;
    }

    public static void clear() {
        BlockView bv = BlockView.getBlockViewByBlock(targetNextBlock);
        activeInput = null;
        targetNextBlock = null;
        projectedBlock = null;
        if (bv != null){
            bv.makeDirty();
            bv.requestLayout();
            bv.reOrderBlocks();
            bv.invalidate();
        }
    }
    private DragProjectionState(){
        throw new AssertionError("Attempt to instantiate static class DragProjectionState");
    }
    public static void setNextConnectionProjection(Block target, Block projected) {
        targetNextBlock = target;
        projectedBlock = projected;
    }

    public static Block getProjectedNextBlock(Block currentBlock) {
        if (targetNextBlock != null && targetNextBlock == currentBlock) {
            return projectedBlock;
        }
        return null;
    }
    public static boolean hasProjection() {
        return activeInput != null;
    }

    public static Block getProjectedBlock(Input input) {
        if (activeInput != null && activeInput == input) {
            return projectedBlock;
        }
        return null;
    }
}