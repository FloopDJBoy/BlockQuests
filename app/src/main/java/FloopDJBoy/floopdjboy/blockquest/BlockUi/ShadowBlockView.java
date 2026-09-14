package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;

/**
 * ShadowBlockView is a temporary visual preview that shows where a block will
 * be placed when dropped. It does not participate in touch events or AST connections.
 */
public class ShadowBlockView extends View {
    private final BlockRenderer blockRenderer = new BlockRenderer();
    private final Block block;

    public ShadowBlockView(Context context, Block block, float x, float y) {
        super(context);
        this.block = block;
        setX(x);
        setY(y);
        setAlpha(0.2f);
        // Don't intercept touch events
        setClickable(false);
        setFocusable(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        BlockRenderer.BlockMeasurement measurement = blockRenderer.measureBlock(block);
        int w = (int) measurement.width;
        int h = (int) (measurement.height + (block instanceof Statement ? Constants.NOTCH_HEIGHT : 0)) + 1;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (block != null) {
            // Render as ghost (no fields/inputs shown)
            blockRenderer.renderBlock(canvas, block, 0, 0, Color.BLACK, true,false);
        }
    }
}
