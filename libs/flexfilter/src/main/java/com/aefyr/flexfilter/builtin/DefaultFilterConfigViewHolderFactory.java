package com.aefyr.flexfilter.builtin;

import com.aefyr.flexfilter.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.builtin.filter.singlechoice.ui.SingleChoiceFilterConfigViewHolder;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.builtin.filter.sort.ui.SortFilterConfigViewHolder;
import com.aefyr.flexfilter.config.core.FilterConfig;
import com.aefyr.flexfilter.ui.adapter.FilterConfigViewHolder;
import com.aefyr.flexfilter.ui.adapter.FilterConfigViewHolderFactory;

public class DefaultFilterConfigViewHolderFactory implements FilterConfigViewHolderFactory {

    protected final int VIEW_TYPE_SINGLE_CHOICE_FILTER = 0;
    protected final int VIEW_TYPE_SORT_FILTER = 1;

    private LayoutInflater mInflater;

    @Override
    public int getViewType(FilterConfig config) {
        if (config instanceof SingleChoiceFilterConfig)
            return VIEW_TYPE_SINGLE_CHOICE_FILTER;

        if (config instanceof SortFilterConfig)
            return VIEW_TYPE_SORT_FILTER;

        throw new IllegalArgumentException("Unsupported FilterConfig: " + config.getClass().getCanonicalName());
    }

    @Override
    public FilterConfigViewHolder createViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_SINGLE_CHOICE_FILTER:
                return new SingleChoiceFilterConfigViewHolder(getInflater(parent).inflate(R.layout.item_single_choice_filter, parent, false));
            case VIEW_TYPE_SORT_FILTER:
                return new SortFilterConfigViewHolder(getInflater(parent).inflate(R.layout.item_sort_filter, parent, false));
        }

        throw new IllegalArgumentException("Unsupported viewType: " + viewType);
    }

    protected LayoutInflater getInflater(View v) {
        if (mInflater == null)
            mInflater = LayoutInflater.from(v.getContext());

        return mInflater;
    }
}
