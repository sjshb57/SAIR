package com.aefyr.flexfilter.builtin.filter.singlechoice.ui;

import com.aefyr.flexfilter.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfigOption;
import com.google.android.material.chip.Chip;

public class SingleChoiceFilterConfigOptionAdapter extends RecyclerView.Adapter<SingleChoiceFilterConfigOptionAdapter.OptionViewHolder> {

    private final LayoutInflater mInflater;

    private SingleChoiceFilterConfig mFilter;

    public SingleChoiceFilterConfigOptionAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFilter(SingleChoiceFilterConfig filter) {
        mFilter = filter;
        notifyDataSetChanged();
    }

    private SingleChoiceFilterConfig getFilter() {
        return mFilter;
    }

    /** @noinspection ClassEscapesDefinedScope*/
    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OptionViewHolder(mInflater.inflate(R.layout.item_single_choice_filter_option, parent, false));
    }

    /** @noinspection ClassEscapesDefinedScope*/
    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        holder.bind(this, mFilter.options().get(position));
    }

    /** @noinspection ClassEscapesDefinedScope*/
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

        private SingleChoiceFilterConfigOptionAdapter mAdapter;

        @SuppressLint("NotifyDataSetChanged")
        OptionViewHolder(@NonNull View itemView) {
            super(itemView);

            mChip = (Chip) itemView;

            mChip.setOnClickListener((v) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || mAdapter == null)
                    return;

                mAdapter.getFilter().options().get(adapterPosition).setSelected();
                mAdapter.notifyDataSetChanged();
            });
        }

        void bind(SingleChoiceFilterConfigOptionAdapter adapter, SingleChoiceFilterConfigOption option) {
            mAdapter = adapter;
            mChip.setText(option.name());
            mChip.setChecked(option.isSelected());
        }

        void unbind() {
            mAdapter = null;
        }
    }
}
