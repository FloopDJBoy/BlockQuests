package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.Field;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.NumberField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.TextField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.Input;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Fields.LabelField;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.HatBlocks.HatBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.InputType;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.ExpressionInput;
import FloopDJBoy.floopdjboy.blockquest.Profiler.BlockProfiler;

/**
 * <p>BlockRenderer is responsible for rendering blocks in Blockly/Zalos style.</p>
 * <p>It handles the visual representation of {@link Block} including their shape, {@link Input}, and {@link Field}.</p>
 *
 */
public class BlockRenderer {

    // Rendering context
    private Canvas canvas;
    private Paint blockPaint;
    private Paint strokePaint;
    private Paint textPaint;
    private Paint texFieldPaint;
    private Paint texFieldTextPaint;
    private Paint textFieldStrokePaint;
    private Paint socketPaint;
    private Paint hightlightPaint;
    private static final HashMap<Block,BlockMeasurement> blockMeasurements = new HashMap<>();

    // Block dimensions (calculated during measurement)
    private float blockWidth;
    private float blockHeight;

    // Current rendering position
    private float currentX;
    private float currentY;

    public HashMap<Path,FieldMeasurement> touchRegions = new HashMap<>();
    private final static boolean ENABLE_CACHE = true;


    public BlockRenderer() {
        initializePaints();
    }

    private void initializePaints() {
        // Highlight paint
        hightlightPaint = new Paint();
        hightlightPaint.setAntiAlias(true);
        hightlightPaint.setStyle(Paint.Style.STROKE);
        hightlightPaint.setStrokeWidth(6);
        hightlightPaint.setColor(Color.parseColor("#FFF200"));


        // Block fill paint
        blockPaint = new Paint();
        blockPaint.setAntiAlias(true);
        blockPaint.setStyle(Paint.Style.FILL);

        //SocketPaint
        socketPaint = new Paint();
        socketPaint.setAntiAlias(true);
        socketPaint.setStyle(Paint.Style.FILL);


        // Stroke paint for outlines
        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1);
        strokePaint.setColor(0xFF000000); // Black outline

        // Text paint for fields
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(Constants.FIELD_TEXT_FONT_SIZE);
        textPaint.setTypeface(Typeface.SANS_SERIF);

        textPaint.setColor(0xFFFFFFFF); // White text
        texFieldPaint = new Paint();
        texFieldPaint.setColor(0xFFFFFFFF);
        texFieldPaint.setStyle(Paint.Style.FILL);
        texFieldPaint.setAntiAlias(true);

        textFieldStrokePaint = new Paint();
        textFieldStrokePaint.setColor(blockPaint.getColor());
        textFieldStrokePaint.setStyle(Paint.Style.STROKE);
        textFieldStrokePaint.setAntiAlias(true);

