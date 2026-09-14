package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;

public class BlockPreview extends View {

    private Block block;
    private final BlockRenderer renderer = new BlockRenderer();
    private int color;

    public BlockPreview(Context c){ super(c); }

    public void setBlock(Block b, int color){
        this.block = b;
        this.color = color;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        BlockRenderer.BlockMeasurement measurement = renderer.measureBlock(block);
        int w = (int) measurement.width;
        int h = (int) (measurement.height + (block instanceof Statement ? Constants.NOTCH_HEIGHT : 0)) + 1;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas){
        if(block!=null)
            renderer.renderBlock(canvas,block,0,0,color,false,false);
    }
}

