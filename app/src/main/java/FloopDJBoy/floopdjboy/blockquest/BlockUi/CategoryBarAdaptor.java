package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import FloopDJBoy.floopdjboy.blockquest.R;

public class CategoryBarAdaptor extends RecyclerView.Adapter<CategoryBarAdaptor.ViewHolder> {
    private final List<PaletteAdapter.PaletteCategory> categories = new ArrayList<>();
    private final PaletteAdapter paletteAdapter;
    public CategoryBarAdaptor(List<PaletteAdapter.PaletteCategory> allCategories, PaletteAdapter paletteAdapter){
        this.paletteAdapter=paletteAdapter;
        for (PaletteAdapter.PaletteCategory category : allCategories) {
            if(!category.blocks.isEmpty()){
                categories.add(category);
            }
        }
    }

    public List<PaletteAdapter.PaletteCategory> getCategories() {
        return categories;
    }

    @NonNull
    @Override
    public CategoryBarAdaptor.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
         return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.category_bar_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryBarAdaptor.ViewHolder holder, int position) {
        PaletteAdapter.PaletteCategory category = categories.get(position);
        holder.circle.getBackground().setTint(category.color);
        holder.label.setText(category.name);
        if (category.isSelected){
            holder.layout.setBackgroundColor(Color.parseColor("#1e1e1e"));
        }else{
            holder.layout.setBackgroundColor(Color.rgb(17, 17, 17));
        }
        holder.itemView.setOnClickListener(v -> {
            boolean isPreSelected = category.isSelected;
            for (PaletteAdapter.PaletteCategory c : categories) {

                c.isSelected = false;
            }
            category.isSelected = !isPreSelected;
            notifyDataSetChanged();

            paletteAdapter.selectCategory(category.isSelected? category:null);
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
