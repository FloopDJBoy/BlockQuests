package FloopDJBoy.floopdjboy.blockquest.BlockUi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableDefinition;
import FloopDJBoy.floopdjboy.blockquest.R;

public class VariableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<VarRow> rows;

    public VariableAdapter(ArrayList<VarRow> rows) {
        this.rows = rows;
    }
    public void setRows(ArrayList<VarRow> rows) {
        this.rows.clear();
        this.rows.addAll(rows);
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VarRow.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_var_header, parent, false);
            return new HeaderHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_var_row, parent, false);
            return new ItemHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VarRow row = rows.get(position);

        if (holder instanceof HeaderHolder h) {
            h.title.setText(row.title);
        } else if (holder instanceof ItemHolder h) {
            VariableDefinition v = row.var;

            Object value = row.value;
            String typeName = v.type.getSimpleName();

            h.text.setText(v.name + ": " + typeName + " = " + value);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView title;
        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.headerText);
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        TextView text;
        ItemHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.varText);
        }
    }
    public static class VarRow {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_ITEM = 1;

        public final int type;
        public final String title; // for headers
        public final VariableDefinition var; // for items
        public final Object value;

        private VarRow(int type, String title, VariableDefinition var,Object value) {
            this.type = type;
            this.title = title;
            this.var = var;
            this.value = value;
        }

        public static VarRow header(String title) {
            return new VarRow(TYPE_HEADER, title, null,null);
        }

        public static VarRow item(VariableDefinition var,Object value) {
            return new VarRow(TYPE_ITEM, null, var,value);
        }
        public static VarRow item(VariableDefinition var) {
            return new VarRow(TYPE_ITEM, null, var,var.getDefaultValue());
        }
    }
}
