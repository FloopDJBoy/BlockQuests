package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.EditMode;
import FloopDJBoy.floopdjboy.blockquest.R;
import FloopDJBoy.floopdjboy.blockquest.Workspace;
import FloopDJBoy.floopdjboy.blockquest.World;

public class StageAdapter extends RecyclerView.Adapter<StageAdapter.ViewHolder> {

    public interface OnTileClickListener {
        /** Called when a tile is tapped in the stage grid.
         *  @param x tile column (0-based)
         *  @param y tile row    (0-based) */
        EditMode onTileClick(int x, int y);
    }

    private World world;
    private int tileSize;
    private OnTileClickListener tileClickListener;

    public StageAdapter(World world, Workspace workspace) {
        this.world = world;
    }

    public void setOnTileClickListener(OnTileClickListener l) {
        this.tileClickListener = l;
    }

    public int getTileSize() {
        return tileSize;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.world_tile_layout, parent, false);
        tileSize = Math.min(parent.getWidth() / world.getWidth(),
                parent.getHeight() / world.getHeight());
        view.setLayoutParams(new RecyclerView.LayoutParams(tileSize, tileSize));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int x = position % world.getWidth();
        int y = position / world.getWidth();
        holder.tileBackground.setBackgroundResource(world.getTileBackgroundRes(x, y));

        holder.itemView.setOnClickListener(v -> {
            if (tileClickListener != null) {
                tileClickListener.onTileClick(x, y);
            }
        });
    }

    @Override
    public int getItemCount() {
        return world.getWidth() * world.getHeight();
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View tileBackground;

        ViewHolder(View itemView) {
            super(itemView);
            tileBackground = itemView.findViewById(R.id.tileBackground);
        }
    }
}