package com.aefyr.flexfilter.builtin.filter.sort.ui;

import com.aefyr.flexfilter.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfigOption;
import com.google.android.material.chip.Chip;

public class SortFilterConfigOptionAdapter extends RecyclerView.Adapter<SortFilterConfigOptionAdapter.OptionViewHolder> {

    private final LayoutInflater mInflater;

    private SortFilterConfig mFilter;

    public SortFilterConfigOptionAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    public void setFilter(SortFilterConfig filter) {
        mFilter = filter;
        notifyDataSetChanged();
    }

    private SortFilterConfig getFilter() {
        return mFilter;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SortFilterConfigOptionAdapter.OptionViewHolder(mInflater.inflate(R.layout.item_sort_filter_option, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        holder.bind(this, mFilter.options().get(position));
    }

    @Override
    public void onViewRecycled(@NonNull OptionViewHolder holder) {
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return mFilter != null ? mFilter.options().size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return mFilter.options().get(position).id().hashCode();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {

        private final Chip mChip;

        private SortFilterConfigOptionAdapter mAdapter;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);

            mChip = (Chip) itemView;

            mChip.setOnClickListener((v) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || mAdapter == null)
                    return;

                SortFilterConfigOption option = mAdapter.getFilter().options().get(adapterPosition);

                if (option.isSelected())
                    option.setAscending(!option.ascending());
                else
                    option.setSelected();

                mAdapter.notifyDataSetChanged();
            });
        }

        void bind(SortFilterConfigOptionAdapter adapter, SortFilterConfigOption option) {
            mAdapter = adapter;

            mChip.setText(option.name());
            mChip.setChecked(option.isSelected());

            if (option.isSelected())
                mChip.setChipIconResource(option.ascending() ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
            else
                mChip.setChipIcon(null);
        }

        void unbind() {
            mAdapter = null;
        }
    }
}
