package FloopDJBoy.floopdjboy.blockquest;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.SpriteGroup;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.WinBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.EditMode;
import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.DataTypes;
import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.Serializer;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ActorCategoryBarAdaptor;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ActorLayerView;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ActorPaletteAdaptor;
import FloopDJBoy.floopdjboy.blockquest.Profiler.BlockProfiler;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.BlockView;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.CategoryBarAdaptor;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.PaletteAdapter;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.StageAdapter;
import FloopDJBoy.floopdjboy.blockquest.MainMenuActivity.MainMenuActivity;
import FloopDJBoy.floopdjboy.blockquest.PlayLevelSelectorActivity.PlayLevelSelectorActivity;
import FloopDJBoy.floopdjboy.blockquest.Profiler.ChromeTraceExporter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Workspace workspace;
    private StageAdapter adapter;
    private RecyclerView stage;
    private World world;
    AlertDialog winDialog;
    private GameExecutor gameExecutor;
    private static final boolean DEBUG = false;
    static {
        BlockProfiler.setEnabled(DEBUG);
    }

    private ImageButton stopBtn, backBtn, runBtn, saveBtn, pauseBtn;
    private Button btnCode, playBtn, btnActors;

    private RecyclerView blockPalette, actorPalette, blockCategoryBar, actorCategoryBar;

    private ActorLayerView actorLayerView;
    private boolean isPublished = false;
    private String levelId;
    private String levelName;
    private WorldEditor worldEditor;
    private FirebaseFirestore db;
    private boolean isPlayMode = false;
    private ActorPaletteAdaptor actorPaletteAdaptor;
    private ChromeTraceExporter exporter;
    LinearLayout tagArea;
    private Spinner modeSelector;
    public void setWorld(World world, HashMap<SpriteGroup,ArrayList<Block>> roots, HashMap<String, DataTypes.BlockData> blocks){
        this.world = world;
        world.fillBackground(R.drawable.grass_tile);
        BlockView.clear();
        workspace.reset();
        adapter.setWorld(world);
        if(gameExecutor!=null){
            gameExecutor.setWorld(world);
        }
        adapter.notifyDataSetChanged();
        ((GridLayoutManager)stage.getLayoutManager()).setSpanCount(world.getWidth());
        actorLayerView.post(
                () -> {
                    actorLayerView.init(world, adapter.getTileSize(),workspace);
                    actorLayerView.startAnimating();
                    actorLayerView.bringToFront();
                    workspace.setActorLayerView(actorLayerView);
                }
        );
        for(Map.Entry<SpriteGroup,ArrayList<Block>> entry: roots.entrySet()){
            SpriteGroup group = entry.getKey();
            workspace.setActiveActor(group.getActor(0));
            for(Block root : entry.getValue()){
                BlockView bv = Serializer.makeBlockViewStack(root,group,blocks,workspace,this);
                bv.reOrderBlocks();
                bv.requestLayout();
                bv.invalidate();
            }
        }
        world.setSpriteGroupRemovedListener(workspace::deleteSpriteGroup);
        workspace.invalidate();
        workspace.requestLayout();
        workspace.setActiveActor(world.getPlayer());

    }

    public void setUpPallet(){
        List<PaletteAdapter.PaletteCategory> categories = new ArrayList<>();
        blockPalette = findViewById(R.id.blockPalette);
        PaletteAdapter paletteAdapter = new PaletteAdapter(categories,workspace);
        blockPalette.setAdapter(paletteAdapter);
        blockPalette.setLayoutManager(new LinearLayoutManager(this));

        CategoryBarAdaptor categoryBarAdaptor = new CategoryBarAdaptor(categories,paletteAdapter);
        blockCategoryBar = findViewById(R.id.categoryBar);
        blockCategoryBar.setAdapter(categoryBarAdaptor);
        blockCategoryBar.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
    }
    private void setUpActorPalette() {
        actorCategoryBar = findViewById(R.id.categoryBarActors);
        actorPalette   = findViewById(R.id.actorPalette);

        actorPaletteAdaptor = new ActorPaletteAdaptor(actorLayerView);

        // Provide the available sprites.
        // Add every sprite resource ID you want to be placeable here.
        ArrayList<Integer> sprites = new ArrayList<>();
        java.lang.reflect.Field[] fields = R.drawable.class.getFields();

        for (java.lang.reflect.Field field : fields) {
            try {
                int resId = field.getInt(null);
                String name = field.getName();

                if (name.startsWith("actor_") && resId != 0){
                    sprites.add(resId);

                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        actorPaletteAdaptor.setActors(sprites);
        actorPaletteAdaptor.setOnActorSelectedListener(spriteResId -> {
            worldEditor.setSelectedSpriteResId(spriteResId);
            if (spriteResId != -1) {
                // Selecting an actor automatically activates PLACE_ACTOR mode.
                worldEditor.setEditMode(EditMode.PLACE_ACTOR);
                modeSelector.setSelection(1);
            } else {
                worldEditor.setEditMode(EditMode.NONE);
                modeSelector.setSelection(0);
                actorPaletteAdaptor.clearSelection();
            }
        });

        actorPalette.setAdapter(actorPaletteAdaptor);
        ActorCategoryBarAdaptor actorCategoryBarAdaptor = new ActorCategoryBarAdaptor(actorPaletteAdaptor);
        actorCategoryBar.setAdapter(actorCategoryBarAdaptor);
        actorCategoryBar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        actorPalette.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

    }
    public void setupTag(){
        tagArea = findViewById(R.id.underStageArea);

        for(Actor.Tag tag : Actor.Tag.values()) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(tag.toString().toLowerCase());
            checkBox.setTextColor(Color.WHITE);
            checkBox.setTag(tag.ordinal());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    workspace.getActiveGroup().addTag(Actor.Tag.values()[(int) buttonView.getTag()]);
                }else{
                    workspace.getActiveGroup().removeTag(Actor.Tag.values()[(int) buttonView.getTag()]);
                }
            });

            tagArea.addView(checkBox);
        }
    }
    /**
     * Switch to the given edit mode.
     * Clears actor palette selection when leaving PLACE_ACTOR.
     */
    private void setupWindow() {
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
    private void bindViews() {
        db = FirebaseFirestore.getInstance();
        playBtn = findViewById(R.id.playBtn);
        modeSelector = findViewById(R.id.mode_selector);
        btnActors = findViewById(R.id.btn_actors_category);
        btnCode = findViewById(R.id.btn_code_category);
        stopBtn = findViewById(R.id.stopBtn);
        backBtn = findViewById(R.id.backBtn);
        pauseBtn = findViewById(R.id.pauseBtn);
        saveBtn = findViewById(R.id.save_btn);
        runBtn = findViewById(R.id.runBtn);
        stage = findViewById(R.id.Stage);
    }
    private void setupStaticUi() {
        db = FirebaseFirestore.getInstance();
        setupTag();
        playBtn.setOnClickListener(v -> setPlayMode(!isPlayMode));


        winDialog = new AlertDialog.Builder(this)
                .setTitle("You win")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .create();

        btnActors.setOnClickListener(this);
        btnCode.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        runBtn.setOnClickListener(this);

        backBtn.setOnClickListener(v -> {
            if (!isPublished) {
                LevelRepository.getInstance().saveLevel(this, levelId, world);
            }
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        btnActors.post(() -> changeBtnSate(btnActors, false));
        btnCode.post(() -> changeBtnSate(btnCode, true));
    }
    private void initWorkspaceAndAdapters() {
        workspace = new Workspace(this);


        adapter = new StageAdapter(world, workspace);

        stage.setAdapter(adapter);
        stage.setClipToPadding(false);
        stage.setClipChildren(false);
        //span count here is a temp
        stage.setLayoutManager(new GridLayoutManager(this,1));
        stage.setNestedScrollingEnabled(false);
        stage.bringToFront();
        setUpPallet();
        RegisterAllBlock.loadBlocks(this);
        if(RegisterAllBlock.isReady()){
            setUpData();
        }else{
            RegisterAllBlock.onReady(()->runOnUiThread(this::setUpData));
        }

    }
    private void setUpData(){
        List<PaletteAdapter.PaletteCategory> categories = RegisterAllBlock.getCached();
        ((CategoryBarAdaptor)(blockCategoryBar.getAdapter())).getCategories().clear();
        ((CategoryBarAdaptor)(blockCategoryBar.getAdapter())).getCategories().addAll(
                categories.stream()
                        .filter(category -> !category.blocks.isEmpty())
                        .collect(java.util.stream.Collectors.toList())
        );
        ((PaletteAdapter)blockPalette.getAdapter()).getCategories().clear();
        ((PaletteAdapter)blockPalette.getAdapter()).getCategories().addAll(
                categories.stream()
                        .filter(category -> !category.blocks.isEmpty())
                        .collect(java.util.stream.Collectors.toList())
        );
        blockCategoryBar.getAdapter().notifyDataSetChanged();
        blockPalette.getAdapter().notifyDataSetChanged();

    }
    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG) {
            try {
                exporter = ChromeTraceExporter.start(this);
                exporter.startAutoFlush(2000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(DEBUG){
            if (exporter != null) {
                try {
                    exporter.close(); // final flush + file finalize
                } catch (Exception ignored) {}
                exporter = null;
            }
        }
    }
    private void initDynamicViews() {
        ConstraintLayout mainContainer = findViewById(R.id.main);
        ConstraintLayout.LayoutParams workspaceLp = new ConstraintLayout.LayoutParams(0, 0);
        workspaceLp.startToEnd = R.id.paletteHolder;
        workspaceLp.endToStart = stage.getId();
        workspaceLp.topToBottom = R.id.topBar;
        workspaceLp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        mainContainer.addView(workspace, workspaceLp);
        actorLayerView = new ActorLayerView(this);

        int widthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 360, getResources().getDisplayMetrics()
        );
        int heightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 270, getResources().getDisplayMetrics()
        );

        ConstraintLayout.LayoutParams actorLayerLp = new ConstraintLayout.LayoutParams(widthPx, heightPx);
        actorLayerLp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        actorLayerLp.topToTop = R.id.paletteHolder;
        mainContainer.addView(actorLayerView, actorLayerLp);

        actorLayerView.post(() -> {
            actorLayerView.init(world, adapter.getTileSize(), workspace);
            actorLayerView.startAnimating();
            actorLayerView.bringToFront();
            workspace.setActorLayerView(actorLayerView);
        });
    }
    private void initGameExecutor() {
        gameExecutor = new GameExecutor(world);
        gameExecutor.setListener(new GameExecutor.GameListener() {
            private Block previousBlock;
            @Override
            public void onLoss() {}
            @Override
            public void onGameStarted() {
                runBtn.setBackgroundColor(Color.parseColor("#00c3ff59"));
                stopBtn.setAlpha(1f);
            }
            @Override
            public void onGameStopped() {
                world.reset();
                workspace.refreshVarRow();
                for (int i = 0; i < workspace.getChildCount(); i++) {
                    View v = workspace.getChildAt(i);
                    if (v instanceof BlockView bv) {
                        bv.setHighlighted(false);
                    }
                }
                previousBlock = null;
                adapter.notifyDataSetChanged();
                stopBtn.setAlpha(0.5f);
                runBtn.setBackground(null);
            }
            @Override
            public void onGamePaused() {}
            @Override
            public void onGameResumed() {}
            @Override
            public void onTick(int tickCount) {
                runOnUiThread(()->{
                    workspace.refreshVarRow();
                    adapter.notifyDataSetChanged();
                    actorLayerView.updateTileStacking();
                });
            }
            @Override
            public void onBlockExecuted(Block block, int tick) {
                if (previousBlock != null) {
                    BlockView.getBlockViewByBlock(previousBlock).setHighlighted(false);
                }

                if (block instanceof WinBlock) {
                    runOnUiThread(() ->
                            workspace.postDelayed(() -> handleLevelComplete(tick), 50)
                    );
                }

                previousBlock = block;

                if (block != null) {
                    BlockView.getBlockViewByBlock(block).setHighlighted(true);
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        bindViews();
        setupStaticUi();
        initWorkspaceAndAdapters();
        initDynamicViews();
        String levelJson = getIntent().getStringExtra("levelJson");
        if(levelJson!=null){
            //assume the user is not trying to spoof or do anything malices
            Serializer.Deserialize(levelJson,this);
        }
        initGameExecutor();
        setUpFromIntent();
        actorCategoryBar = findViewById(R.id.categoryBarActors);
        actorPalette   = findViewById(R.id.actorPalette);
        actorPaletteAdaptor = new ActorPaletteAdaptor(actorLayerView);
        worldEditor= new WorldEditor(gameExecutor,world,adapter,actorLayerView,actorPaletteAdaptor,isPlayMode,modeSelector);
        if(isPublished){
            setPlayMode(true);
            playBtn.setVisibility(View.GONE);
        }
        adapter.setOnTileClickListener(worldEditor::onTileClicked);
        actorLayerView.setOnTileClickListener(worldEditor::onTileClicked);
        setUpActorPalette();

    }
    private void handleLevelComplete(int tick) {

        if (!isPublished && isPlayMode) {
            showPublishDialog(tick);
            return;
        }
        if(isPublished){
            showResultsDialog(tick);
        }else{
            winDialog.show();
        }
    }
    private void showResultsDialog(int tick) {

        String uid = FirebaseAuth.getInstance().getUid();
        LevelRepository repo = LevelRepository.getInstance();

        repo.getLevel(levelId).addOnSuccessListener(level -> {

            long best = level.getLong("bestMoves") != null
                    ? level.getLong("bestMoves")
                    : Long.MAX_VALUE;

            boolean newRecord = tick < best;

            View view = getLayoutInflater()
                    .inflate(R.layout.dialog_level_complete, null);

            repo.getReaction(levelId, uid)
                    .addOnSuccessListener(reactionDoc -> {

                        LevelRepository.Reaction reaction = LevelRepository.Reaction.fromString(reactionDoc.exists()
                                ? reactionDoc.getString("reaction")
                                : null);
                        LevelCompleteDialog.show(
                                MainActivity.this,
                                view,
                                tick,
                                best,
                                newRecord,
                                reaction,
                                new LevelCompleteDialog.Listener() {

                                    @Override
                                    public void onLike() {
                                        repo.sendReaction(levelId, uid, LevelRepository.Reaction.LIKE);

                                    }

                                    @Override
                                    public void onDislike() {
                                        repo.sendReaction(levelId, uid,  LevelRepository.Reaction.DISLIKE);
                                    }

                                    @Override
                                    public void onNext() {
                                        Intent intent = new Intent(
                                                MainActivity.this,
                                                PlayLevelSelectorActivity.class
                                        );
                                        intent.setFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        );
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                        );
                    }).addOnFailureListener(e->{
                        Toast.makeText(this, "an unknown error occurred", Toast.LENGTH_SHORT).show();
                    });
        });
    }
    private void showPublishDialog(int tick) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("You win")
                .setMessage("Would you like to publish your level?")
                .setPositiveButton("YES", (dialog, which) -> {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    LevelRepository.getInstance().publishLevel(levelId,gson.toJson(prePlayData), tick);
                })
                .setNegativeButton("NO", null)
                .setCancelable(true)
                .show();
    }
    void setUpFromIntent(){
        Intent intent = getIntent();
        if(intent==null) return;
        levelId = intent.getStringExtra("levelId");
        levelName = intent.getStringExtra("levelName");
        isPublished = intent.getBooleanExtra("isPublished",false);
        String levelJson = intent.getStringExtra("levelJson");
        db.collection("levels").document(levelId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        exitWithError();
                        return;
                    }

                    String serverJson = snapshot.getString("levelJson");
                    isPublished = Boolean.TRUE.equals(snapshot.getBoolean("isPublished"));
                    if(isPublished){
                        setPlayMode(true);
                    }

                    if (!serverJson.equals(levelJson)) {
                        Serializer.Deserialize(serverJson, this); // correct it
                    }
                })
                .addOnFailureListener(e -> {
                    exitWithError();
                });
    }

    private void exitWithError() {
        Toast.makeText(this, "an unknown error occurred", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    int dpToPx(float dp) {
        return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
    }
    private void changeBtnSate(Button btn, boolean on) {
        if (btn != btnActors && btn != btnCode) return;

        btn.setBackgroundResource(on ? R.drawable.tab_selector_on_layout : R.drawable.tab_selector_layout);

        btn.setTextColor(Color.parseColor(on ? "#00c3ff" : "#EEEEEE"));

        Drawable vector = AppCompatResources.getDrawable(btn.getContext(), btn==btnActors? R.drawable.btn_actors : R.drawable.btn_code);
        vector = DrawableCompat.wrap(vector.mutate());
        DrawableCompat.setTint(vector, Color.parseColor(on ? "#00c3ff" : "#EEEEEE"));
        Drawable[] drawables = btn.getCompoundDrawables();
        btn.setCompoundDrawablesWithIntrinsicBounds(vector, drawables[1], drawables[2], drawables[3]);


        ViewGroup.LayoutParams params = btn.getLayoutParams();
        params.height = dpToPx(on ? 35 : 30);
        btn.setLayoutParams(params);

    }
    /**
     * Enter or exit play mode.
     * In play mode:
     *   - tile/actor placement is blocked
     *   - the actor palette is hidden
     *   - block editing is blocked for non-player groups (enforced in Workspace/BlockView)
     *   - browsing other actors' blocks is still allowed
     */
    private DataTypes.WorldData prePlayData = null;
    public void setPlayMode(boolean enabled) {
        isPlayMode = enabled;
        worldEditor.setPlayMode(enabled);
        workspace.setPlayMode(enabled);
        world.reset();
        gameExecutor.stop();

        if (enabled) {
            // Hide actor palette and its category bar — not usable in play mode
            actorPalette.setVisibility(View.GONE);
            actorCategoryBar.setVisibility(View.GONE);
            // Reset tile edit mode so no accidental tile edits occur
            worldEditor.setEditMode(EditMode.NONE);
            prePlayData = Serializer.SerializeRaw(world);
            modeSelector.setEnabled(false);
            modeSelector.setSelection(0);
            btnActors.setEnabled(false);
            btnActors.setAlpha(0.6f);
            for(int i = 0; i < tagArea.getChildCount(); ++i){
                View v = tagArea.getChildAt(i);
                if(v instanceof CheckBox) {
                    v.setEnabled(false);
                }
            }
            playBtn.setText("Exit Play Mode");
            changeBtnSate(btnActors,false);
            changeBtnSate(btnCode,true);
            saveBtn.setVisibility(View.GONE);
        } else {
            modeSelector.setEnabled(true);
            btnActors.setEnabled(true);
            saveBtn.setVisibility(View.VISIBLE);
            btnActors.setAlpha(1f);
            // Restore palette visibility only if the actors tab is currently active.
            // The tab visibility is managed by btnActors/btnCode; we just re-apply it.
            boolean actorsTabVisible = actorCategoryBar.getTag() != null && (boolean) actorCategoryBar.getTag();
            if (actorsTabVisible) {
                actorPalette.setVisibility(View.VISIBLE);
                actorCategoryBar.setVisibility(View.VISIBLE);
            }
            for(int i = 0; i < tagArea.getChildCount(); ++i){
                View v = tagArea.getChildAt(i);
                if(v instanceof CheckBox) {
                    v.setEnabled(true);
                }
            }
            playBtn.setText("Enter Play Mode");
            if(prePlayData!=null){
                world = Serializer.DeserializeRaw(prePlayData,this);
                worldEditor.setWorld(world);
            }
        }
    }
    @Override
    public void onClick(View v) {
        if(v==runBtn){
            gameExecutor.start();
        }
        else if(v==stopBtn){
            gameExecutor.stop();
        }
        else if(v==pauseBtn){
            if(gameExecutor.isPaused()){
                gameExecutor.resume();
                pauseBtn.setImageResource(R.drawable.btn_pause);
            }
            else{
                gameExecutor.pause();
                pauseBtn.setImageResource(R.drawable.btn_unpause);
            }
        } else if (v==saveBtn) {
            LevelRepository.getInstance().saveLevel(this,levelId,world);
        }else if (v == btnActors || v == btnCode) {

            boolean actorsOn = (v == btnActors);

            changeBtnSate(btnActors, actorsOn);
            changeBtnSate(btnCode, !actorsOn);
            float top = dpToPx(4);
            float bottom = 0;

            btnActors.setElevation(actorsOn ? top : bottom);
            btnCode.setElevation(actorsOn ? bottom : top);

            Runnable updateY = () -> {
                View parentActors = (View) btnActors.getParent();
                btnActors.setY(parentActors.getHeight() - btnActors.getHeight());

                View parentCode = (View) btnCode.getParent();
                btnCode.setY(parentCode.getHeight() - btnCode.getHeight());
            };
            btnActors.post(updateY);
            actorCategoryBar.setVisibility(actorsOn ? View.VISIBLE : View.GONE);
            actorPalette.setVisibility(actorsOn ? View.VISIBLE : View.GONE);
            blockCategoryBar.setVisibility(actorsOn ? View.GONE : View.VISIBLE);
            blockPalette.setVisibility(actorsOn ? View.GONE : View.VISIBLE);
        }
    }
}
