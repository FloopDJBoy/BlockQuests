package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.StatementInput;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.SpriteGroup;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.R;
import FloopDJBoy.floopdjboy.blockquest.Workspace;

public class BlockView extends FrameLayout {

    public static final float CONNECTION_SNAP_DISTANCE = 100f;
    private final SpriteGroup owner;
    private BlockRenderer blockRenderer;
    protected final Block block;
    public  int blockColor;
    private boolean isHighlighted;

    public static void clear() {
        blockToViews.clear();
    }

    public Block getBlock() {
        return block;
    }
    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
        invalidate();
    }


    protected BlockView parentBlock = null;
    protected BlockView next = null;

    // For expression connections
    protected ExpressionInput parentInput = null;
    protected final List<BlockView> childExpressionViews = new ArrayList<>();

    // For statement input connections (block inside a pocket of a flow-control block)
    protected StatementInput parentStatementInput = null;
    protected BlockView parentStatementInputView = null;
    protected final List<BlockView> childStatementInputViews = new ArrayList<>();

    private static final HashMap<Block,BlockView> blockToViews = new HashMap<>();


    private boolean isDragging = false;
    private float initialTouchX;
    private float initialTouchY;
    private Workspace workspace;
    public static HashMap<SpriteGroup,ArrayList<Block>> getGlobalBlockData(){
        HashMap<SpriteGroup,ArrayList<Block>> data = new HashMap<>();
        for(BlockView b : blockToViews.values()){
            if(!data.containsKey(b.owner)){
                data.put(b.owner, new ArrayList<>());
            }
            data.get(b.owner).add(b.block);
        }
        return data;
    }
    public static HashMap<SpriteGroup,ArrayList<Block>> getGlobalBlockRootsData(){
        HashMap<SpriteGroup,ArrayList<Block>> data = new HashMap<>();
        for(BlockView b : blockToViews.values()) {
            if (b.block.getParent() == null) {
                if(!data.containsKey(b.owner)){
                    data.put(b.owner, new ArrayList<>());
                }
                data.get(b.owner).add(b.block);
            }
        }
        return data;
    }
    public BlockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        block = null;
        owner = null;
        blockColor = 0;
    }
    public static BlockView getBlockViewByBlock(Block block){
        return blockToViews.get(block);
    }

    public SpriteGroup getOwner(){
        return owner;
    }
    /**
     * Returns true if editing this block should be blocked.
     * Editing is blocked when play mode is active and this block's owner
     * is not a player group.
     */
    private boolean isEditingBlocked() {
        if (workspace == null) return false;
        if (!workspace.isPlayMode()) return false;
        if (owner == null) return false;
        return !owner.hasTag(Actor.Tag.PLAYER);
    }

    public BlockView(Context context, Block block, int color, float x, float y, SpriteGroup owner,Workspace workspace) {
        super(context);
        this.workspace = workspace;
        this.block = block;
        this.owner = owner;
        this.blockColor = color;
        blockToViews.put(block, this);
        setX(x);
        setY(y);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        BlockRenderer.BlockMeasurement measurement = blockRenderer.measureBlock(block);
        int w = (int) measurement.width;
        int h = (int) (measurement.height + (block instanceof Statement ? Constants.NOTCH_HEIGHT : 0)) + 1;
        setMeasuredDimension(w, h);
    }

    private void init() {
        setWillNotDraw(false);
        blockRenderer = new BlockRenderer();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
             //   Log.d("drag","drag started at ("+event.getRawX()+","+event.getRawY()+")"+" in screen cords");
                float touchX = event.getX();
                float touchY = event.getY();
                // In play mode, block all interaction for non-player groups
                if(isEditingBlocked()){
                    return false;
                }

                // Check if touching a field first
                for (Path path : blockRenderer.touchRegions.keySet()) {
                    RectF bounds = new RectF();
                    path.computeBounds(bounds, true);

                    Region clipRegion = new Region(
                            (int) Math.floor(bounds.left),
                            (int) Math.floor(bounds.top),
                            (int) Math.ceil(bounds.right),
                            (int) Math.ceil(bounds.bottom)
                    );

                    Region r = new Region();
                    r.setPath(path, clipRegion);

                    if (r.contains((int) touchX, (int) touchY)) {
                        BlockRenderer.FieldMeasurement fm = blockRenderer.touchRegions.get(path);

                        // Convert block-local coordinates to workspace coordinates
                        assert fm != null;
                        float workspaceX = getX() + fm.textX;
                        float workspaceY = getY() + fm.textY;

                        if (getParent() instanceof Workspace ws) {
                            ws.showFieldEditor(this,fm, workspaceX, workspaceY);
                        }
                        return true;
                    }
                }
                // Start drag
                int[] screenPos = new int[2];
                getLocationOnScreen(screenPos);

                initialTouchX = event.getRawX() - screenPos[0];
                initialTouchY = event.getRawY() - screenPos[1];

                isDragging = true;

                // Disconnect from parent (statement input pocket, expression input, or next/prev chain)
                if (parentStatementInput != null) {
                    disconnectFromStatementInput();
                } else if (parentBlock != null) {
                    if (parentInput != null) {
                        disconnectFromInput();
                    } else {
                        disconnect();
                    }
                }

                View root = getRootView().findViewById(R.id.dragOverlay);
                if (getParent() instanceof ViewGroup vg && root instanceof ViewGroup overlay) {

                    moveStack(overlay);
                    float scale = workspace.getScaleFactor();

                    setPivotX(0);
                    setPivotY(0);
                    setScaleX(scale);
                    setScaleY(scale);
                    initialTouchX *=  scale;
                    initialTouchY *=  scale;
                    int[] overlayPos = new int[2];
                    overlay.getLocationOnScreen(overlayPos);

                    setX(screenPos[0] - overlayPos[0]);
                    setY(screenPos[1] - overlayPos[1]);
                    reOrderBlocks();
                }
               // Log.d("drag","drag started after moving to overlay at ("+event.getRawX()+","+event.getRawY()+")"+" in screen cords");
//                bringToFrontRecursive();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {

                    // Position of the overlay on screen
                    int[] overlayPos = new int[2];
                    ((View) getParent()).getLocationOnScreen(overlayPos);

                    // Compute block position so the finger stays at the same point
                    float newX = event.getRawX() - overlayPos[0] - initialTouchX;
                    float newY = event.getRawY() - overlayPos[1] - initialTouchY;

                    setX(newX);
                    setY(newY);
                    reOrderBlocks();

                    // ---- Connection detection ----

                    // Workspace position on screen
                    int[] wsPos = new int[2];
                    workspace.getLocationOnScreen(wsPos);

                    // Finger position inside workspace viewport
                    float viewportX = event.getRawX() - wsPos[0];
                    float viewportY = event.getRawY() - wsPos[1];



                    // Convert block position to canvas coordinates
                    float blockCanvasX = workspace.screenToCanvasX(event.getRawX() - initialTouchX);
                    float blockCanvasY = workspace.screenToCanvasY(event.getRawY() - initialTouchY);

                    // Only run connection detection if the finger is inside workspace
                    if (viewportX >= 0 && viewportY >= 0 &&
                            viewportX <= workspace.getWidth() &&
                            viewportY <= workspace.getHeight()) {

                        workspace.onBlockDragging(this, blockCanvasX, blockCanvasY);
                    }

                    return true;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;

                    // Notify workspace of drop.
                    // blockPos gives us the on-screen top-left of this view (which is in the dragOverlay).
                    // wsPos gives us the on-screen top-left of the workspace viewport.
                    // The difference is the visual (viewport) position; adding scroll gives canvas position.
                    int[] wsPos = new int[2];
                    workspace.getLocationOnScreen(wsPos);

                    int[] blockPos = new int[2];
                    getLocationOnScreen(blockPos);

                    // Viewport-relative position (where it appears on screen within the workspace)
                    float viewportX = blockPos[0] - wsPos[0];
                    float viewportY = blockPos[1] - wsPos[1];

                    // Canvas position = viewport position + scroll offset
                    float canvasX = workspace.screenToCanvasX(event.getRawX() - initialTouchX);
                    float canvasY = workspace.screenToCanvasY(event.getRawY() - initialTouchY);

                    if (viewportX >= 0 && viewportY >= 0 &&
                            viewportX <= workspace.getWidth() &&
                            viewportY <= workspace.getHeight()) {
                        workspace.onBlockDropped(this, canvasX, canvasY);
                    }else{
                        animateDelete();
                    }
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void disconnectNext() {
        if (next == null) return;
        BlockView child = next;
        next = null;
        child.parentBlock = null;
        block.disconnect(ConnectionType.NEXT_STATEMENT);
        propagateDirtyUp();
    }
    private void invalidateParentStatementInputView(){
        if(parentStatementInputView != null){
            parentStatementInputView.makeDirty();
            parentStatementInputView.invalidate();
            parentStatementInputView.requestLayout();
            parentStatementInputView.reOrderBlocks();
        }
    }
    public void propagateDirtyUp(){
        BlockView current = this;
        while (current != null) {
            current.block.makeDirty();
            current.invalidate();
            current.requestLayout();

            // Expression child -> parent measurement depends on child size
            if (current.parentInput != null && current.parentBlock != null) {
                current = current.parentBlock;
                continue;
            }
            // Statement chain inside a pocket -> pocket owner measurement depends on chain height
            if (current.parentStatementInputView != null) {
                current = current.parentStatementInputView;
                continue;
            }

            // Pure next-chain relation does not affect measured size
            current.reOrderBlocks();
            break;
        }
    }
    public void disconnectFromInput() {
        if (parentBlock == null || parentInput == null) return;
        if(block instanceof Expression<?>){
            parentBlock.invalidate();
            parentBlock.requestLayout();
        }
        parentBlock.childExpressionViews.remove(this);
        parentBlock.makeDirty();
        block.disconnect(ConnectionType.OUTPUT_VALUE);

        parentBlock = null;
        parentInput = null;
    }

    public void disconnectFromStatementInput() {
        if (parentStatementInputView == null || parentStatementInput == null) return;

        parentStatementInputView.childStatementInputViews.remove(this);
        // Disconnect at AST level: the child's previousConnection connects to the StatementInput's connection
        if (block instanceof Statement stmt && stmt.previousConnection != null && stmt.previousConnection.isConnected()) {
            stmt.previousConnection.disconnect();
        }
        propagateDirtyUp();
        parentStatementInput = null;
        parentStatementInputView = null;
    }

    public void disconnect() {
        if (parentBlock == null) return;
        BlockView parent = parentBlock;
        parentBlock = null;
        parent.next = null;
        if(block instanceof Statement){
            block.disconnect(ConnectionType.PREVIOUS_STATEMENT);
        }else{
            block.disconnect(ConnectionType.OUTPUT_VALUE);
        }
        propagateDirtyUp();


    }

    public void connectNext(BlockView child) {
        if (child.parentBlock != null || child == this) return;

        this.next = child;
        child.parentBlock = this;
        child.parentInput = null;
        block.connect(child.block, ConnectionType.NEXT_STATEMENT);
        child.parentStatementInputView = parentStatementInputView;
        propagateDirtyUp();
    }

    public void connectToInput(BlockView parent, ExpressionInput input) {
        if (parent == this) return;

        this.parentBlock = parent;
        this.parentInput = input;
        parent.childExpressionViews.add(this);

        // AST connection
        input.connect(block);
        propagateDirtyUp();
    }

    public BlockView getParentStatementInputView() {
        return parentStatementInputView;
    }

    /**
     * Connect this block (must be a Statement) as the first child of a StatementInput pocket.
     *
     * @param parent The BlockView that owns the StatementInput
     * @param input  The StatementInput to connect into
     */
    public void connectToStatementInput(BlockView parent, StatementInput input) {
        if (parent == this) return;
        if (!(block instanceof Statement)) return;

        this.parentStatementInputView = parent;
        this.parentStatementInput = input;
        parent.childStatementInputViews.add(this);

        // AST connection
        input.connect(block);
        propagateDirtyUp();
    }
    public void reOrderBlocks() {
        if (block == null) return;

        BlockRenderer.BlockMeasurement m = blockRenderer.measureBlock(block);
        float x = getX();
        float y = getY();

        if (next != null && m.nextNotch != null) {
            next.setX(x);
            next.setY(y + m.nextNotch.y);
            if (DragProjectionState.getProjectedNextBlock(block) != null) {
                BlockRenderer.BlockMeasurement projM = blockRenderer.measureBlock(DragProjectionState.getProjectedNextBlock(block));
                next.setY(y + m.nextNotch.y + projM.height);
            }
            next.reOrderBlocks();
        }

        for (BlockRenderer.SocketLayout socket : m.sockets.values()) {
            if (socket.input.connection != null && socket.input.connection.isConnected()) {
                BlockView childView = findViewForBlock(socket.input.connection.getTargetBlock());
                if (childView != null) {
                    float projectionOffset = 0;
                    if (!socket.isExpression) {
                        Block projBlock = DragProjectionState.getProjectedBlock(socket.input);
                        if (projBlock != null) {
                            BlockView projView = BlockView.getBlockViewByBlock(projBlock);
                            if (projView != null) projectionOffset = projView.getStackHeight();
                        }
                    }
                    childView.setX(x + socket.x);
                    childView.setY(y + socket.y + projectionOffset);
                    childView.reOrderBlocks();
                }
            }
        }
    }

    public void moveStack(ViewGroup newParent){
        List<BlockView> list = new ArrayList<>();
        collectStack(this, list);

        for(BlockView b : list){
            ViewParent p = b.getParent();
            if(p instanceof ViewGroup vg){
                vg.removeView(b);
            }
            newParent.addView(b);
        }
    }
    private void animateDelete() {
        // Collect every view in the stack before re-parenting
        List<BlockView> stack = new ArrayList<>();
        collectStack(this, stack);

        // Grab the current parent (the drag overlay) before we start moving views
        ViewParent currentParent = getParent();
        if (!(currentParent instanceof ViewGroup overlay)) {
            // Fallback: no parent, just delete immediately
            deleteStack();
            return;
        }

        // Create a temporary container that sits at (0,0) filling the overlay.
        // All stack blocks already have absolute positions within the overlay
        // via setX/setY, so re-parenting them into a full-size container at (0,0)
        // keeps them visually in exactly the same place.
        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        overlay.addView(container, lp);

        // Move every block in the stack into the container
        for (BlockView bv : stack) {
            ViewParent p = bv.getParent();
            if (p instanceof ViewGroup vg) {
                vg.removeView(bv);
            }
            container.addView(bv);
        }

        container.setPivotX(getX());
        container.setPivotY(getY());
        container.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    deleteStack();
                    ViewParent cp = container.getParent();
                    if (cp instanceof ViewGroup cvg) {
                        cvg.removeView(container);
                    }
                })
                .start();
    }
    /**
     * Delete this block and the entire stack connected below/inside it.
     * Removes all views from the hierarchy, cleans up the blockToViews map,
     * and unregisters any HatBlock scripts from the owning actor.
     */
    public void deleteStack() {
        // Collect every BlockView in this stack
        List<BlockView> stack = new ArrayList<>();
        collectStack(this, stack);

        for (BlockView bv : stack) {
            bv.isHighlighted = false;
            // Remove from blockToViews lookup
            blockToViews.remove(bv.block);
            bv.block.dispose();

            // If this is a HatBlock, remove its script from the actor
            if (bv.block instanceof HatBlock hatBlock && bv.owner != null) {
                bv.owner.removeScript(hatBlock);
            }

            // Remove from whatever parent view it currently sits in
            ViewParent parent = bv.getParent();
            if (parent instanceof ViewGroup vg) {
                vg.removeView(bv);
            }
        }
    }
    public float getStackHeight() {
        float height = 0f;
        BlockView current = this;
        while (current != null) {
            height += current.getMeasurement().height;
            current = current.getNext();
        }
        return height;
    }
    public BlockView getLastBlockInStack(){
        BlockView current = this;
        while (current.getNext() != null) {
            current = current.getNext();
        }
        return current;
    }
    private void collectStack(BlockView b, List<BlockView> out){
        out.add(b);

        for(BlockView c : b.childExpressionViews){
            collectStack(c, out);
        }

        for(BlockView c : b.childStatementInputViews){
            collectStack(c, out);
        }

        if(b.next != null){
            collectStack(b.next, out);
        }
    }

    private BlockView findViewForBlock(Block targetBlock) {
        for (BlockView bv : childExpressionViews) {
            if (bv.block == targetBlock) return bv;
        }
        for (BlockView bv : childStatementInputViews) {
            if (bv.block == targetBlock) return bv;
        }
        if (next != null && next.block == targetBlock) return next;

        if (getParent() instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);
                if(v.getVisibility() == GONE){
                    continue;
                }
                if (v instanceof BlockView bv && bv.block == targetBlock) return bv;
            }
        }
        return null;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (block != null && blockRenderer != null) {
            blockRenderer.renderBlock(canvas, block, 0, 0, blockColor, false,isHighlighted);
        }
    }
    public float[] getNextConnectionPosition() {
        if(getVisibility() == GONE) return null;
        BlockRenderer.BlockMeasurement m = blockRenderer.measureBlock(block);
        if (m.nextNotch == null) return null;
        return new float[]{ getX() + m.nextNotch.x + Constants.NOTCH_WIDTH / 2, getY() + m.nextNotch.y };
    }
    public float[] getPreviousConnectionPosition() {
        if(getVisibility() == GONE) return null;
        BlockRenderer.BlockMeasurement m = blockRenderer.measureBlock(block);
        if (m.prevNotch == null) return null;
        return new float[]{ getX() + m.prevNotch.x + Constants.NOTCH_WIDTH / 2, getY() + m.prevNotch.y };
    }

    public BlockView getNext() { return next; }
    public BlockView getParentBlock() { return parentBlock; }
    public BlockRenderer.BlockMeasurement getMeasurement() { return blockRenderer.measureBlock(block); }

    public boolean isPreviousConnectionNear(float worldX, float worldY) {
        if (!(block instanceof Statement) || block instanceof HatBlock) return false;
        if (parentBlock != null) return false;
        float[] pos = getPreviousConnectionPosition();
        if (pos == null) return false;
        float distance = (float) Math.sqrt(Math.pow(worldX - pos[0], 2) + Math.pow(worldY - pos[1], 2));
        return distance < CONNECTION_SNAP_DISTANCE;
    }

    /**
     * <p>Find the nearest unoccupied StatementInput socket on this block to (worldX, worldY).
     * Only the top notch of a pocket is a connection point.</p>
     *
     * @return a {@link StatementSocketInfo} if one is near, or null
     */
    public StatementSocketInfo findStatementSocketNear(float worldX, float worldY) {
        if (!(block instanceof Statement)) return null;
        BlockRenderer.BlockMeasurement m = blockRenderer.measureBlock(block);

        for (BlockRenderer.SocketLayout socket : m.sockets.values()) {
            if (!socket.isExpression && socket.input instanceof StatementInput stmtInput) {
                float notchCenterX = getX() + socket.x + Constants.NOTCH_OFFSET_LEFT + Constants.NOTCH_WIDTH / 2f;
                float pocketTopY = getY() + socket.y;

                if (Math.sqrt(Math.pow(worldX - notchCenterX, 2) + Math.pow(worldY - pocketTopY, 2)) < CONNECTION_SNAP_DISTANCE) {
                    return new StatementSocketInfo(stmtInput, getX() + socket.x, pocketTopY);
                }
            }
        }
        return null;
    }

    public ExpressionSocketInfo findExpressionSocketNear(float worldX, float worldY) {
        BlockRenderer.BlockMeasurement m = blockRenderer.measureBlock(block);

        for (BlockRenderer.SocketLayout socket : m.sockets.values()) {
            if (socket.isExpression && socket.input instanceof ExpressionInput exprInput && !socket.input.connection.isConnected()) {
                float centerX = getX() + socket.x + socket.width / 2f;
                float centerY = getY() + socket.y + socket.height / 2f;

                if (Math.sqrt(Math.pow(worldX - centerX, 2) + Math.pow(worldY - centerY, 2)) < CONNECTION_SNAP_DISTANCE) {
                    return new ExpressionSocketInfo(exprInput, getX() + socket.x, getY() + socket.y);
                }
            }
        }
        return null;
    }

    public void makeDirty() {
        block.makeDirty();
        propagateDirtyUp();
    }


    public static class ExpressionSocketInfo {
        public final ExpressionInput input;
        public final float x;
        public final float y;

        public ExpressionSocketInfo(ExpressionInput input, float x, float y) {
            this.input = input;
            this.x = x;
            this.y = y;
        }
    }

    public static class StatementSocketInfo {
        public final StatementInput input;
        /** The X position where the child block should be placed (snap X). */
        public final float x;
        /** The Y position where the child block should be placed (snap Y). */
        public final float y;

        public StatementSocketInfo(StatementInput input, float x, float y) {
            this.input = input;
            this.x = x;
            this.y = y;
        }
    }
}