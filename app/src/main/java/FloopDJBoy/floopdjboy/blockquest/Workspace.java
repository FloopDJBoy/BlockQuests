package FloopDJBoy.floopdjboy.blockquest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.NumberField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.TextField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.SpriteGroup;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableDefinition;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ActorLayerView;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.BlockRenderer;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.BlockView;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.Constants;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.DragProjectionState;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.PaletteAdapter;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ShadowBlockView;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.VariableAdapter;

public class Workspace extends FrameLayout {

    // Virtual canvas dimensions — the scrollable area blocks live in.
    // The view's on-screen footprint is still controlled entirely by the
    // ConstraintLayout params set in the Activity; we do NOT override onMeasure.
    public static final int CANVAS_WIDTH  = 8000;
    public static final int CANVAS_HEIGHT = 8000;

    // Dot-grid background
    private static final float GRID_DOT_SPACING = 40f;
    private ImageView activeActorImage;
    private static final float GRID_DOT_RADIUS   = 1.5f;
    private final ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private final RecyclerView variableHolder;
    private final VariableAdapter varAdapter;

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final FrameLayout fieldOverlay;
    private ShadowBlockView shadowBlock;
    private BlockView targetConnectionBlock = null;
    private boolean isConnectingToTop = false;
    private BlockView draggingBlockView;
    private Supplier<Block> draggingFactory;
    // For expression connections
    private BlockView.ExpressionSocketInfo targetSocket = null;

    // For statement input (pocket) connections
    private BlockView.StatementSocketInfo targetStatementSocket = null;
    private int baseWidth, baseHeight;

    private BlockView targetStatementSocketBlock = null;
    private LinearLayout tagArea;
    private SpriteGroup activeGroup;

    // -----------------------------------------------------------------------
    // Pan state
    //
    // Pan activates when TWO fingers are on screen simultaneously, regardless
    // of whether either finger started on a BlockView.  The workspace midpoint
    // of the two pointers is used as the anchor so the canvas tracks naturally.
    //
    // Single-finger on empty canvas also pans (finger 1 on empty space only).
    // -----------------------------------------------------------------------
    private boolean isPanning    = false;
    private boolean twoFingerPan = false; // true while 2 fingers are driving the pan
    private int     panPointer0  = -1;    // first  tracked pointer id
    private int     panPointer1  = -1;    // second tracked pointer id (-1 = not active)
    private float   panLastX;
    private float   panLastY;
    // -----------------------------------------------------------------------
    // Play mode
    //
    // When true:
    //   - tile/actor placement is blocked (handled in MainActivity)
    //   - block dragging and field editing are blocked for non-player groups
    //   - the actor palette is hidden (handled in MainActivity)
    //   - browsing (switching active actor, viewing blocks) is still allowed
    // -----------------------------------------------------------------------
    private boolean isPlayMode = false;
    private ActorLayerView actorLayerView;
    private RecyclerView blockPalette;
    // -----------------------------------------------------------------------
    public boolean isPlayMode() {
        return isPlayMode;
    }

