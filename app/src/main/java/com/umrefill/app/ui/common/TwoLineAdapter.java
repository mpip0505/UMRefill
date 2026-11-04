package com.umrefill.app.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.umrefill.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple RecyclerView adapter for "title + subtitle" rows, shared by the placeholder lists.
 * Owners can swap in their own adapter and row layout (e.g. with status icons) when they style a screen.
 */
public class TwoLineAdapter extends RecyclerView.Adapter<TwoLineAdapter.Holder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final List<String> titles = new ArrayList<>();
    private final List<String> subtitles = new ArrayList<>();
    private final OnItemClickListener listener;

    public TwoLineAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /** Replaces all rows. Both lists must be the same size. */
    public void submit(List<String> newTitles, List<String> newSubtitles) {
        titles.clear();
        subtitles.clear();
        titles.addAll(newTitles);
        subtitles.addAll(newSubtitles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_two_line, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.title.setText(titles.get(position));
        holder.subtitle.setText(subtitles.get(position));
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) listener.onItemClick(pos);
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtTitle);
            subtitle = itemView.findViewById(R.id.txtSubtitle);
        }
    }
}
