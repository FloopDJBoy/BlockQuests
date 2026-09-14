package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.R;

public class ActorCategoryBarAdaptor extends RecyclerView.Adapter<ActorCategoryBarAdaptor.ViewHolder> {
    private final ArrayList<String> categories = new ArrayList<>();
    {
        categories.add("All");
        categories.add("Recent");
        categories.add("Favorites");
    }
    private final ActorPaletteAdaptor actorPaletteAdaptor;

    public ActorCategoryBarAdaptor(ActorPaletteAdaptor actorPaletteAdaptor){
        this.actorPaletteAdaptor = actorPaletteAdaptor;
        actorPaletteAdaptor.setMode(ActorPaletteAdaptor.Mode.ALL);

    }
    @NonNull
    @Override
    public ActorCategoryBarAdaptor.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ActorCategoryBarAdaptor.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.category_bar_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ActorCategoryBarAdaptor.ViewHolder holder, int position) {
        holder.label.setText(categories.get(position));
        holder.circle.getBackground().setTint(Block.Category.values()[position%Block.Category.values().length].getColor());
        holder.itemView.setOnClickListener(v -> {
            switch (categories.get(position)){
                case "All":
                    actorPaletteAdaptor.setMode(ActorPaletteAdaptor.Mode.ALL);
                    break;
                case "Recent":
                    actorPaletteAdaptor.setMode(ActorPaletteAdaptor.Mode.RECENT);
                    break;
                case "Favorites":
                    actorPaletteAdaptor.setMode(ActorPaletteAdaptor.Mode.FAVORITES);
                    break;
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View circle;
        public TextView label;
        public LinearLayout layout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            circle = itemView.findViewById(R.id.colorBubble);
            layout = itemView.findViewById(R.id.categoryLayout);
            label = itemView.findViewById(R.id.categoryLabel);

        }
    }
}
