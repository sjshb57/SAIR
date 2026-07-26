package com.aefyr.flexfilter.builtin.filter.sort.ui;

import com.aefyr.flexfilter.R;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.ui.adapter.FilterConfigViewHolder;

public class SortFilterConfigViewHolder extends FilterConfigViewHolder<SortFilterConfig> {

    private SortFilterConfigOptionAdapter mOptionsAdapter;
    private TextView mTitle;

    public SortFilterConfigViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    protected void onCreate(View itemView) {
        mTitle = itemView.findViewById(R.id.tv_sort_filter_title);

        mOptionsAdapter = new SortFilterConfigOptionAdapter(itemView.getContext());
        RecyclerView optionsRecycler = itemView.findViewById(R.id.rv_sort_filter_options);
        optionsRecycler.setRecycledViewPool(getViewPool());

        optionsRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        optionsRecycler.setAdapter(mOptionsAdapter);
        optionsRecycler.setItemAnimator(null);
    }

    @Override
    protected void onBind(SortFilterConfig config) {
        mTitle.setText(config.name());
        mOptionsAdapter.setFilter(config);
    }

    @Override
    protected void onUnbind() {
        mOptionsAdapter.setFilter(null);
    }

    private RecyclerView.RecycledViewPool getViewPool() {
        RecyclerView.RecycledViewPool pool = getSharedObject();
        if (pool == null) {
            pool = new RecyclerView.RecycledViewPool();
            pool.setMaxRecycledViews(0, 16);
            putSharedObject(pool);
        }

        return pool;
    }
}
