package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.ArrayList;

import FloopDJBoy.floopdjboy.blockquest.R;

public class ActorPaletteAdaptor extends RecyclerView.Adapter<ActorPaletteAdaptor.ViewHolder> {

    public interface OnActorSelectedListener {
        /** Called when the user taps a sprite in the palette.
         *  @param spriteResId the selected drawable resource id, or -1 if deselected */
        void onActorSelected(int spriteResId);
    }

    private ArrayList<Integer> actors = new ArrayList<>();
    private final ActorLayerView actorLayerView;

    private int selectedPosition = Math.toIntExact(RecyclerView.NO_ID);
    private OnActorSelectedListener listener;
    private final ArrayList<Integer> favorites = new ArrayList<>();
    private Mode mode = null;
    private ArrayList<Integer> active = new ArrayList<>();
    enum Mode{
        ALL,
        RECENT,
        FAVORITES
    }
    private static final int MAX_RECENT_SIZE = 5;
    private final ArrayDeque<Integer> recent = new ArrayDeque<>();

    public ActorPaletteAdaptor(ActorLayerView actorLayerView) {
        this.actorLayerView = actorLayerView;

    }

    public void setOnActorSelectedListener(OnActorSelectedListener l) {
        this.listener = l;
    }

    public void setActors(ArrayList<Integer> actors) {
        this.actors = actors;
        selectedPosition = Math.toIntExact(RecyclerView.NO_ID);
        notifyDataSetChanged();
    }

    public void setMode(Mode mode){
        if(this.mode == mode) return;
        this.mode = mode;
        switch (mode){
            case ALL:
                active = actors;
                break;
            case RECENT:
                active = new ArrayList<>(recent);
                break;
            case FAVORITES:
                active = favorites;
                break;

        }
        clearSelection();
        notifyDataSetChanged();
    }

    /** Clears the current selection (e.g. when switching modes). */
    public void clearSelection() {
        int prev = selectedPosition;
        selectedPosition = Math.toIntExact(RecyclerView.NO_POSITION);
        if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
    }

    /** Returns the currently selected sprite resource id, or -1 if none selected. */
    public int getSelectedSpriteResId() {
        if (selectedPosition == RecyclerView.NO_ID || selectedPosition >= active.size()) return -1;
        return active.get(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.actor_perview_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int resId = active.get(position);

        // Sprite preview — use resId overload so no Actor instance is needed
        android.graphics.Bitmap bmp = actorLayerView.getPreviewFrame(resId);
        if (bmp != null) {
            holder.actor_img.setImageBitmap(bmp);
        } else {
            holder.actor_img.setImageResource(resId);
        }

        // Highlight selected item
        boolean isSelected = (position == selectedPosition);
        holder.itemView.setBackgroundColor(
                isSelected ? Color.parseColor("#3399FF") : Color.TRANSPARENT);
        holder.favorite_btn.setImageResource(favorites.contains(resId) ? R.drawable.baseline_star_24 : R.drawable.star_svg);
        holder.favorite_btn.setTag(favorites.contains(resId));

        // Favourite button
        holder.favorite_btn.setOnClickListener(v -> {
            boolean current = (boolean) holder.favorite_btn.getTag();
            holder.favorite_btn.setImageResource(
                    current ? R.drawable.star_svg : R.drawable.baseline_star_24);
            holder.favorite_btn.setTag(!current);
            if(!current){
                favorites.add(resId);
            }else{
                favorites.remove((Integer)resId);
            }

        });

        // Tap the item to select/deselect
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            if (selectedPosition == position) {
                // Tap again → deselect
                selectedPosition = Math.toIntExact(RecyclerView.NO_ID);
                notifyItemChanged(position);
                if (listener != null) listener.onActorSelected(-1);
            } else {
                if(mode != Mode.RECENT){
                    selectedPosition = position;
                    if (recent.size() == MAX_RECENT_SIZE) {
                        recent.removeFirst();
                    }
                    recent.remove(resId);
                    recent.addLast(resId);
                }
                notifyItemChanged(position);
                if (prev != RecyclerView.NO_ID) notifyItemChanged(prev);
                if (listener != null) listener.onActorSelected(resId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return active.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton favorite_btn;
        ImageView actor_img;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            favorite_btn = itemView.findViewById(R.id.favorite_btn);
            actor_img    = itemView.findViewById(R.id.actor_img);
        }
    }
}