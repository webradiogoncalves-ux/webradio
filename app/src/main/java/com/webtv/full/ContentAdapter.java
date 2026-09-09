package com.webtv.full;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.Holder> {
    public interface OnClick { void click(Content content); }
    private final List<Content> data = new ArrayList<>();
    private final OnClick click;

    public ContentAdapter(OnClick click) { this.click = click; }
    public void setData(List<Content> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Content c = data.get(position);
        h.name.setText(c.name);
        h.type.setText(c.type);
        h.itemView.setOnClickListener(v -> click.click(c));
    }
    @Override public int getItemCount() { return data.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name, type;
        Holder(View view) {
            super(view);
            name = view.findViewById(R.id.name);
            type = view.findViewById(R.id.type);
        }
    }
}
