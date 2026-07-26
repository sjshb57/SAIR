package com.aefyr.flexfilter.builtin.filter.singlechoice.ui;

import com.aefyr.flexfilter.R;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.ui.adapter.FilterConfigViewHolder;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

public class SingleChoiceFilterConfigViewHolder extends FilterConfigViewHolder<SingleChoiceFilterConfig> {

    private SingleChoiceFilterConfigOptionAdapter mOptionsAdapter;
    private TextView mTitle;

    public SingleChoiceFilterConfigViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    protected void onCreate(View itemView) {
        mTitle = itemView.findViewById(R.id.tv_simple_filter_title);

        RecyclerView optionsRecycler = itemView.findViewById(R.id.rv_simple_filter_options);
        optionsRecycler.setRecycledViewPool(getViewPool());

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(itemView.getContext(), FlexDirection.ROW, FlexWrap.WRAP);
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);
        optionsRecycler.setLayoutManager(layoutManager);

        mOptionsAdapter = new SingleChoiceFilterConfigOptionAdapter(itemView.getContext());
        optionsRecycler.setAdapter(mOptionsAdapter);
    }

    @Override
    protected void onBind(SingleChoiceFilterConfig config) {
        mTitle.setText(config.name());
        mOptionsAdapter.setFilter(config);
    }

    @Override
    protected void onUnbind() {
        mOptionsAdapter.setFilter(null);
    }

    private RecyclerView.RecycledViewPool getViewPool() {
        RecyclerView.RecycledViewPool pool = getSharedObject("pool");
        if (pool == null) {
            pool = new RecyclerView.RecycledViewPool();
            pool.setMaxRecycledViews(0, 16);
            putSharedObject("pool", pool);
        }

        return pool;
    }
}
