package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.graphics.Path;

import androidx.core.graphics.PathParser;

/**
 * notches were the most expensive part of the whole rendering processes
 * costing up to 1.3ms per call. as such I made this class to solve this issue
 */
public class NotchRender {
    private static final Path TOP_BASE;
    private static final Path BOTTOM_BASE;
    public static final PathProgram  TOP_BASE_POINT_LIST;
    public static final PathProgram  BOTTOM_BASE_POINT_LIST;

    static {
        TOP_BASE = PathParser.createPathFromPathData(
                "m0,0 c2,0 3,1 4,2 l4,4 c1,1 2,2 4,2 h12 c2,0 3,-1 4,-2 l4,-4 c1,-1 2,-2 4,-2"
        );

        BOTTOM_BASE = PathParser.createPathFromPathData(
                "m0,0 c-2,0 -3,1 -4,2 l-4,4 c-1,1 -2,2 -4,2 h-12 c-2,0 -3,-1 -4,-2 l-4,-4 c-1,-1 -2,-2 -4,-2"
        );
        TOP_BASE_POINT_LIST =  SvgToDelta.parse(BlockRenderer.pathToSvg(TOP_BASE));
        BOTTOM_BASE_POINT_LIST = SvgToDelta.parse(BlockRenderer.pathToSvg(BOTTOM_BASE));

    }
    private static final class SvgToDelta {

        private static PathProgram parse(String svg) {
            String[] tokens = svg
                    .replace(",", " ")
                    .trim()
                    .split("\\s+");
            float[] dx = new float[256];
            float[] dy = new float[256];
            int count = 0;
            float cx = 0;
            float cy = 0;
            char mode = 0;
            for (int i = 0; i < tokens.length; ) {
                String t = tokens[i];
                if (t.length() == 1 && Character.isLetter(t.charAt(0))) {
                    mode = t.charAt(0);
                    i++;
                    continue;
                }
                float x = Float.parseFloat(tokens[i]);
                float y = Float.parseFloat(tokens[i + 1]);
                i += 2;

                switch (mode) {
                    case 'm':
                        cx = x;
                        cy = y;
                        break;

                    case 'M':
                        cx = x;
                        cy = y;
                        break;

                    case 'l':
                        cx += x;
                        cy += y;

                        dx[count] = x;
                        dy[count] = y;
                        count++;
                        break;

                    case 'L':
                        dx[count] = x - cx;
                        dy[count] = y - cy;
                        cx = x;
                        cy = y;
                        count++;
                        break;
                }
            }
            PathProgram out = new PathProgram();
            out.dx = java.util.Arrays.copyOf(dx, count);
            out.dy = java.util.Arrays.copyOf(dy, count);
            out.count = count;

            return out;
        }
    }
    public static class PathProgram {
        public float[] dx;
        public float[] dy;
        public int count;
    }
}
