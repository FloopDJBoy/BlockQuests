package FloopDJBoy.floopdjboy.blockquest.BlockSerialization;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import FloopDJBoy.floopdjboy.blockquest.R;

/**
 * LevelPreviewRenderer generates a {@link Bitmap} thumbnail of a saved level
 * from its JSON string.
 *
 * <p>Rendering is done on a background thread. The result is delivered on the
 * main thread via {@link PreviewCallback}.</p>
 *
 * <p>Each call to {@link #render} is independent — no shared bitmap cache is
 * used so that tile sizes (which vary per container width) never collide with
 * each other or with the game's own {@code ActorLayerView} sprite cache.</p>
 *
 * Aspect ratio is always 4:3 (width:height).
 *
 * Usage:
 * <pre>
 *     LevelPreviewRenderer.render(
 *         resources, jsonString, containerWidthPx,
 *         bitmap -> imageView.setImageBitmap(bitmap)
 *     );
 * </pre>
 */
public final class LevelPreviewRenderer {

    /** Source cell size in the sprite sheet (pixels) — must match ActorLayerView. */
    private static final int SPRITE_CELL_SIZE = 24;
    /** Gap between cells in a sprite strip — must match ActorLayerView. */
    private static final int SPRITE_GAP = 1;

    /** Shared single-thread executor so previews are serialised and never flood the CPU. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** Callback delivered on the main thread after rendering finishes. */
    public interface PreviewCallback {
        /**
         * @param bitmap The rendered preview, or {@code null} if rendering failed
         *               (e.g. malformed JSON).
         */
        void onPreviewReady(Bitmap bitmap);
    }