    public void setPlayMode(boolean playMode) {
        isPlayMode = playMode;
    }
    public void setActiveActor(Actor newActor) {
        this.activeGroup = newActor.getSpriteGroup();
        if(activeActorImage==null){
            activeActorImage =getRootView().findViewById(R.id.activeActorImage);
        }
        if(actorLayerView!=null){
            Bitmap frame = actorLayerView.getPreviewFrame(newActor);
            if(frame!=null){
                activeActorImage.setImageBitmap(frame);
            }else{
                activeActorImage.setImageResource(newActor.getSpriteResourceId());
            }
        }else{
            activeActorImage.setImageResource(newActor.getSpriteResourceId());
        }
        activeActorImage.setAlpha(0.6f);
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof BlockView bv) {
                if (bv.getOwner() == activeGroup) {
                    bv.setVisibility(VISIBLE);
                } else {
                    bv.setVisibility(GONE);
                }
            }
        }
        if(tagArea==null){
            tagArea = getRootView().findViewById(R.id.underStageArea);
        }
        for(int i = 0; i < tagArea.getChildCount(); i++) {
            View v = tagArea.getChildAt(i);
            if (v instanceof CheckBox bx) {
                bx.setChecked(activeGroup.hasTag(Actor.Tag.values()[(int)(bx.getTag())]));
            }
        }
        if(blockPalette == null){
            blockPalette = getRootView().findViewById(R.id.blockPalette);
        }
        if(blockPalette.getAdapter() != null) {
            ((PaletteAdapter) blockPalette.getAdapter()).rebuildVariablePalette();
        }
        varAdapter.setRows(buildRows());
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        baseWidth = w;
        baseHeight = h;
    }
    public void setActorLayerView(ActorLayerView view) {
        this.actorLayerView = view;
        setActiveActor(activeGroup.getActor(0));
    }

    public BlockView spawnBlockFromPalette(Supplier<Block> factory, float x, float y) {
        Block block = factory.get();

        BlockView bv = new BlockView(
                getContext(),
                block,
                block.getCategory().getColor(),
                x, y,
                activeGroup,
                this
        );

        addView(bv);

        if (block instanceof HatBlock h && !activeGroup.hasScript(h)) {
            activeGroup.addScript(h);
        }

        return bv;
    }

    public Workspace(Context context) {
        super(context);
        setClipChildren(true);
        setClipToPadding(true);
        gridPaint.setColor(Color.parseColor("#484848"));
        gridPaint.setStyle(Paint.Style.FILL);

        // Anchor scale at top-left so the canvas always fills from the origin
        // and we can clamp scroll to eliminate any gap.
        setPivotX(0f);
        setPivotY(0f);

        // The fieldOverlay sits on top of all blocks and hosts the inline EditText.
        // It is translated in onScrollChanged to stay pinned to the visible viewport.
        fieldOverlay = new FrameLayout(getContext());
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        addView(fieldOverlay, lp);
        fieldOverlay.bringToFront();

        setWillNotDraw(false);

        setOnDragListener((v, event) -> {
            switch (event.getAction()) {

                case DragEvent.ACTION_DRAG_STARTED:
                    Object local = event.getLocalState();
                    if (local instanceof PaletteAdapter.PaletteItem item) {
                        draggingFactory = item.factory;

                        // event cords are viewport-relative; add scroll for canvas position
                        Block block = draggingFactory.get();
                        draggingBlockView = new BlockView(
                                getContext(),
                                block,
                                block.getCategory().getColor(),
                                event.getX() + getScrollX(),
                                event.getY() + getScrollY(),
                                activeGroup,
                                this
                        );
                        addView(draggingBlockView);
                        return true;
                    }
                    return false;

                case DragEvent.ACTION_DRAG_LOCATION:
                    if (draggingBlockView != null) {
                        draggingBlockView.setX(event.getX() + getScrollX());
                        draggingBlockView.setY(event.getY() + getScrollY());
                    }
                    return true;

                case DragEvent.ACTION_DROP:
                case DragEvent.ACTION_DRAG_ENDED:
                    draggingFactory   = null;
                    draggingBlockView = null;
                    return true;
            }

            return true;
        });
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        float newScale = scaleFactor * detector.getScaleFactor();
                        applyScale(newScale);
                        return true;
                    }
                });
        post(() -> {
            float centerX = CANVAS_WIDTH / 2f;
            float centerY = CANVAS_HEIGHT / 2f;

            float halfViewportX = getWidth() / (2f * scaleFactor);
            float halfViewportY = getHeight() / (2f * scaleFactor);

            int scrollX = (int)(centerX - halfViewportX);
            int scrollY = (int)(centerY - halfViewportY);

            scrollTo(scrollX, scrollY);
        });

        variableHolder = new RecyclerView(getContext());
        variableHolder.setLayoutManager(new LinearLayoutManager(getContext()));
        FrameLayout.LayoutParams holderLp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START
        );
        varAdapter = new VariableAdapter( new ArrayList<>());
        variableHolder.setAdapter(varAdapter);
        fieldOverlay.addView(variableHolder, holderLp);

    }
    private ArrayList<VariableAdapter.VarRow> buildRows() {
        ArrayList<VariableAdapter.VarRow> rows = new ArrayList<>();
        rows.add(VariableAdapter.VarRow.header("Local"));
        for (VariableDefinition v : getActiveGroup().getAllLocalVariables()) rows.add(VariableAdapter.VarRow.item(v,getActiveGroup().getVariable(v.id)));
        rows.add(VariableAdapter.VarRow.header("Global"));
        for (VariableDefinition v : getWorld().getAllGlobalVariables()) rows.add(VariableAdapter.VarRow.item(v,getActiveGroup().getVariable(v.id)));
        return rows;
    }
    // -----------------------------------------------------------------------
    // Scaling
    // -----------------------------------------------------------------------

    /**
     * Apply a new scale factor, clamped so the canvas always fills the viewport
     * (no gap/hole), and update the scroll bounds accordingly.
     * <p>
     * Uses setScaleX/setScaleY on the view itself so that the Android framework
     * correctly transforms touch/hit-test coordinates along with the visuals.
     * Previously this was done via a canvas transform in dispatchDraw, which
     * caused the visual and hitbox to become misaligned after scaling.
     */
    private void applyScale(float newScale) {
        newScale = Math.max(computeMinScale(), Math.min(newScale, MAX_SCALE));
        if (newScale == scaleFactor) return;

        scaleFactor = newScale;

        setScaleX(scaleFactor);
        setScaleY(scaleFactor);

        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.width  = (int)(baseWidth  / scaleFactor);
        lp.height = (int)(baseHeight / scaleFactor);
        setLayoutParams(lp);

        clampScroll();
    }

    /**
     * The minimum scale is whichever value makes the canvas exactly fill
     * the visible viewport — we never want to show empty space outside it.
     * Also respects the hard MIN_SCALE constant as a lower bound.
     */
    private float computeMinScale() {
        if (getWidth() == 0 || getHeight() == 0) return MIN_SCALE;

        float sx = (float) getWidth() / CANVAS_WIDTH;
        float sy = (float) getHeight() / CANVAS_HEIGHT;

        return Math.max(MIN_SCALE, Math.max(sx, sy));
    }

    /**
     * Clamp the current scroll so we never show canvas outside [0, CANVAS - viewport/scale].
     * With pivot at (0,0), setScaleX/Y shrinks the rendered area, so the max scroll
     * decreases as scale decreases.
     */
    private void clampScroll() {
        int maxX = Math.max(0, (int)(CANVAS_WIDTH  - getWidth()  / scaleFactor));
        int maxY = Math.max(0, (int)(CANVAS_HEIGHT - getHeight() / scaleFactor));

        int x = Math.max(0, Math.min(getScrollX(), maxX));
        int y = Math.max(0, Math.min(getScrollY(), maxY));

        if (x != getScrollX() || y != getScrollY()) {
            scrollTo(x, y);
        }
    }
    // -----------------------------------------------------------------------
    // NOTE: onMeasure is intentionally NOT overridden.
    // The view's on-screen size is determined solely by the ConstraintLayout
    // params set in the Activity.  Scroll bounds are enforced manually in
    // panScrollBy() so the canvas never scrolls outside [0, CANVAS - viewport].
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Background grid
    // -----------------------------------------------------------------------

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
    }

    private void drawGrid(Canvas canvas) {
        // Only draw dots visible inside the current viewport
        float left   = getScrollX();
        float top    = getScrollY();
        float right  = left + getWidth()  / scaleFactor;
        float bottom = top  + getHeight() / scaleFactor;

        float startX = (float)(Math.floor(left / GRID_DOT_SPACING) * GRID_DOT_SPACING);
        float startY = (float)(Math.floor(top  / GRID_DOT_SPACING) * GRID_DOT_SPACING);

        for (float x = startX; x <= right;  x += GRID_DOT_SPACING) {
            for (float y = startY; y <= bottom; y += GRID_DOT_SPACING) {
                canvas.drawCircle(x, y, GRID_DOT_RADIUS, gridPaint);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Pan gesture
    // -----------------------------------------------------------------------

    /**
     * Scroll by (dx, dy) clamped so we never go outside the virtual canvas.
     * Scroll units are canvas units (pre-scale), so we divide viewport size by
     * scaleFactor to get the visible canvas area.
     */
    private void panScrollBy(int dx, int dy) {
        int maxScrollX = Math.max(0, (int)(CANVAS_WIDTH  - getWidth()  / scaleFactor));
        int maxScrollY = Math.max(0, (int)(CANVAS_HEIGHT - getHeight() / scaleFactor));
        int newX = Math.max(0, Math.min(getScrollX() + dx, maxScrollX));
        int newY = Math.max(0, Math.min(getScrollY() + dy, maxScrollY));
        scrollTo(newX, newY);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Single finger down — only pan if it lands on empty canvas (not a block).
                float canvasX = ev.getX() + getScrollX();
                float canvasY = ev.getY() + getScrollY();

                if (hitTestBlockView(canvasX, canvasY)) {
                    // Let the block handle it; reset any stale pan state.
                    isPanning    = false;
                    twoFingerPan = false;
                    panPointer0  = -1;
                    panPointer1  = -1;
                    return false;
                }

                // Empty canvas single-finger pan
                isPanning    = true;
                twoFingerPan = false;
                panPointer0  = ev.getPointerId(0);
                panPointer1  = -1;
                panLastX     = ev.getX();
                panLastY     = ev.getY();
                return true; // steal the stream
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                // Second (or further) finger arrived.  Switch to two-finger pan
                // regardless of what the first finger is doing — this is what
                // lets the user pan even when one finger started on a block.
                int idx      = ev.getActionIndex();
                isPanning    = true;
                twoFingerPan = true;
                panPointer0  = ev.getPointerId(0);
                panPointer1  = ev.getPointerId(idx);

                // Anchor to the midpoint of the two pointers
                int idx0 = ev.findPointerIndex(panPointer0);
                int idx1 = ev.findPointerIndex(panPointer1);
                if (idx0 >= 0 && idx1 >= 0) {
                    panLastX = (ev.getX(idx0) + ev.getX(idx1)) / 2f;
                    panLastY = (ev.getY(idx0) + ev.getY(idx1)) / 2f;
                }
                return true; // steal from whatever child had it
            }

            case MotionEvent.ACTION_POINTER_UP: {
                // A finger lifted — if we were in two-finger pan, drop back gracefully
                if (twoFingerPan) {
                    int liftedId = ev.getPointerId(ev.getActionIndex());
                    // Find the remaining pointer
                    int remainingId = (liftedId == panPointer0) ? panPointer1 : panPointer0;
                    int remainIdx = ev.findPointerIndex(remainingId);
                    if (remainIdx >= 0) {
                        panPointer0  = remainingId;
                        panPointer1  = -1;
                        twoFingerPan = false;
                        panLastX     = ev.getX(remainIdx);
                        panLastY     = ev.getY(remainIdx);
                    }
                }
                return false;
            }
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        if (!isPanning) return false;
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (ev.getPointerCount() >= 2) {
                    // Refresh two-finger anchor
                    twoFingerPan = false;
                    panPointer0  = ev.getPointerId(0);
                    panPointer1  = ev.getPointerId(1);
                    int idx0 = ev.findPointerIndex(panPointer0);
                    int idx1 = ev.findPointerIndex(panPointer1);
                    if (idx0 >= 0 && idx1 >= 0) {
                        panLastX = (ev.getX(idx0) + ev.getX(idx1)) / 2f;
                        panLastY = (ev.getY(idx0) + ev.getY(idx1)) / 2f;
                    }
                } else {
                    panPointer0  = ev.getPointerId(0);
                    panPointer1  = -1;
                    twoFingerPan = false;
                    panLastX     = ev.getX();
                    panLastY     = ev.getY();
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                float currentX, currentY;

                if (twoFingerPan && panPointer1 != -1) {
                    // Use midpoint of both tracked pointers
                    int idx0 = ev.findPointerIndex(panPointer0);
                    int idx1 = ev.findPointerIndex(panPointer1);
                    if (idx0 < 0 || idx1 < 0) return true;
                    currentX = (ev.getX(idx0) + ev.getX(idx1)) / 2f;
                    currentY = (ev.getY(idx0) + ev.getY(idx1)) / 2f;
                } else {
                    int idx = ev.findPointerIndex(panPointer0);
                    if (idx < 0) return true;
                    currentX = ev.getX(idx);
                    currentY = ev.getY(idx);
                }

                float dx = currentX - panLastX;
                float dy = currentY - panLastY;
                panLastX = currentX;
                panLastY = currentY;

                // Pan delta is in screen pixels; divide by scaleFactor to get canvas units.
                panScrollBy((int)(-dx / scaleFactor), (int)(-dy / scaleFactor));
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int liftedId = ev.getPointerId(ev.getActionIndex());
                if (twoFingerPan) {
                    // Drop back to single-finger pan with the remaining pointer
                    int remainingId  = (liftedId == panPointer0) ? panPointer1 : panPointer0;
                    int remainIdx    = ev.findPointerIndex(remainingId);
                    twoFingerPan     = false;
                    panPointer0      = remainingId;
                    panPointer1      = -1;
                    if (remainIdx >= 0) {
                        panLastX = ev.getX(remainIdx);
                        panLastY = ev.getY(remainIdx);
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isPanning    = false;
                twoFingerPan = false;
                panPointer0  = -1;
                panPointer1  = -1;
                return true;
            }
        }

        return false;
    }

    // Returns true if canvas point (canvasX, canvasY) hits a visible BlockView.
    private boolean hitTestBlockView(float canvasX, float canvasY) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (!(v instanceof BlockView)) continue;
            if (v.getVisibility() != VISIBLE) continue;
            float l = v.getX();
            float t = v.getY();
            float r = l + v.getMeasuredWidth();
            float b = t + v.getMeasuredHeight();
            if (canvasX >= l && canvasX <= r && canvasY >= t && canvasY <= b) return true;
        }
        return false;
    }
    public void refreshVarRow(){
        varAdapter.setRows(buildRows());
    }

    // -----------------------------------------------------------------------
    // Keep fieldOverlay pinned to the visible viewport when we scroll
    // -----------------------------------------------------------------------

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        fieldOverlay.setTranslationX(l);
        fieldOverlay.setTranslationY(t);
        invalidate();
    }


    // -----------------------------------------------------------------------
    // Coordinate helpers: convert between canvas space and screen space
    // -----------------------------------------------------------------------

    /**
     * Convert a canvas-space point to screen-space (pixel) coordinates.
     * canvasX/Y are the block's setX/setY values (canvas units, scroll-relative).
     */
    public float canvasToScreenX(float canvasX) {
        int[] ws = new int[2];
        getLocationOnScreen(ws);
        return (canvasX - getScrollX()) * scaleFactor + ws[0];
    }

    public float canvasToScreenY(float canvasY) {
        int[] ws = new int[2];
        getLocationOnScreen(ws);
        return (canvasY - getScrollY()) * scaleFactor + ws[1];
    }

    /**
     * Convert screen-space coordinates back to canvas-space.
     */
    public float screenToCanvasX(float screenX) {
        int[] ws = new int[2];
        getLocationOnScreen(ws);
        return (screenX - ws[0]) / scaleFactor + getScrollX();
    }

    public float screenToCanvasY(float screenY) {
        int[] ws = new int[2];
        getLocationOnScreen(ws);
        return (screenY - ws[1]) / scaleFactor + getScrollY();
    }

    /** Current scale factor, so BlockView can query it when entering/leaving the overlay. */
    public float getScaleFactor() {
        return scaleFactor;
    }

    // -----------------------------------------------------------------------

    public void onBlockDragging(BlockView draggedBlock, float x, float y) {
        if(findFocus() !=null){
            findFocus().clearFocus();
        }
        BlockView oldStmtTarget = targetStatementSocketBlock;
        BlockView oldExprTarget = targetConnectionBlock;
        targetConnectionBlock = null;
        targetSocket = null;
        targetStatementSocket = null;
        targetStatementSocketBlock = null;
       // removeAllExcept(fieldOverlay,variableHolder);
        if (shadowBlock != null) {
            removeView(shadowBlock);
            shadowBlock = null;
        }
        boolean projectionChanged = DragProjectionState.hasProjection();
        DragProjectionState.clear();
        if (projectionChanged) {
            if(oldStmtTarget != null){
                oldStmtTarget.makeDirty();
                oldStmtTarget.requestLayout();
                oldStmtTarget.reOrderBlocks();
                oldStmtTarget.invalidate();
            }else if(oldExprTarget != null){
                oldExprTarget.makeDirty();
                oldExprTarget.requestLayout();
                oldExprTarget.reOrderBlocks();
                oldExprTarget.invalidate();
            }
        }
        // Check for expression connections first if dragged block is an expression
        if (draggedBlock.getBlock() instanceof Expression<?> expr) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!(child instanceof BlockView otherBlock)) continue;
                if (otherBlock == draggedBlock) continue;
                if (child == fieldOverlay) continue;
                if (child instanceof ShadowBlockView) continue;

                BlockView.ExpressionSocketInfo socket = otherBlock.findExpressionSocketNear(x, y);
                if (socket != null) {
                    if (socket.input.getExpectedType().isAssignableFrom(expr.getReturnType())) {
                        targetSocket = socket;
                        targetConnectionBlock = otherBlock;
                        DragProjectionState.setProjection(socket.input,draggedBlock.getBlock());
                        otherBlock.makeDirty();
                        otherBlock.requestLayout();
                        otherBlock.reOrderBlocks();
                        otherBlock.invalidate();
                        shadowBlock = new ShadowBlockView(
                                getContext(),
                                draggedBlock.getBlock(),
                                socket.x,
                                socket.y
                        );
                        addView(shadowBlock);
                        return;
                    }
                }
            }
        }

        // Statement connections
        if (!(draggedBlock.getBlock() instanceof Statement)) {
            return;
        }

        float draggedTopNotchX = x + Constants.NOTCH_OFFSET_LEFT + Constants.NOTCH_WIDTH / 2;
        float draggedStackHeight = draggedBlock.getStackHeight();
        float draggedBottomNotchX = x + Constants.NOTCH_OFFSET_LEFT + Constants.NOTCH_WIDTH / 2;
        float draggedBottomNotchY = y + draggedStackHeight;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!(child instanceof BlockView otherBlock)) continue;
            if (otherBlock == draggedBlock) continue;
            if (child == fieldOverlay) continue;
            if (child instanceof ShadowBlockView) continue;
            if (isInDraggedChain(draggedBlock, otherBlock)) continue;

            // Check for StatementInput (pocket) connection — top notch of pocket
            if (!(draggedBlock.getBlock() instanceof HatBlock)) {
                BlockView.StatementSocketInfo stmtSocket = otherBlock.findStatementSocketNear(draggedTopNotchX, y);
                //Log.d("stmtSocket", stmtSocket != null ? stmtSocket.toString() : "null");
                if (stmtSocket != null) {
                    targetStatementSocket = stmtSocket;
                    targetStatementSocketBlock = otherBlock;
                    DragProjectionState.setProjection(stmtSocket.input, draggedBlock.getBlock());
                    otherBlock.makeDirty();
                    otherBlock.requestLayout();
                    otherBlock.reOrderBlocks();
                    otherBlock.invalidate();
                    shadowBlock = new ShadowBlockView(
                            getContext(),
                            draggedBlock.getBlock(),
                            stmtSocket.x ,
                            stmtSocket.y
                    );
                    addView(shadowBlock);
                    return;
                }
            }

            float[] otherBottomPos = otherBlock.getNextConnectionPosition();
            if (otherBottomPos != null) {
                float distance = (float) Math.sqrt(
                        Math.pow(draggedTopNotchX - otherBottomPos[0], 2) +
                                Math.pow(y - otherBottomPos[1], 2)
                );

                if (distance < BlockView.CONNECTION_SNAP_DISTANCE && !(draggedBlock.getBlock() instanceof HatBlock)) {
                    targetConnectionBlock = otherBlock;
                    isConnectingToTop = false;

                    float shadowX = otherBottomPos[0] - Constants.NOTCH_OFFSET_LEFT - Constants.NOTCH_WIDTH / 2;
                    float shadowY = otherBottomPos[1];
                    shadowBlock = new ShadowBlockView(
                            getContext(),
                            draggedBlock.getBlock(),
                            shadowX,
                            shadowY
                    );
                    addView(shadowBlock);
                    DragProjectionState.setNextConnectionProjection(otherBlock.getBlock(), draggedBlock.getBlock());
                    otherBlock.propagateDirtyUp();
                    otherBlock.requestLayout();
                    otherBlock.reOrderBlocks();
                    otherBlock.invalidate();
                    return;
                }
            }

            if (otherBlock.getParentBlock() == null
                    && otherBlock.getParentStatementInputView() == null
                    && otherBlock.isPreviousConnectionNear(draggedBottomNotchX, draggedBottomNotchY)
                    && (targetStatementSocket == null || !targetStatementSocket.input.connection.isConnected())) {

                targetConnectionBlock = otherBlock;
                isConnectingToTop = true;

                float[] connectionPos = otherBlock.getPreviousConnectionPosition();
                if (connectionPos != null) {
                    BlockView lastBlock = draggedBlock.getLastBlockInStack();
                    float shadowX = connectionPos[0] - Constants.NOTCH_OFFSET_LEFT - Constants.NOTCH_WIDTH / 2;
                    float shadowY = connectionPos[1] - lastBlock.getMeasurement().height;

                    shadowBlock = new ShadowBlockView(
                            getContext(),
                            lastBlock.getBlock(),
                            shadowX,
                            shadowY
                    );
                    addView(shadowBlock);
                }
                return;
            }
        }
    }

    private boolean isInDraggedChain(BlockView draggedBlock, BlockView candidate) {
        BlockView current = draggedBlock.getNext();
        while (current != null) {
            if (current == candidate) return true;
            current = current.getNext();
        }
        return false;
    }
    public void deleteSpriteGroup(SpriteGroup group){
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof BlockView bv) {
                if (bv.getOwner() == group) {
                    bv.deleteStack();
                    removeView(bv);

                }
            }
        }
    }

    public void onBlockDropped(BlockView draggedBlock, float x, float y) {
        FrameLayout dragOverlay = getRootView().findViewById(R.id.dragOverlay);
        if (dragOverlay.indexOfChild(draggedBlock) != -1) {
            dragOverlay.removeView(draggedBlock);
            draggedBlock.moveStack(this);


            draggedBlock.setX(x);
            draggedBlock.setY(y);

            // Remove the per-block scale that was applied when entering the overlay.
            draggedBlock.setScaleX(1f);
            draggedBlock.setScaleY(1f);

            draggedBlock.reOrderBlocks();
        }
        if (shadowBlock != null) {
            removeView(shadowBlock);
            shadowBlock = null;
        }
        DragProjectionState.clear();
        if (targetStatementSocket != null && targetStatementSocketBlock != null) {
            draggedBlock.setX(targetStatementSocket.x);
            draggedBlock.setY(targetStatementSocket.y);
            if(targetStatementSocket.input.connection.isConnected()){
                BlockView oldNext = BlockView.getBlockViewByBlock(targetStatementSocket.input.connection.getTargetBlock());
                oldNext.disconnectFromStatementInput();
                draggedBlock.connectToStatementInput(targetStatementSocketBlock, targetStatementSocket.input);
                draggedBlock.connectNext(oldNext);
            }else{
                draggedBlock.connectToStatementInput(targetStatementSocketBlock, targetStatementSocket.input);
            }
            targetStatementSocket = null;
            targetStatementSocketBlock = null;
        } else if (targetSocket != null && targetConnectionBlock != null) {
            draggedBlock.setX(targetSocket.x);
            draggedBlock.setY(targetSocket.y);
            draggedBlock.connectToInput(targetConnectionBlock, targetSocket.input);
            targetConnectionBlock.makeDirty();
            targetConnectionBlock.reOrderBlocks();
            targetConnectionBlock.invalidate();
            targetConnectionBlock.requestLayout();
            targetSocket = null;
            targetConnectionBlock = null;

        } else if (targetConnectionBlock != null) {
            if (!isConnectingToTop) {
                float[] connectionPos = targetConnectionBlock.getNextConnectionPosition();
                if (connectionPos != null) {
                    float snapX = connectionPos[0] - Constants.NOTCH_OFFSET_LEFT - Constants.NOTCH_WIDTH / 2;
                    float snapY = connectionPos[1];
                    draggedBlock.setX(snapX);
                    draggedBlock.setY(snapY);

                    BlockView oldNext = targetConnectionBlock.getNext();
                    if (oldNext != null) {
                        targetConnectionBlock.disconnectNext();
                        targetConnectionBlock.connectNext(draggedBlock);
                        draggedBlock.connectNext(oldNext);

                        BlockView root = targetConnectionBlock;
                        while (root.getParentBlock() != null) root = root.getParentBlock();
                        root.reOrderBlocks();
                    } else {
                        targetConnectionBlock.connectNext(draggedBlock);
                    }
                }
            } else {
                float[] connectionPos = targetConnectionBlock.getPreviousConnectionPosition();
                if (connectionPos != null) {
                    float snapX = connectionPos[0] - Constants.NOTCH_OFFSET_LEFT - Constants.NOTCH_WIDTH / 2;
                    float snapY = connectionPos[1] - draggedBlock.getStackHeight();
                    draggedBlock.setX(snapX);
                    draggedBlock.setY(snapY);
                    BlockView current = draggedBlock;
                    while (current.getNext() != null) {
                        current = current.getNext();
                    }
                    current.connectNext(targetConnectionBlock);
                }
            }
            targetConnectionBlock = null;
            isConnectingToTop = false;
        }

        if (draggedBlock.getBlock() instanceof Expression<?>) {
            if (draggedBlock.getParentBlock() != null) {
                draggedBlock.getParentBlock().invalidate();
                draggedBlock.getParentBlock().requestLayout();
            }
        }
        draggedBlock.invalidate();
        draggedBlock.reOrderBlocks();
    }

    /**
     * Shows an inline EditText over an editable field.
     *
     * @param block   the BlockView that owns the field
     * @param fm      measurement data for the field
     * @param canvasX x position in canvas (block) coordinates
     * @param canvasY y position in canvas (block) coordinates
     */
    public void showFieldEditor(BlockView block, BlockRenderer.FieldMeasurement fm,
                                float canvasX, float canvasY) {
        if (isPlayMode && !activeGroup.hasTag(Actor.Tag.PLAYER)) {
            return;
        }
        fieldOverlay.bringToFront();
        removeAllExcept(fieldOverlay,variableHolder);

        EditText editText = new EditText(getContext());
        editText.setText(fm.field.getText());
        editText.setBackground(null);
        editText.setTextColor(Color.TRANSPARENT);
        editText.setTypeface(Typeface.SANS_SERIF);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Constants.FIELD_TEXT_FONT_SIZE);
        editText.setPadding(0, 0, 0, 0);
        editText.setIncludeFontPadding(false);
        editText.setGravity(Gravity.START | Gravity.TOP);
        editText.setMaxLines(1);
        editText.setHorizontallyScrolling(true);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(Constants.FIELD_TEXT_FONT_SIZE);
        p.setTypeface(Typeface.SANS_SERIF);

        float textWidth = p.measureText(fm.text);

        // fieldOverlay is translated by (scrollX, scrollY) so it stays pinned to the
        // viewport.  Convert canvas cords to viewport cords by subtracting scroll.
        float viewportX = canvasX - getScrollX();
        float viewportY = canvasY - getScrollY();

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Math.max((int) textWidth, (int) fm.width),
                (int) fm.height
        );
        lp.leftMargin = (int) (viewportX - textWidth / 2f);
        lp.topMargin  = (int) (viewportY + p.getFontMetrics().ascent);

        fieldOverlay.addView(editText, lp);
        editText.requestFocus();

        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                float newWidth = p.measureText(s.toString());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) editText.getLayoutParams();
                lp.width = (int) Math.max(newWidth, fm.width);
                editText.setLayoutParams(lp);

                if (fm.field instanceof TextField tx) tx.setValue(s.toString());
                else if (fm.field instanceof NumberField nf) nf.setValue(s.toString());
                block.makeDirty();
                block.requestLayout();
                block.invalidate();
            }
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) removeAllExcept(fieldOverlay,variableHolder);
        });
    }
    public World getWorld(){
        return activeGroup.getActor(0).getWorld();
    }

    public SpriteGroup getActiveGroup() {
        return activeGroup;
    }
    public static void removeAllExcept(ViewGroup parent, View... keep) {
        Set<View> keepSet = new HashSet<>(Arrays.asList(keep));
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                View child = parent.getChildAt(i);
                if (child == null) continue;

                if (!keepSet.contains(child)) {
                    parent.removeView(child);
                }
            }
    }
    public void reset() {
        scaleFactor = 1f;
        removeAllExcept(this, fieldOverlay,activeActorImage);
        post(() -> {
            float centerX = CANVAS_WIDTH / 2f;
            float centerY = CANVAS_HEIGHT / 2f;

            float halfViewportX = getWidth() / (2f * scaleFactor);
            float halfViewportY = getHeight() / (2f * scaleFactor);

            int scrollX = (int)(centerX - halfViewportX);
            int scrollY = (int)(centerY - halfViewportY);

            scrollTo(scrollX, scrollY);
        });

    }
}