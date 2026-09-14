package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.EditMode;
import FloopDJBoy.floopdjboy.blockquest.Workspace;
import FloopDJBoy.floopdjboy.blockquest.World;

/**
 * ActorLayerView is a FrameLayout overlay that sits on top of the stage RecyclerView.
 * It owns one ImageView per actor and handles:
 * - Sprite sheet slicing (directional + animated)
 * - Smooth tile-to-tile interpolation
 * - Walk animation cycling
 * - Fallback to single static sprite
 */
public class ActorLayerView extends FrameLayout {

    /** Milliseconds per animation frame */
    public static final int FRAME_DURATION_MS = 100;

    /** Number of animation frames per direction */
    private static final int FRAMES_PER_DIR = 4;

    /** Source cell size in the sprite sheet (pixels) */
    private static final int SPRITE_CELL_SIZE = 24;
    /** Gap between sprite cells (pixels) */
    private static final int GAP = 1;

    private World world;
    private int tileSize = 0;

    /** One ImageView per actor */
    private final Map<Actor, ImageView> actorViews = new HashMap<>();
    Workspace workspace;

    /**
     * Cache of sliced bitmaps per resource id.
     * Key: resourceId
     * Value: list of bitmaps in strip order [dir0frame0, dir0frame1, ..., dir3frame3]
     *        OR a single-element list if the sprite is not a strip.
     */
    private final SparseArray<List<Bitmap>> spriteCache = new SparseArray<>();
    private OnTileClickListener tileClickListener;