    private LevelPreviewRenderer() { /* utility class — no instances */ }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Render a level preview asynchronously.
     *
     * @param resources    Android {@link Resources} used to decode drawables.
     * @param levelJson    The JSON string produced by {@link Serializer#Serialize}.
     * @param targetWidth  The desired width of the output bitmap in pixels.
     *                     Height is derived automatically to maintain a 4:3 ratio.
     * @param callback     Delivers the finished {@link Bitmap} on the main thread.
     */
    public static void render(Resources resources,
                              String levelJson,
                              int targetWidth,
                              PreviewCallback callback) {

        EXECUTOR.execute(() -> {
            Bitmap result = null;
            try {
                result = renderSync(resources, levelJson, targetWidth);
            } catch (Exception e) {
                // result stays null — caller receives null in callback
            }

            final Bitmap finalResult = result;
            MAIN_HANDLER.post(() -> callback.onPreviewReady(finalResult));
        });
    }

    // -----------------------------------------------------------------------
    // Core rendering (runs on background thread)
    // -----------------------------------------------------------------------

    private static Bitmap renderSync(Resources resources,
                                     String levelJson,
                                     int targetWidth) {

        // --- 1. Parse JSON ---
        DataTypes.WorldData worldData = new Gson().fromJson(levelJson, DataTypes.WorldData.class);
        if (worldData == null) return null;

        int worldW = worldData.width;
        int worldH = worldData.height;
        if (worldW <= 0 || worldH <= 0) return null;

        // --- 2. Compute bitmap dimensions (4:3, width-driven) ---
        int bitmapWidth  = targetWidth;
        int bitmapHeight = (targetWidth * 3) / 4;

        // Tile size: fit the grid into the bitmap, keeping tiles square.
        int tileW = bitmapWidth  / worldW;
        int tileH = bitmapHeight / worldH;
        int tileSize = Math.min(tileW, tileH);
        if (tileSize <= 0) return null;

        // Grid pixel dimensions (may be slightly smaller than bitmap due to integer division)
        int gridPixelW = tileSize * worldW;
        int gridPixelH = tileSize * worldH;

        // Centre the grid within the bitmap
        int offsetX = (bitmapWidth  - gridPixelW) / 2;
        int offsetY = (bitmapHeight - gridPixelH) / 2;

        // --- 3. Create output bitmap & canvas ---
        Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // --- 4. Draw tile background (all grass for now) ---
        Bitmap grassTile = decodeTile(resources, R.drawable.grass_tile, tileSize);
        if (grassTile != null) {
            for (int y = 0; y < worldH; y++) {
                for (int x = 0; x < worldW; x++) {
                    canvas.drawBitmap(grassTile,
                            offsetX + x * tileSize,
                            offsetY + y * tileSize,
                            null);
                }
            }
        } else {
            // Fallback: fill with a solid colour so the grid is at least visible
            Paint fallback = new Paint();
            fallback.setColor(0xFF4CAF50); // green
            canvas.drawRect(offsetX, offsetY,
                    offsetX + gridPixelW,
                    offsetY + gridPixelH,
                    fallback);
        }
        // --- 4.5 Draw grid lines ---
        Paint gridPaint = new Paint();
        gridPaint.setColor(0x55000000); // semi-transparent black
        gridPaint.setStrokeWidth(Math.max(1f, tileSize * 0.05f)); // scale with tile

        // vertical lines
        for (int x = 0; x <= worldW; x++) {
            float drawX = offsetX + x * tileSize;
            canvas.drawLine(drawX, offsetY, drawX, offsetY + gridPixelH, gridPaint);
        }

        // horizontal lines
        for (int y = 0; y <= worldH; y++) {
            float drawY = offsetY + y * tileSize;
            canvas.drawLine(offsetX, drawY, offsetX + gridPixelW, drawY, gridPaint);
        }

        // --- 5. Draw actors ---
        if (worldData.spriteGroups != null) {
            for (DataTypes.SpriteGroupData groupData : worldData.spriteGroups.values()) {
                if (groupData == null || groupData.actors == null) continue;

                // All actors in a group share the same sprite resource id.
                // We load it once per group.
                int spriteResId = groupData.sprite;
                Bitmap actorFrame = loadFirstFrame(resources, spriteResId, tileSize);

                for (DataTypes.ActorData actorData : groupData.actors.values()) {
                    if (actorData == null) continue;
                    if (actorData.x < 0 || actorData.x >= worldW) continue;
                    if (actorData.y < 0 || actorData.y >= worldH) continue;

                    float drawX = offsetX + actorData.x * tileSize;
                    float drawY = offsetY + actorData.y * tileSize;

                    if (actorFrame != null) {
                        canvas.drawBitmap(actorFrame, drawX, drawY, null);
                    }
                }
            }
        }

        // Recycle intermediaries (not the output)
        if (grassTile != null) grassTile.recycle();

        return output;
    }

    // -----------------------------------------------------------------------
    // Sprite helpers (isolated — no shared cache)
    // -----------------------------------------------------------------------

    /**
     * Decode a tile drawable and scale it to {@code tileSize × tileSize}.
     */
    private static Bitmap decodeTile(Resources resources, int resId, int tileSize) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            Bitmap raw = BitmapFactory.decodeResource(resources, resId, opts);
            if (raw == null) return null;

            Bitmap scaled = Bitmap.createScaledBitmap(raw, tileSize, tileSize, false);
            if (scaled != raw) raw.recycle();
            return scaled;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Load direction-0 / frame-0 of an actor sprite.
     *
     * <p>Mirrors the slicing logic of {@code ActorLayerView.getOrLoadSprite}:
     * if the raw bitmap is wider than it is tall it is treated as a horizontal
     * strip of {@code SPRITE_CELL_SIZE × h} cells separated by {@code SPRITE_GAP}
     * pixels; otherwise it is a single static sprite.</p>
     *
     * <p>This method has <em>no shared cache</em> — each call decodes and slices
     * independently so that the tile size used here never pollutes the game's own
     * sprite cache (which is keyed by resource id against a different tile size).</p>
     *
     * @return A {@code tileSize × tileSize} bitmap of the first frame, or
     *         {@code null} if decoding fails.
     */
    private static Bitmap loadFirstFrame(Resources resources, int resId, int tileSize) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            Bitmap raw = BitmapFactory.decodeResource(resources, resId, opts);
            if (raw == null) return null;

            int w = raw.getWidth();
            int h = raw.getHeight();
            Log.d("SPRITE", "raw size: " + raw.getWidth() + "x" + raw.getHeight());

            Bitmap cell;
            if (w <= h) {
                // Single static sprite — use as-is
                cell = raw;
            } else {
                // Sprite strip — extract cell 0 (direction 0, frame 0)
                // Guard: strip must have at least one full cell
                if (w < SPRITE_CELL_SIZE || h < 1) {
                    cell = raw;
                } else {
                    cell = Bitmap.createBitmap(raw, 0, 0, SPRITE_CELL_SIZE, h);
                    raw.recycle();
                }
            }

            // Scale the cell to tileSize × tileSize
            Bitmap scaled = Bitmap.createScaledBitmap(cell, tileSize/3, tileSize/3, false);
            if (scaled != cell) cell.recycle();
            return scaled;

        } catch (Exception e) {
            return null;
        }
    }
}