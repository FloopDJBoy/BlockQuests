package FloopDJBoy.floopdjboy.blockquest.Profiler;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

public class ProfilingConstraintLayout extends ConstraintLayout {

    private int frames = 0;

    public ProfilingConstraintLayout(Context context) {
        super(context);
    }

    public ProfilingConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfilingConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isInEditMode()) {
            super.dispatchDraw(canvas);
            return;
        }

        BlockProfiler.beginFrame(++frames);

        try {
            super.dispatchDraw(canvas);
        } finally {
            BlockProfiler.endFrame();
        }
    }
}