    public interface OnTileClickListener {
        /** Called when a tile is tapped in the stage grid.
         *  @param x tile column (0-based)
         *  @param y tile row    (0-based) */
        EditMode onTileClick(int x, int y);
    }
    public void setOnTileClickListener(OnTileClickListener l) {
        this.tileClickListener = l;
    }
    private boolean running = false;
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) return;

            updateActorViews();

            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    public ActorLayerView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
    }


    /**
     * Bind the world and tile size, then build ImageViews for all current actors.
     */
    public void init(World world, int tileSize,Workspace workspace) {
        this.world = world;
        this.workspace = workspace;
        this.tileSize = Math.min(getWidth() / world.getWidth(),getHeight() / world.getHeight());
        rebuild();
    }

    /**
     * Update tile size (called if layout changes).
     */
    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
        // Reposition all views
        for (Actor actor : actorViews.keySet()) {
            positionView(actorViews.get(actor), actor.getX(), actor.getY(), actor.getX(), actor.getY(), 1f, 0);
        }
    }

    /**
     * Rebuild all actor ImageViews from scratch.
     * Call this when the actor set changes.
     */
    public void rebuild() {
        removeAllViews();
        actorViews.clear();

        if (world == null || tileSize == 0) return;

        for (Actor actor : world.getAllActors()) {
            addActorView(actor);
        }
    }

    public void addActorView(Actor actor) {
        if (actorViews.containsKey(actor)) return;

        ImageView iv = new ImageView(getContext());
        iv.setScaleType(ImageView.ScaleType.FIT_XY);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tileSize, tileSize);
        addView(iv, lp);
        iv.setOnClickListener((v) -> {
            if(tileClickListener != null && tileClickListener.onTileClick(actor.getX(),actor.getY()) == EditMode.NONE) {
                workspace.setActiveActor(actor);
            }
        });
        actorViews.put(actor, iv);

        // Position immediately so it appears at the right tile
        positionView(iv, actor.getX(), actor.getY(), actor.getX(), actor.getY(), 1f, actor.getSpriteResourceId());

        // Set initial sprite
        Bitmap frame = getFrame(actor, 0, false);
        if (frame != null) iv.setImageBitmap(frame);
        else iv.setImageResource(actor.getSpriteResourceId());
    }
    public void removeActorView(Actor actor) {
        ImageView iv = actorViews.remove(actor);
        if (iv != null) removeView(iv);
    }

    /**
     * Start the animation loop.
     */
    public void startAnimating() {
        if (running) return;
        running = true;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }
    public void updateTileStacking() {
        if (world == null) return;

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                ArrayList<Actor> actors = world.getActors(x, y);
                for (Actor actor : actors) {
                    ImageView iv = actorViews.get(actor);
                    if (iv != null) {
                        iv.bringToFront(); // last in the list = topmost
                    }
                }
            }
        }
    }
    /**
     * Stop the animation loop.
     */
    public void stopAnimating() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    /**
     * Called every FRAME_DURATION_MS. Updates position and sprite frame for each actor.
     */
    private void updateActorViews() {
        if (tileSize == 0 || world == null) return;

        long now = System.currentTimeMillis();
        // Total animation duration = FRAMES_PER_DIR * FRAME_DURATION_MS
        long animDuration = (long) FRAMES_PER_DIR * FRAME_DURATION_MS;

        for (Actor actor : world.getAllActors()) {
            ImageView iv = actorViews.get(actor);
            if (iv == null) continue;

            if (!actor.isAlive()) {
                iv.setVisibility(View.GONE);
                continue;
            }
            iv.setVisibility(View.VISIBLE);

            long moveStart = actor.getMoveStartTime();
            boolean animating = moveStart >= 0 && (now - moveStart) < animDuration;

            float progress = 1f;
            int frameIndex = 0;

            if (animating) {
                long elapsed = now - moveStart;
                progress = Math.min(1f, (float) elapsed / animDuration);
                frameIndex = (int) (elapsed / FRAME_DURATION_MS) % FRAMES_PER_DIR;
            }

            // Update sprite frame
            Bitmap frame = getFrame(actor, frameIndex, animating);
            if (frame != null) {
                iv.setImageBitmap(frame);
            } else {
                iv.setImageResource(actor.getSpriteResourceId());
            }

            // Update position (interpolate from prev to current)
            positionView(iv, actor.getPrevX(), actor.getPrevY(), actor.getX(), actor.getY(), progress, actor.getSpriteResourceId());
        }
    }

    /**
     * Position an ImageView by interpolating between two tile positions.
     */
    private void positionView(ImageView iv, int fromX, int fromY, int toX, int toY, float progress, int resourceId) {
        float pixelFromX = fromX * tileSize;
        float pixelFromY = fromY * tileSize;
        float pixelToX   = toX * tileSize;
        float pixelToY   = toY * tileSize;

        float x = pixelFromX + (pixelToX - pixelFromX) * progress;
        float y = pixelFromY + (pixelToY - pixelFromY) * progress;

        iv.setX(x);
        iv.setY(y);
        iv.setZ(99);
    }

    /**
     * Get the correct bitmap frame for an actor.
     *
     * @param actor      the actor
     * @param frameIndex 0-3, which animation frame within the direction
     * @param animating  true if currently walking
     * @return the correct Bitmap, or null if we should fall back to setImageResource
     */
    private Bitmap getFrame(Actor actor, int frameIndex, boolean animating) {
        int resId = actor.getSpriteResourceId();
        List<Bitmap> frames = getOrLoadSprite(resId);

        if (frames == null || frames.isEmpty()) return null;

        // Single sprite fallback — no slicing needed, return null to use setImageResource
        if (frames.size() == 1) return null;

        // Directional strip: pick direction group then frame within it
        int dirIndex = actor.getFacing().getSpriteFrameIndex();
        int index = dirIndex * FRAMES_PER_DIR + (animating ? frameIndex : 0);
        if (index >= frames.size()) index = dirIndex * FRAMES_PER_DIR; // safety clamp

        return frames.get(index);
    }

    /**
     * Load and slice a sprite resource, caching the result.
     * Returns a list of Bitmaps:
     *   - size == 1: single static sprite
     *   - size == FRAMES_PER_DIR * 4 (or more): directional strip
     */
    private List<Bitmap> getOrLoadSprite(int resId) {
        List<Bitmap> cached = spriteCache.get(resId);
        if (cached != null) return cached;

        Bitmap raw;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            raw = BitmapFactory.decodeResource(getResources(), resId,options);
        } catch (Exception e) {
            return null;
        }

        if (raw == null){
            return null;
        }

        List<Bitmap> frames = new ArrayList<>();

        int w = raw.getWidth();
        int h = raw.getHeight();

        if (w <= h) {
            // Single sprite (square or taller than wide) — no slicing
            frames.add(raw);
        } else {
            // Sprite strip — slice into SPRITE_CELL_SIZE wide cells
            // Scale factor: source cell is SPRITE_CELL_SIZE px, target is tileSize px
            int totalFrames = (w + GAP) / (SPRITE_CELL_SIZE + GAP);
            for (int i = 0; i < totalFrames; i++) {
                int srcX = i * SPRITE_CELL_SIZE + i*GAP;
                // Guard against strips that are slightly off
                if (srcX + SPRITE_CELL_SIZE > w) break;
                Bitmap cell = Bitmap.createBitmap(raw, srcX, 0, SPRITE_CELL_SIZE, h);
                // Scale to tileSize
                Bitmap scaled = Bitmap.createScaledBitmap(cell, tileSize, tileSize, false);
                cell.recycle();
                frames.add(scaled);
            }
            raw.recycle();
        }

        spriteCache.put(resId, frames);
        return frames;
    }
    public Bitmap getPreviewFrame(Actor actor) {
        return getFrame(actor, 0, false);
    }
    public Bitmap getPreviewFrame(int resId) {
        return getOrLoadSprite(resId).get(0);
    }
    /**
     * Clear the sprite cache (call when resources change).
     */
    public void clearSpriteCache() {
        for (int i = 0; i < spriteCache.size(); i++) {
            List<Bitmap> frames = spriteCache.valueAt(i);
            for (Bitmap b : frames) {
                if (!b.isRecycled()) b.recycle();
            }
        }
        spriteCache.clear();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimating();
        clearSpriteCache();
    }
}