        // Reusable paint for TextField text (black, opaque)
        texFieldTextPaint = new Paint();
        texFieldTextPaint.setColor(0xFF000000);
        texFieldTextPaint.setStyle(Paint.Style.FILL);
        texFieldTextPaint.setAntiAlias(true);
        texFieldTextPaint.setTextSize(Constants.FIELD_TEXT_FONT_SIZE);
        texFieldTextPaint.setTypeface(Typeface.SANS_SERIF);
    }
    public void renderBlock(Canvas canvas, Block block, float x, float y, int color,boolean isHighlighted) {
        renderBlock(canvas, block, x, y, color, false,isHighlighted);
    }
    /**
     * Render a block at the specified position
     * @param canvas The canvas to draw on
     * @param block The block to render
     * @param x The x-coordinate to start rendering
     * @param y The y-coordinate to start rendering
     * @param color The color of the block
     */
    public void renderBlock(Canvas canvas, Block block, float x, float y,
                            int color, boolean isGhost, boolean isHighlighted) {
        try (var ignored = BlockProfiler.scope("renderBlock")) {

            touchRegions.clear();
            this.canvas = canvas;
            this.currentX = x;
            this.currentY = y;

            try (var s = BlockProfiler.scope("setColors")) {
                blockPaint.setColor(color);
                textFieldStrokePaint.setColor(color);
                socketPaint.setColor(darkenColor(color, 0.79f));
            }
            BlockMeasurement measurement;
            try (var s = BlockProfiler.scope("measureBlock")) {
                measurement = measureBlock(block);
            }

            this.blockWidth = measurement.width;
            this.blockHeight = measurement.height;

            Path blockPath;
            try (var s = BlockProfiler.scope("createBlockPath")) {
                blockPath = createBlockPath(block, measurement);
            }

            try (var s = BlockProfiler.scope("drawPath")) {
                canvas.drawPath(blockPath, blockPaint);
                canvas.drawPath(blockPath, strokePaint);

                if (isHighlighted) {
                    canvas.drawPath(blockPath, hightlightPaint);
                }
            }

            if (!isGhost) {
                try (var s = BlockProfiler.scope("renderInputsAndFields")) {
                    renderInputsAndFields(block, measurement);
                }
            }
        }
    }
    private int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);

        return Color.argb(a, r, g, b);
    }


    /**
     * Measure the block to determine its dimensions
     */
    public BlockMeasurement measureBlock(Block block) {
        if(ENABLE_CACHE){
            BlockMeasurement cached = blockMeasurements.get(block);
            if (!block.isDirty() && cached != null) {
                return cached;
            }
        }
        BlockMeasurement measurement = new BlockMeasurement();
        boolean isBooleanExpression = isBooleanExpr(block);
        boolean isRoundExpression = isNonBooleanExpr(block);
        boolean hasInputStatement = false;
        float baseX = isBooleanExpression ? 0 : isRoundExpression ? 0 : Constants.SMALL_PADDING;
        float currentRowWidth = 0;
        float currentRowHeight =
                (isBooleanExpression ? Constants.BOOLEAN_EXPRESSION_HEIGHT :
                        isRoundExpression ? Constants.FIELD_ROUND_HEIGHT :
                                Constants.MIN_BLOCK_HEIGHT);

        float totalHeight =
                (isBooleanExpression || isRoundExpression) ? 0 :
                        Constants.TOP_ROW_MIN_HEIGHT;

        float maxWidth =
                (isBooleanExpression || isRoundExpression) ? 0 :
                        Constants.MIN_BLOCK_WIDTH;


        ArrayList<RowMeasurement> rows = new ArrayList<>();
        RowMeasurement currentRow = new RowMeasurement();
        if (block instanceof Statement && !(block instanceof HatBlock)) {
            measurement.prevNotch = new NotchLayout();
            measurement.prevNotch.x = Constants.NOTCH_OFFSET_LEFT;
            measurement.prevNotch.y = 0;
        }
        for (Input<?> input : block.inputs) {
            InputMeasurement inputMeasurement = measureInput(input);

            // Check if this is an END_ROW or STATEMENT input
            if (input.type == InputType.END_ROW) {
                // Finalize current row
                computeRowTrims(currentRow, isBooleanExpression, isRoundExpression);
                finalizeRow(currentRow, currentRowWidth, currentRowHeight);
                layoutRow(measurement, currentRow, totalHeight, baseX, block instanceof Expression<?>);
                rows.add(currentRow);

                totalHeight += currentRow.height;
                maxWidth = Math.max(maxWidth, currentRowWidth);

                // Start new row
                currentRow = new RowMeasurement();
                currentRowWidth = 0;
                currentRowHeight = Constants.MIN_BLOCK_HEIGHT;
            } else if (input.type == InputType.STATEMENT) {
                // Statement inputs take full width and add vertical space
                hasInputStatement = true;
                computeRowTrims(currentRow, isBooleanExpression, isRoundExpression);
                finalizeRow(currentRow, currentRowWidth, currentRowHeight);
                layoutRow(measurement, currentRow, totalHeight, baseX, block instanceof Expression<?>);
                rows.add(currentRow);

                totalHeight += currentRow.height;
                maxWidth = Math.max(maxWidth, currentRowWidth);
                inputMeasurement.socket.x = Constants.STATEMENT_INPUT_PADDING_LEFT;
                inputMeasurement.socket.y = totalHeight;
                measurement.sockets.put(input, inputMeasurement.socket);

                // Add statement row
                RowMeasurement statementRow = new RowMeasurement();
                statementRow.width = Constants.STATEMENT_INPUT_SPACER_MIN_WIDTH;
                if (inputMeasurement.height > 0) {
                    statementRow.height = inputMeasurement.height;
                } else {
                    statementRow.height = Constants.EMPTY_STATEMENT_INPUT_HEIGHT + Constants.CORNER_RADIUS * 2;
                }
                statementRow.isStatement = true;

                rows.add(statementRow);

                totalHeight += statementRow.height;
                maxWidth = Math.max(maxWidth, statementRow.width + Constants.STATEMENT_INPUT_PADDING_LEFT);

                // Start new row after statement
                currentRow = new RowMeasurement();
                currentRowWidth = 0;
                currentRowHeight = Constants.MIN_BLOCK_HEIGHT;
            } else {
                // Regular input (EXPRESSION or DUMMY)
                currentRowWidth += inputMeasurement.width;
                currentRowHeight = Math.max(currentRowHeight, inputMeasurement.height);
                currentRow.inputs.add(inputMeasurement);
            }
        }

        // Finalize last row if it has content
        if (currentRowWidth > 0 || !currentRow.inputs.isEmpty()) {
            computeRowTrims(currentRow, isBooleanExpression, isRoundExpression);
            finalizeRow(currentRow, currentRowWidth, currentRowHeight);
            layoutRow(measurement, currentRow, totalHeight, baseX, block instanceof Expression<?>);

            rows.add(currentRow);
            totalHeight += currentRow.height;
            maxWidth = Math.max(maxWidth, currentRow.width);
        }

        // Add bottom row height
        if (!isBooleanExpression && !isRoundExpression) {
            totalHeight += Constants.BOTTOM_ROW_MIN_HEIGHT;
            if(hasInputStatement){
                totalHeight += Constants.BOTTOM_ROW_AFTER_STATEMENT_MIN_HEIGHT;
            }
        }

        if (isBooleanExpression) {
            float height = Math.max(totalHeight, Constants.BOOLEAN_EXPRESSION_HEIGHT);
            float sideWidth = height / 2f;
            // Add symmetrical padding
            measurement.width = maxWidth + (2 * sideWidth) ;
            measurement.height = height;
        } else if (isRoundExpression) {
            float height = Math.max(totalHeight, Constants.FIELD_ROUND_HEIGHT);
            float r = height / 2f;
            // Add symmetrical padding
            measurement.width = maxWidth + r;
            measurement.height = height;
        } else {
            // Standard blocks: Add padding for BOTH left and right
            measurement.width = maxWidth + (Constants.SMALL_PADDING * 2);
            measurement.height = totalHeight;
        }
        if (block instanceof Statement) {
            measurement.nextNotch = new NotchLayout();
            measurement.nextNotch.x = Constants.NOTCH_OFFSET_LEFT;
            measurement.nextNotch.y = totalHeight;
            measurement.nextNotch.isNextConnection = true;
        }
        measurement.rows = rows;
        if(ENABLE_CACHE){
            blockMeasurements.put(block,measurement);
            block.makeClean();
        }
        return measurement;
    }
    private static float finalizeRowWidth(RowMeasurement row, float rawWidth) {
        if (row.inputs.isEmpty()) {
            return rawWidth;
        }

        return Math.max(0, rawWidth - row.leadingTrim - row.trailingTrim);
    }
    // Helper method inside BlockRenderer to calculate X/Y the moment a row finishes
    private void layoutRow(BlockMeasurement bm, RowMeasurement row, float rowY, float baseX, boolean isExpression ) {
        float x = baseX;
        int inputIndex = 0;
        for (InputMeasurement im : row.inputs) {
            if (!im.fields.isEmpty() && isExpression && inputIndex == 0) x += Constants.SMALL_PADDING;

            for (int i = 0; i < im.fields.size(); i++) {
                FieldMeasurement fm = im.fields.get(i);
                fm.x = x;
                fm.y = rowY + (row.height - fm.height) / 2f;
                x += fm.width;
                if(i<im.fields.size()-1){
                    x+= Constants.BETWEEN_FIELDS_PADDING;
                }
            }
            if (im.type == InputType.EXPRESSION && im.socket != null) {
                x += Constants.SMALL_PADDING;
                im.socket.x = x;
                if (im.input.connection != null && im.input.connection.isConnected()) {
                    im.socket.y = rowY + (row.height - im.socket.height) / 2f;
                } else if (im.input instanceof ExpressionInput expr && expr.getExpectedType() == Boolean.class) {
                    im.socket.y = rowY + (row.height - Constants.BOOLEAN_EXPRESSION_HEIGHT * 0.75f) / 2f;
                } else {
                    im.socket.y = rowY + (row.height - Constants.FIELD_ROUND_HEIGHT) / 2f;
                }
                x += im.socket.width + Constants.GRID_UNIT;
                bm.sockets.put(im.input, im.socket);
            }
            inputIndex++;
        }
    }

    private static void finalizeRow(RowMeasurement row, float rawWidth, float rawHeight) {
        row.width = finalizeRowWidth(row, rawWidth);
        row.height = Math.max(rawHeight, Constants.MIN_BLOCK_HEIGHT);
    }

    /**
     * Measure an individual input
     */
    private InputMeasurement measureInput(Input<?> input) {
        InputMeasurement measurement = new InputMeasurement();
        measurement.type = input.type;

        float width = 0;
        float height = Constants.MIN_BLOCK_HEIGHT;
        measurement.input = input;
        if (input.type == InputType.EXPRESSION || input.type == InputType.STATEMENT) {
            measurement.socket = new SocketLayout();
            measurement.socket.input = input;
            measurement.socket.isExpression = (input.type == InputType.EXPRESSION);
        }
        // Measure fields
        for (int i = 0; i < input.fields.size(); i++) {
            FieldMeasurement fieldMeasurement = measureField(input.fields.get(i));
            measurement.fields.add(fieldMeasurement);
            width += fieldMeasurement.width;
            height = Math.max(height, fieldMeasurement.height);

            // Add spacing between fields
            if (i < input.fields.size() - 1) {
                width += Constants.BETWEEN_FIELDS_PADDING;
            }
        }

        // Add connection space for expression inputs
        if (input.type == InputType.EXPRESSION) {
            if ((input.connection != null && input.connection.isConnected()) || DragProjectionState.getProjectedBlock(input)!=null) {
                Block connectedBlock = input.connection != null && input.connection.isConnected()
                        ? input.connection.getTargetBlock()
                        : DragProjectionState.getProjectedBlock(input);
                BlockMeasurement connectedMeasurement = measureBlock(connectedBlock);
                width += connectedMeasurement.width +  Constants.SMALL_PADDING*2;
                height = Math.max(height, connectedMeasurement.height + Constants.GRID_UNIT);
                measurement.socket.width = connectedMeasurement.width;
                measurement.socket.height = connectedMeasurement.height;

            } else {
                // Empty expression input
                if (input instanceof ExpressionInput exprInput && exprInput.getExpectedType() == Boolean.class) {
                    float emptyW = Constants.BOOLEAN_EXPRESSION_SIDE_WIDTH * 0.75f
                            + Constants.BOOLEAN_EXPRESSION_HEIGHT * 0.75f+ Constants.SMALL_PADDING * 2;
                    width += emptyW;
                    height = Math.max(height, Constants.BOOLEAN_EXPRESSION_HEIGHT);
                    measurement.socket.width = Constants.BOOLEAN_EXPRESSION_SIDE_WIDTH * 0.75f + Constants.BOOLEAN_EXPRESSION_HEIGHT * 0.75f;
                    measurement.socket.height = Constants.BOOLEAN_EXPRESSION_HEIGHT;
                } else {
                    float emptyW = Constants.SMALL_PADDING * 2 + Constants.EMPTY_EXPRESSION_INPUT_WIDTH;
                    width += emptyW;
                    height = Math.max(height, Constants.FIELD_ROUND_HEIGHT);
                    measurement.socket.width = Constants.EMPTY_EXPRESSION_INPUT_WIDTH;
                    measurement.socket.height = Constants.FIELD_ROUND_HEIGHT;
                }
            }
        } else if (input.type == InputType.STATEMENT) {
            float statementHeight = 0;
            Block projected = DragProjectionState.getProjectedBlock(input);
            if (projected != null) {
                Block current = projected;
                while (current != null) {
                    BlockMeasurement pm = measureBlock(current);
                    statementHeight += pm.height;
                    if (current instanceof Statement stmt && stmt.hasNext()) {
                        current = stmt.nextConnection.getTargetBlock();
                    } else break;
                }
            }
            // Statement inputs are measured separately
            if (input.connection != null && input.connection.isConnected()) {
                // Measure all connected statements in the chain
                Block current = input.connection.getTargetBlock();
                while (current != null) {
                    BlockMeasurement statementMeasurement = measureBlock(current);
                    statementHeight += statementMeasurement.height;
                    Block projectedNext = DragProjectionState.getProjectedNextBlock(current);
                    if (projectedNext != null) {
                        // Add the height of the projected shadow block (and any blocks attached to it)
                        Block pCurrent = projectedNext;
                        while (pCurrent != null) {
                            statementHeight += measureBlock(pCurrent).height;
                            if (pCurrent instanceof Statement stmt && stmt.hasNext()) {
                                pCurrent = stmt.nextConnection.getTargetBlock();
                            } else break;
                        }
                    }
                    if (current instanceof Statement stmt) {
                        if (stmt.hasNext()) {
                            current = stmt.nextConnection.getTargetBlock();
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            height = statementHeight > 0 ? statementHeight : Constants.EMPTY_STATEMENT_INPUT_HEIGHT;
            measurement.socket.width = Constants.STATEMENT_INPUT_SPACER_MIN_WIDTH;
            measurement.socket.height = height;
        }

        measurement.width = width;
        measurement.height = height;
        measurement.input = input;
        return measurement;
    }

    /**
     * Measure a field
     */
    FieldMeasurement measureField(Field<?> field) {
        FieldMeasurement m = new FieldMeasurement();
        String text = field.getText();

        float textWidth = textPaint.measureText(text);

        m.field = field;
        m.text = text;

        boolean isBooleanField =
                field.getSourceBlock() instanceof Expression
                        && ((Expression<?>) field.getSourceBlock()).getReturnType() == Boolean.class;

        if ((isBooleanField || isNonBooleanExpr(field.getSourceBlock()))) {
            //m.width = Math.max(textWidth+Constants.SMALL_PADDING, Constants.FIELD_ROUND_MIN_WIDTH);
            m.height = Constants.BOOLEAN_EXPRESSION_HEIGHT;
        } else {
            m.height = Constants.FIELD_BORDER_RECT_HEIGHT;
        }
        m.width = textWidth;

        if (field instanceof TextField || field instanceof NumberField) {
            m.width = Math.max(textWidth+Constants.SMALL_PADDING, Constants.FIELD_ROUND_MIN_WIDTH);
        }

        return m;
    }
    static void logLong(String tag, String s) {
        int max = 3000;
        for (int i = 0; i < s.length(); i += max) {
            Log.d(tag, s.substring(i, Math.min(s.length(), i + max)));
        }
    }
    public static String pathToSvg(Path path) {
        float[] pts = path.approximate(0.05f);
        StringBuilder sb = new StringBuilder();

        if (pts.length < 3) return "";

        sb.append("M ")
                .append(pts[1]).append(" ")
                .append(pts[2]);

        for (int i = 3; i < pts.length; i += 3) {
            sb.append(" L ")
                    .append(pts[i + 1]).append(" ")
                    .append(pts[i + 2]);
        }

        return sb.toString();
    }

    /**
     * Create the path for the block shape
     */
    private Path createBlockPath(Block block, BlockMeasurement measurement) {
        if (isBooleanExpr(block)) {
            return createBooleanExpressionPath(measurement);
        }

        if (isNonBooleanExpr(block)) {
            return createRoundExpressionPath(measurement);
        }

        Path path = new Path();

        float x = currentX;
        float y = currentY;

        // Start at top-left corner
        path.moveTo(x, y + Constants.CORNER_RADIUS);

        // Top-left corner
        path.arcTo(new RectF(x, y, x + Constants.CORNER_RADIUS * 2, y + Constants.CORNER_RADIUS * 2),
                180, 90, false);

        // Top edge with previous connection notch for statements
        // Top edge with previous connection notch
        if (block instanceof Statement && !(block instanceof HatBlock)) {
            path.lineTo(x + Constants.NOTCH_OFFSET_LEFT, y); // move to notch start
            drawNotch(path,x+Constants.NOTCH_OFFSET_LEFT,y,true);                           // append notch to path
            path.lineTo(x + blockWidth - Constants.CORNER_RADIUS, y); // continue top edge
        } else {
            path.lineTo(x + blockWidth - Constants.CORNER_RADIUS, y);
        }
        // Top-right corner
        path.arcTo(new RectF(x + blockWidth - Constants.CORNER_RADIUS * 2, y,
                        x + blockWidth, y + Constants.CORNER_RADIUS * 2),
                270, 90, false);

        // Right edge — for every statement-input row, cut a pocket into the right side.
        //
        // Geometry derived by tracing the Blockly SVG paths directly (r = CORNER_RADIUS):
        //
        //  • innerWall  = x + STATEMENT_INPUT_PADDING_LEFT          (= x + 4r)
        //  • notch RTL start (right side) = innerWall + NOTCH_OFFSET_LEFT + NOTCH_WIDTH
        //  • notch RTL end   (left side)  = innerWall + NOTCH_OFFSET_LEFT
        //  • 2r gap between notch end and inner-arc tangent point (both top and bottom)
        //  • inner top  arc: center=(innerWall+r, pocketTopY+r),   CCW, start=270°, sweep=−90°
        //  • inner bot  arc: center=(innerWall+r, pocketBottomY−r), CCW, start=180°, sweep=−90°
        //  • outer top-right pocket corner: CW, start=0°,   sweep=+90°
        //  • outer bot-right pocket corner: CW, start=270°, sweep=+90°
        //  • After each pocket the right-edge downstroke resumes at (x+blockWidth, pocketBottomY+r).
        final float r         = Constants.CORNER_RADIUS;
        final float innerWall = x + Constants.STATEMENT_INPUT_PADDING_LEFT; // x + 4r

        float rowStartY = y + Constants.TOP_ROW_MIN_HEIGHT;
        for (RowMeasurement row : measurement.rows) {
            if (row.isStatement) {
                final float pocketTopY    = rowStartY;
                final float pocketBottomY = rowStartY + row.height;

                // 1. Line down right edge to the inward-corner arc start (stop r above shelf).
                path.lineTo(x + blockWidth, pocketTopY - r);

                // 2. Outer top-right inward corner (CW: going DOWN → going LEFT).
                //    center = (x+blockWidth−r, pocketTopY−r)
                //    rect   = (x+blockWidth−2r, pocketTopY−2r, x+blockWidth, pocketTopY)
                //    start=0°  → (x+blockWidth, pocketTopY−r)  ✓ matches lineTo above
                //    end  =90° → (x+blockWidth−r, pocketTopY)
                path.arcTo(new RectF(
                        x + blockWidth - 2 * r, pocketTopY - 2 * r,
                        x + blockWidth,          pocketTopY), 0, 90);
                // now at (x+blockWidth−r, pocketTopY) going LEFT

                // 3. Line left along pocket shelf to RTL entrance-notch right side.
                path.lineTo(
                        innerWall + Constants.NOTCH_OFFSET_LEFT + Constants.NOTCH_WIDTH,
                        pocketTopY);

                // 4. RTL entrance notch (going left, isTop=false).
                //    Ends at (innerWall + NOTCH_OFFSET_LEFT, pocketTopY).
                drawNotch(path,
                        innerWall + Constants.NOTCH_OFFSET_LEFT + Constants.NOTCH_WIDTH,
                        pocketTopY, false);

                // 5. Line 2r more left to the inner-top-arc tangent point.
                //    innerWall + NOTCH_OFFSET_LEFT − 2r = innerWall + NOTCH_OFFSET_LEFT − 2r
                //    but simpler: innerWall + r  (= x+5r, the arc's start tangent).
                path.lineTo(innerWall + r, pocketTopY);

                // 6. Inner top-left concave corner (CCW in screen: turning LEFT → DOWN).
                //    center = (innerWall+r, pocketTopY+r)
                //    rect   = (innerWall, pocketTopY, innerWall+2r, pocketTopY+2r)
                //    start=270° → (innerWall+r, pocketTopY)  ✓ matches step 5
                //    CCW −90°  → 180° → (innerWall, pocketTopY+r)
                path.arcTo(new RectF(
                        innerWall, pocketTopY,
                        innerWall + 2 * r, pocketTopY + 2 * r), 270, -90);
                // now at (innerWall, pocketTopY+r) going DOWN

                // 7. Inner wall downstroke.
                path.lineTo(innerWall, pocketBottomY - r);

                // 8. Inner bottom-left concave corner (CCW in screen: turning DOWN → RIGHT).
                //    center = (innerWall+r, pocketBottomY−r)
                //    rect   = (innerWall, pocketBottomY−2r, innerWall+2r, pocketBottomY)
                //    start=180° → (innerWall, pocketBottomY−r)  ✓ matches step 7
                //    CCW −90°  → 90° → (innerWall+r, pocketBottomY)
                path.arcTo(new RectF(
                        innerWall, pocketBottomY - 2 * r,
                        innerWall + 2 * r, pocketBottomY), 180, -90);
                // now at (innerWall+r, pocketBottomY) going RIGHT

                // 9. Line 2r right to LTR exit-notch left side (= innerWall + NOTCH_OFFSET_LEFT).
                path.lineTo(innerWall + Constants.NOTCH_OFFSET_LEFT, pocketBottomY);

                // 10. LTR exit notch (going right, isTop=true).
                //     Ends at (innerWall + NOTCH_OFFSET_LEFT + NOTCH_WIDTH, pocketBottomY).
                drawNotch(path, innerWall + Constants.NOTCH_OFFSET_LEFT, pocketBottomY, true);

                // 11. Line right along bottom shelf to the outward-corner arc start.
                // 11. Line right along bottom shelf to the outer arc tangent
                float notchExit = innerWall + Constants.NOTCH_OFFSET_LEFT + Constants.NOTCH_WIDTH;
                float arcStart  = x + blockWidth - r;

                path.lineTo(arcStart, pocketBottomY);

                // 12. Outer bottom-right outward corner (CW: going RIGHT → going DOWN).
                //     center = (x+blockWidth−r, pocketBottomY+r)
                //     rect   = (x+blockWidth−2r, pocketBottomY, x+blockWidth, pocketBottomY+2r)
                //     start=270° → (x+blockWidth−r, pocketBottomY)  ✓ matches step 11
                //     CW  +90°  → 0° → (x+blockWidth, pocketBottomY+r)
                path.arcTo(new RectF(
                        x + blockWidth - 2 * r, pocketBottomY,
                        x + blockWidth,          pocketBottomY + 2 * r), 270, 90);
                // now at (x+blockWidth, pocketBottomY+r) going DOWN — right edge resumes
            }
            rowStartY += row.height;
        }
        // Straight down to the bottom-right corner start
        path.lineTo(x + blockWidth, y + blockHeight - Constants.CORNER_RADIUS);

        // Bottom-right corner
        path.arcTo(new RectF(x + blockWidth - Constants.CORNER_RADIUS * 2,
                        y + blockHeight - Constants.CORNER_RADIUS * 2,
                        x + blockWidth, y + blockHeight),
                0, 90, false);

        // Bottom edge with next connection notch for statements
        if (block instanceof Statement) {
            drawNotch(path, x + Constants.NOTCH_OFFSET_LEFT+Constants.NOTCH_WIDTH, y + blockHeight,false);
            path.lineTo(x + Constants.CORNER_RADIUS, y + blockHeight);
        } else {
            path.lineTo(x + Constants.CORNER_RADIUS, y + blockHeight);
        }
        // Bottom-left corner
        path.arcTo(new RectF(x, y + blockHeight - Constants.CORNER_RADIUS * 2,
                        x + Constants.CORNER_RADIUS * 2, y + blockHeight),
                90, 90, false);

        // Left edge — close() draws a straight line back to moveTo (x, y + CORNER_RADIUS)
        //as of now very slight off as it goes back ot 0,0 and not CORNER_RADIUS
        //this can only be seen thought an a tool live svg editor as it is to small to be seen for real
        //TODO fix this

        //path.lineTo(0,10);
        path.lineTo(x, y + Constants.CORNER_RADIUS);
        //path.close();

        return path;
    }

    private Path createBooleanExpressionPath(BlockMeasurement m) {
        float x = currentX;
        float y = currentY;

        float h = m.height;
        float s = h / 2f;
        float cw = m.width - 2 * s;

        Path p = new Path();
        p.moveTo(x + s, y);
        p.lineTo(x + s + cw, y);
        p.lineTo(x + s + cw + s, y + s);
        p.lineTo(x + s + cw, y + h);
        p.lineTo(x + s, y + h);
        p.lineTo(x, y + s);
        p.close();

        return p;
    }
    private void drawNotch(Path path, float x, float y, boolean isTop) {
        float w = Constants.NOTCH_WIDTH;
        float h = Constants.NOTCH_HEIGHT;

        // SVG path relative to its origin
        String svgPath;
        long t0 = 0;

        float sx = w / 36f, sy =  h / 8f;
        NotchRender.PathProgram notch = isTop? NotchRender.TOP_BASE_POINT_LIST: NotchRender.BOTTOM_BASE_POINT_LIST;
        for (int i = 0; i < notch.count; i++) {
            x += notch.dx[i] * sx;
            y += notch.dy[i] * sy;
            path.lineTo(x, y);
        }
    }
    /**
     * Render inputs and fields on the block
     * @param block The block to render
     * @param m The measurement of the block
     */
    @Deprecated
    private void renderInputsAndFieldsLegacy(Block block, BlockMeasurement m) {
        boolean isBooleanExpression = isBooleanExpr(block);
        boolean isRoundExpression = isNonBooleanExpr(block);

        float baseX =
                isBooleanExpression ? currentX:
                        isRoundExpression ? currentX:
                                currentX + Constants.SMALL_PADDING; // Add padding to standard blocks

        float y =
                (isBooleanExpression || isRoundExpression)
                        ? currentY
                        : currentY + Constants.TOP_ROW_MIN_HEIGHT;

        int rowIndex = 0;

        for (RowMeasurement row : m.rows) {

            float x = baseX;
            int inputIndex = 0;
            for (InputMeasurement inputM : row.inputs) {
                if(!inputM.fields.isEmpty() && isBooleanExpression && inputIndex==0){
                    x+= Constants.SMALL_PADDING;
                }
                // render fields using cached sizes
                renderFieldRow(inputM, x, y,row);

                for (FieldMeasurement fm : inputM.fields) {
                    x += fm.width;
                }

                x += Constants.BETWEEN_FIELDS_PADDING * Math.max(0, inputM.fields.size() - 1);

                // expression inputs (cached branch)
                if (inputM.type == InputType.EXPRESSION) {

                    Input input = inputM.input;

                    if (input.connection != null && input.connection.isConnected()) {
                        BlockMeasurement bm = measureBlock(input.connection.getTargetBlock());
                        x += bm.width + Constants.SMALL_PADDING;
                    } else {
                        if (input instanceof ExpressionInput exprInput &&
                                exprInput.getExpectedType() == Boolean.class) {
                            x += Constants.SMALL_PADDING;
                            drawEmptyBooleanInput(x, y+(row.height-Constants.BOOLEAN_EXPRESSION_HEIGHT*0.75f)/2);
                            x += Constants.BOOLEAN_EXPRESSION_SIDE_WIDTH * 2
                                    + Constants.SMALL_PADDING;

                        } else {
                            x+=Constants.SMALL_PADDING;
                            drawEmptyExpressionInput(x,y+(row.height-Constants.FIELD_ROUND_HEIGHT)/2);
                            x += Constants.EMPTY_EXPRESSION_INPUT_WIDTH + Constants.SMALL_PADDING;
                        }
                    }
                }
                inputIndex++;
            }

            y += row.height;
            rowIndex++;
        }
    }
    @Deprecated
    private void renderFieldRow(InputMeasurement im, float startX, float y,RowMeasurement row) {
        // Measure all fields first

        float spacing = Constants.BETWEEN_FIELDS_PADDING;

        float x = startX;

        for (int i = 0; i < im.fields.size(); i++) {
            FieldMeasurement m = im.fields.get(i);
            Field<?> field = m.field;

            renderField(field, x,y + (row.height - m.height) / 2f, m);

            x += m.width;
            if (i < im.fields.size()-1) {
                x += spacing;
            }
        }
    }
    private void renderInputsAndFields(Block block, BlockMeasurement m){
        for (RowMeasurement row : m.rows) {
            for (InputMeasurement inputM : row.inputs) {
                for (FieldMeasurement fm : inputM.fields) {
                    renderField(fm.field, currentX + fm.x, currentY + fm.y, fm);
                }
                if (inputM.type == InputType.EXPRESSION && inputM.socket != null) {
                    if (inputM.input.connection == null || !inputM.input.connection.isConnected()) {
                        if (inputM.input instanceof ExpressionInput exprInput && exprInput.getExpectedType() == Boolean.class) {
                            drawEmptyBooleanInput(currentX + inputM.socket.x, currentY + inputM.socket.y);
                        } else {
                            drawEmptyExpressionInput(currentX + inputM.socket.x, currentY + inputM.socket.y);
                        }
                    }
                }
            }
        }
    }

    private void renderField(Field<?> field, float x, float y, FieldMeasurement m) {
        Paint textP = (field instanceof TextField || field instanceof NumberField)
                ? texFieldTextPaint
                : textPaint;

        Paint.FontMetrics fm = textP.getFontMetrics();

        float centerY = y + m.height / 2f;
        float textY = centerY - (fm.ascent + fm.descent) / 2f;

        if (field instanceof LabelField) {
            float textX = x + m.width / 2f;

            textP.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(m.text, textX, textY, textP);
            return;
        }

        if (field instanceof TextField || field instanceof NumberField) {
            float fieldWidth = Math.max(Constants.FIELD_ROUND_MIN_WIDTH, m.width);

            float fieldY = y + (m.height - Constants.FIELD_ROUND_HEIGHT) / 2f;

            Path path = MakeRoundInputField(
                    x,
                    fieldY,
                    fieldWidth
            );

            canvas.drawPath(path, texFieldPaint);
            canvas.drawPath(path, textFieldStrokePaint);

            float textX = x + fieldWidth / 2f;

            textP.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(m.text, textX, textY, textP);

            m.textX = textX;
            m.textY = textY;
            touchRegions.put(path, m);
        }
    }
    private static boolean isBooleanExpr(Block b){
        return b instanceof Expression<?> expr && expr.getReturnType() == Boolean.class;
    }

    private static boolean isNonBooleanExpr(Block b){
        return b instanceof Expression<?> expr && expr.getReturnType() != Boolean.class;
    }
    private Path createRoundExpressionPath(BlockMeasurement m){
        float x = currentX;
        float y = currentY;

        float h = m.height;
        float r = h/2f;
        float w = m.width;

        Path p = new Path();

        p.moveTo(x+r, y);
        p.lineTo(x+w-r, y);

        RectF right = new RectF(x+w-2*r, y, x+w, y+h);
        p.arcTo(right, -90, 180);

        p.lineTo(x+r, y+h);

        RectF left = new RectF(x, y, x+2*r, y+h);
        p.arcTo(left, 90, 180);

        p.close();
        return p;
    }

    @NonNull
    private static Path MakeRoundInputField(float x, float y, float length) {
        float h = Constants.FIELD_ROUND_HEIGHT; // total height
        float r = h / 2f; // radius = half height to make capsule shape
        Path path = new Path();

        // Top-left
        path.moveTo(r, 0f);

        // Top horizontal
        path.lineTo(length - r, 0f);

        // Top-right corner
        RectF arcRight = new RectF(length - 2*r, 0f, length, h);
        path.arcTo(arcRight, -90f, 180f, false);

        // Bottom horizontal
        path.lineTo(r, h);

        // Bottom-left corner
        RectF arcLeft = new RectF(0f, 0f, 2*r, h);
        path.arcTo(arcLeft, 90f, 180f, false);

        path.close();

        // Move entire path to (x, y)
        path.offset(x, y);

        return path;
    }


    /**
     * Draw an empty expression input socket
     */
    private void drawEmptyExpressionInput(float x, float y) {
        canvas.drawPath(MakeRoundInputField(x, y, Constants.EMPTY_EXPRESSION_INPUT_WIDTH), socketPaint);
    }

    /**
     * Draw an empty boolean expression input socket (hexagon)
     */
    private void drawEmptyBooleanInput(float x, float y) {
        float h = Constants.BOOLEAN_EXPRESSION_HEIGHT*0.75f;
        float s = h / 2f;
        float cw = Constants.BOOLEAN_EXPRESSION_SIDE_WIDTH *  0.75f;
        Path p = new Path();
        p.moveTo(x + s, y);
        p.lineTo(x + s + cw, y);
        p.lineTo(x + s + cw + s, y + s);
        p.lineTo(x + s + cw, y + h);
        p.lineTo(x + s, y + h);
        p.lineTo(x, y + s);
        p.close();

        canvas.drawPath(p, socketPaint);
    }
    private static void computeRowTrims(
            RowMeasurement row,
            boolean isBooleanExpression,
            boolean isRoundExpression
    ) {
        row.leadingTrim = 0;
        row.trailingTrim = 0;

        if (row.inputs.isEmpty()) {
            return;
        }

        InputMeasurement first = row.inputs.get(0);
        InputMeasurement last = row.inputs.get(row.inputs.size() - 1);
        if (!first.fields.isEmpty() && (isBooleanExpression)) {
            row.leadingTrim = Constants.SMALL_PADDING;
        }

        // expression sockets currently add right padding after themselves
        if (last.type == InputType.EXPRESSION) {
            row.trailingTrim = Constants.SMALL_PADDING;
        }
    }

    // Measurement helper classes
    public static class BlockMeasurement {
        public float width;
        public float height;
        public NotchLayout nextNotch;
        public HashMap<Input<?>, SocketLayout> sockets = new HashMap<>();
        private ArrayList<RowMeasurement> rows = new ArrayList<>();
        public NotchLayout prevNotch = null;

        public ArrayList<RowMeasurement> getRows() {
            return rows;
        }
    }

    public static class RowMeasurement {
        public float width;
        public float height;
        public boolean isStatement = false;
        public float leadingTrim=0;
        public float trailingTrim=0;
        public ArrayList<InputMeasurement> inputs = new ArrayList<>();
    }

    public static class InputMeasurement {
        InputType type;
        float width;
        float height;
        Input<?> input;
        SocketLayout socket = new SocketLayout();
        ArrayList<FieldMeasurement> fields = new ArrayList<>();
    }
    public static class FieldMeasurement {
        public float x,y;
        public float textX;
        public float textY;
        public Field<?> field;
        public float width;
        public float height;
        public String text;
    }
    public static class NotchLayout {
        float x;
        float y;

        boolean isNextConnection;

    }
    public static class SocketLayout {
        float x;
        float y;
        float width;
        float height;

        Input<?> input;
        boolean isExpression;
    }

}