package FloopDJBoy.floopdjboy.blockquest.LevelStoreActivity;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.LevelPreviewRenderer;
import FloopDJBoy.floopdjboy.blockquest.R;

public class LevelSaveAdaptor extends RecyclerView.Adapter<LevelSaveAdaptor.ViewHolder> {
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final Context ctx;
    public ArrayList<LevelPreview> data = new ArrayList<>();
    private final LevelActionListener listener;
    interface LevelCallback {
        void onLoaded(ArrayList<LevelPreview> data);
    }
    public interface LevelActionListener {
        void onDelete(LevelPreview item, int position);
        void onChooseLevel(LevelPreview item, int position);
    }
    public LevelSaveAdaptor(FirebaseFirestore db, FirebaseAuth mAuth, Context ctx,
                            LevelActionListener listener) {
        this.db = db;
        this.ctx = ctx;
        this.mAuth = mAuth;
        this.listener = listener;

        makeData(data1 -> {
            data.clear();
            data.addAll(data1);
            notifyDataSetChanged();
        });
    }
    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                ctx.getResources().getDisplayMetrics()
        );
    }
    private void makeData(LevelCallback callback) {
        ArrayList<LevelPreview> data = new ArrayList<>();

        db.collection("levels")
                .whereEqualTo("authorId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("isPublished", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        QueryDocumentSnapshot q =
                                (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(i);
                        String json = q.getString("levelJson");
                        LevelPreview lv = new LevelPreview(q.getId(),q.getString("name"),null,json);
                        data.add(lv);
                        final int position = i;
                        LevelPreviewRenderer.render(
                                ctx.getResources(),
                                json,
                                dpToPx(138),
                                bitmap -> {
                                    lv.img = bitmap;
                                    lv.isLoading = false;
                                    notifyItemChanged(position);
                                }
                        );
                    }
                    callback.onLoaded(data);
                });
    }
    @NonNull
    @Override
    public LevelSaveAdaptor.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.level_store_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LevelPreview item = data.get(position);

        holder.name.setText(item.name);
        holder.name.setSingleLine();

        if (item.isLoading) {
            holder.image.setBackgroundColor(Color.parseColor("#3C3C3C"));
            holder.image.setImageBitmap(null);
            holder.delete.setEnabled(false);
            holder.rename.setEnabled(false);
        } else {
            holder.image.setImageBitmap(item.img);
            holder.delete.setEnabled(true);
            holder.rename.setEnabled(true);
            holder.image.post(()->{
                //Log.d("IMG", "view width px = " + holder.image.getWidth());
                //Log.d("IMG", "bitmap width px = " + item.img.getWidth());
            });
        }

        holder.delete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item, holder.getBindingAdapterPosition());
            }
        });
        holder.rename.setOnClickListener(v -> {
            holder.name.setFocusable(true);
            holder.name.setFocusableInTouchMode(true);
            holder.name.setCursorVisible(true);

            holder.name.requestFocus();

            holder.name.post(() -> {
                InputMethodManager imm =
                        (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.showSoftInput(holder.name, InputMethodManager.SHOW_FORCED);

            });
        });
        holder.name.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String newName = holder.name.getText().toString().trim();
                InputMethodManager imm =
                        (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                // lock back editing
                holder.name.setFocusable(false);
                holder.name.setFocusableInTouchMode(false);
                holder.name.setCursorVisible(false);

                // prevent empty names
                if (newName.isEmpty()) {
                    holder.name.setText(item.name);
                    return;
                }

                if (!newName.equals(item.name)) {
                    item.name = newName;
                    updateLevelNameInDb(item);
                }
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener!=null){
                listener.onChooseLevel(item, holder.getBindingAdapterPosition());
            }
        });
    }

    private void updateLevelNameInDb(LevelPreview item) {
        db.collection("levels").document(item.id)
                .update("name", item.name);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextInputEditText name;
        ImageButton delete, rename;
        ImageView image;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.level_name);
            delete = itemView.findViewById(R.id.delete_button);
            rename = itemView.findViewById(R.id.rename_button);
            image = itemView.findViewById(R.id.imageView2);
        }
    }
    public static class LevelPreview {
        String id;
        String name;
        String levelJson;
        Bitmap img;
        boolean isLoading = true;

        public LevelPreview(String id, String name, Bitmap img, String levelJson) {
            this.id = id;
            this.name = name;
            this.img = img;
            this.levelJson = levelJson;
        }
    }
}

