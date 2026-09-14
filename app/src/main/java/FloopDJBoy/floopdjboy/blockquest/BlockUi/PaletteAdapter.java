package FloopDJBoy.floopdjboy.blockquest.BlockUi;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import FloopDJBoy.floopdjboy.blockquest.Actor;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableDefinition;
import FloopDJBoy.floopdjboy.blockquest.R;
import FloopDJBoy.floopdjboy.blockquest.Workspace;

public class PaletteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Workspace workspace;

    private final List<PaletteCategory> categories;

    private final List<PaletteItem> flat = new ArrayList<>();

    private static final int TYPE_BLOCK = 0;
    private static final int TYPE_ADD_VARIABLE = 1;
    private Dialog newVariableDialog;

    public PaletteAdapter(List<PaletteCategory> cats, Workspace ws) {
        this.categories = cats;
        this.workspace = ws;
        this.newVariableDialog = setUpNewVariableDialog(ws.getContext());
        rebuild();
    }
    private boolean hasAddButton() {
        return categories.stream().anyMatch(c -> c.isSelected && c.category == Block.Category.VARIABLES);
    }
    public Dialog setUpNewVariableDialog(Context context){
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_variable, null);
        EditText nameInput = dialogView.findViewById(R.id.variableNameInput);
        RadioButton localRadio = dialogView.findViewById(R.id.localRadio);
        RadioButton globalRadio = dialogView.findViewById(R.id.globalRadio);
        Spinner typeSpinner = dialogView.findViewById(R.id.typeSpinner);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        Button createBtn = dialogView.findViewById(R.id.createBtn);
        localRadio.setChecked(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                R.layout.mode_item_layout,
                new String[]{"Integer", "String"}
        );
        adapter.setDropDownViewResource(R.layout.mode_item_layout);
        typeSpinner.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        createBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Required");
                return;
            }
            boolean global = globalRadio.isChecked();
            Class<?> type = switch ((String) typeSpinner.getSelectedItem()) {
                case "Integer" -> Integer.class;
                case "String" -> String.class;
                default -> Object.class;
            };
            VariableDefinition def = new VariableDefinition(
                    UUID.randomUUID().toString(),
                    name,
                    type
            );
            workspace.getActiveGroup().addVariable(global,def);
            rebuildVariablePalette();
            workspace.refreshVarRow();
            rebuild();
            dialog.dismiss();
        });
        return dialog;

    }
    public List<PaletteCategory> getCategories() {
        return categories;
    }

    /** Flatten all blocks for RecyclerView */
    public void rebuild() {
        flat.clear();
        for (PaletteCategory cat : categories) {
            if (cat.isSelected) {
                for (Supplier<Block> f : cat.blocks) {
                    flat.add(new PaletteItem(cat, f));
                }
            }
        }
        notifyDataSetChanged();
    }
    public void rebuildVariablePalette(){
        ArrayList<VariableDefinition> vars = workspace.getActiveGroup().getAllActiveVariables();
        PaletteCategory cat = categories.stream().filter(p->p.category==Block.Category.VARIABLES).findFirst().orElseThrow();
        cat.blocks.removeIf(p->p.get() instanceof VariableBlock);
        cat.blocks.addAll(
                vars.stream()
                        .<Supplier<Block>>map(var -> () -> {
                            VariableBlock v = new VariableBlock();
                            v.configure(var);
                            return v;
                        })
                        .collect(Collectors.toList())
        );
        if(categories.stream().anyMatch(c -> c.isSelected && c.category == Block.Category.VARIABLES)){
            rebuild();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (hasAddButton() && position == flat.size()) {
            return TYPE_ADD_VARIABLE;
        }
        return TYPE_BLOCK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == TYPE_BLOCK){
            BlockPreview v = new BlockPreview(parent.getContext());
            return new BlockHolder(v);
        }else if(viewType == TYPE_ADD_VARIABLE){
            Button btn = new Button(parent.getContext());
            btn.setText("Add Variable");
            btn.setAllCaps(false);
            btn.setTextColor(0xFFFFFFFF);
            btn.setTextSize(10f);
            btn.setHeight(dpToPx(10,parent.getContext()));
            btn.setPadding(0,0,0,0);
            btn.setBackgroundResource(R.drawable.btn_play_level);
            btn.setOnClickListener(v -> newVariableDialog.show());
            return new AddVariableHolder(btn);
        }else{
            throw new IllegalStateException("Unknown view type: " + viewType);
        }
    }
    private int dpToPx(float dp,Context ctx) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                ctx.getResources().getDisplayMetrics()
        );
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof AddVariableHolder) return;
        PaletteItem item = flat.get(position);
        BlockHolder h = (BlockHolder) holder;

        Block temp = item.factory.get();
        h.preview.setBlock(temp, item.category.color);
        h.preview.setOnTouchListener(new View.OnTouchListener() {
            BlockView draggedBlock = null;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                View overlay = v.getRootView().findViewById(R.id.dragOverlay);
                int[] overlayPos = new int[2];
                overlay.getLocationOnScreen(overlayPos);
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        //tell the recycler view to not intercept touch events
                        // so they could be correctly handled in the BlockView
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        if(workspace.isPlayMode() && !workspace.getActiveGroup().hasTag(Actor.Tag.PLAYER)){
                            return false;
                        }
                        // Spawn new block
                        draggedBlock = workspace.spawnBlockFromPalette(item.factory, 0, 0);
                        float touchOffsetX = e.getX() * workspace.getScaleFactor();
                        float touchOffsetY = e.getY() * workspace.getScaleFactor();

                        // Move to overlay
                        if (overlay instanceof ViewGroup vg) {
                            workspace.removeView(draggedBlock);
                            vg.addView(draggedBlock);
                        }

                        // Position is overlay-local: subtract overlayPos from raw screen coords.
                        draggedBlock.setX(e.getRawX() - overlayPos[0] - touchOffsetX);
                        draggedBlock.setY(e.getRawY() - overlayPos[1] - touchOffsetY);

                        // Dispatch synthetic ACTION_DOWN with raw screen coords so that
                        // BlockView.onTouchEvent computes initialTouchX/Y correctly via
                        // event.getRawX() - getLocationOnScreen().
                        MotionEvent downEvent = MotionEvent.obtain(
                                e.getDownTime(),
                                e.getEventTime(),
                                MotionEvent.ACTION_DOWN,
                                e.getRawX(),
                                e.getRawY(),
                                e.getMetaState()
                        );
                        draggedBlock.dispatchTouchEvent(downEvent);
                        downEvent.recycle();

                        return true;
                    // Forward all subsequent events with raw screen coords so BlockView
                    // can subtract its own overlayPos — consistent with ACTION_DOWN above.
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (draggedBlock != null) {
                            MotionEvent translated = MotionEvent.obtain(
                                    e.getDownTime(),
                                    e.getEventTime(),
                                    e.getActionMasked(),
                                    e.getRawX(),
                                    e.getRawY(),
                                    e.getMetaState()
                            );
                            draggedBlock.dispatchTouchEvent(translated);
                            translated.recycle();

                            // Clear reference on drag end
                            if (e.getActionMasked() == MotionEvent.ACTION_UP ||
                                    e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                                draggedBlock = null;
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return flat.size() + (hasAddButton() ? 1 : 0);
    }

    /** Call this when a category is clicked in the CategoryBar */
    public void selectCategory(PaletteCategory category) {
        for (PaletteCategory c : categories) {
            c.isSelected = c == category;
        }
        rebuild();
    }

    static class BlockHolder extends RecyclerView.ViewHolder {
        BlockPreview preview;

        public BlockHolder(BlockPreview v) {
            super(v);
            preview = v;
        }
    }
    static class AddVariableHolder extends RecyclerView.ViewHolder {
        public AddVariableHolder(Button v) {
            super(v);
        }
    }

    /** Category object for palette */
    public static class PaletteCategory {
        public final List<Supplier<Block>> blocks = new ArrayList<>();
        public final String name;
        public final Block.Category category;
        public final int color;
        public boolean isSelected = false;

        public PaletteCategory(Block.Category category) {
            this.category = category;
            this.name = category.toString();
            this.color = category.getColor();
        }
    }

    public static class PaletteItem {
        public final PaletteCategory category;
        public final Supplier<Block> factory;

        public PaletteItem(PaletteCategory c, Supplier<Block> f) {
            category = c;
            factory = f;
        }
    }
}