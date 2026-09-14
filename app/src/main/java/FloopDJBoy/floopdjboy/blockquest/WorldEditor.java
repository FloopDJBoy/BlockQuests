package FloopDJBoy.floopdjboy.blockquest;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.EditMode;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ActorLayerView;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.ActorPaletteAdaptor;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.StageAdapter;

public class WorldEditor {
    private EditMode currentEditMode = EditMode.NONE;
    /** Sprite resource ID currently selected for PLACE_ACTOR or PAINT_TILE.
     *  -1 means nothing is selected. */
    private int selectedSpriteResId = -1;
    private static final int DEFAULT_TILE_BG = R.drawable.grass_tile;
    private final GameExecutor gameExecutor;
    private boolean isPlayMode;
    private World world;
    private final StageAdapter adapter;
    private final ActorLayerView actorLayerView;
    private final ActorPaletteAdaptor actorPaletteAdaptor;
    // -----------------------------------------------------------------------
    // Tile click handling — the core of the feature
    // -----------------------------------------------------------------------

    public void setPlayMode(boolean playMode) {
        this.isPlayMode = playMode;
    }
    public EditMode getCurrentEditMode() {
        return currentEditMode;
    }
    public void setSpinner(Spinner modeSelector){
        ArrayAdapter<String> modeSelectorAdapter = new ArrayAdapter<>(
                modeSelector.getContext(),
                R.layout.mode_select_layout,
                new String[]{"None", "Place Actor", "Erase Actor", "Paint Tile", "Erase Tile"}
        );
        modeSelectorAdapter.setDropDownViewResource(R.layout.mode_item_layout);
        modeSelector.setAdapter(modeSelectorAdapter);
        modeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0 -> setEditMode(EditMode.NONE);
                    case 1 -> setEditMode(EditMode.PLACE_ACTOR);
                    case 2 -> setEditMode(EditMode.ERASE_ACTOR);
                    case 3 -> setEditMode(EditMode.PAINT_TILE);
                    case 4 -> setEditMode(EditMode.ERASE_TILE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    public WorldEditor(GameExecutor gameExecutor, World world, StageAdapter adapter, ActorLayerView actorLayerView, ActorPaletteAdaptor actorPaletteAdaptor, boolean isPlayMode,Spinner modeSelector){
        this.gameExecutor = gameExecutor;
        this.world = world;
        this.actorPaletteAdaptor = actorPaletteAdaptor;
        this.isPlayMode = isPlayMode;
        this.adapter = adapter;
        this.actorLayerView = actorLayerView;
        setSpinner(modeSelector);
    }
    public EditMode onTileClicked(int x, int y) {
        if (gameExecutor != null && gameExecutor.isRunning()) return EditMode.NONE ; // no editing while running
        if (isPlayMode) return EditMode.NONE;

        switch (currentEditMode) {
            case PLACE_ACTOR:
                placeActor(x, y);
                break;
            case ERASE_ACTOR:
                eraseTopActor(x, y);
                break;
            case PAINT_TILE:
                paintTile(x, y);
                break;
            case ERASE_TILE:
                eraseTile(x, y);
                break;
            case NONE:
            default:
                break;
        }
        return currentEditMode;
    }
    public void setWorld(World world) {
        this.world = world;
    }
    /** Place a new actor of the selected sprite at (x, y). */
    private void placeActor(int x, int y) {
        if (selectedSpriteResId == -1) return;

        Actor newActor = new Actor(x, y, world,
                Actor.AbsoluteDirection.DOWN, selectedSpriteResId);

        // Notify the actor layer so an ImageView is created for it
        actorLayerView.addActorView(newActor);
    }

    /**
     * Remove the topmost actor on tile (x, y).
     * If the resulting SpriteGroup is empty, the workspace blocks for that
     * group will be cleaned up via the SpriteGroupRemovedListener.
     */
    private void eraseTopActor(int x, int y) {
        ArrayList<Actor> actors = world.getActors(x, y);
        if (actors.isEmpty()) return;

        Actor top = actors.get(actors.size() - 1);
        actorLayerView.removeActorView(top);
        world.removeActor(top);
        // SpriteGroupRemovedListener (set in onCreate) handles workspace cleanup
    }

    /** Paint the background of tile (x, y) with the selected sprite. */
    private void paintTile(int x, int y) {
        if (selectedSpriteResId == -1) return;
        world.setTileBackgroundRes(x, y, selectedSpriteResId);
        // Only the changed tile needs rebinding
        int position = y * world.getWidth() + x;
        adapter.notifyItemChanged(position);
    }

    /** Reset tile (x, y) background to the default grass tile. */
    private void eraseTile(int x, int y) {
        world.setTileBackgroundRes(x, y, DEFAULT_TILE_BG);
        int position = y * world.getWidth() + x;
        adapter.notifyItemChanged(position);
    }

    public void setEditMode(EditMode mode) {
        if (currentEditMode == mode) return;
        currentEditMode = mode;

        if (mode != EditMode.PLACE_ACTOR && actorPaletteAdaptor != null) {
            actorPaletteAdaptor.clearSelection();
        }
        if (mode != EditMode.PLACE_ACTOR) selectedSpriteResId = -1;
    }

    public void setSelectedSpriteResId(int spriteResId) {
        selectedSpriteResId = spriteResId;
    }
}
