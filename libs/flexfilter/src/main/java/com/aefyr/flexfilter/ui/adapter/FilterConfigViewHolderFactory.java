package com.aefyr.flexfilter.ui.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.aefyr.flexfilter.config.core.FilterConfig;

/**
 * Classes subclassing this must have a zero arguments constructor
 */
@SuppressWarnings("ALL")
public interface FilterConfigViewHolderFactory {

    int getViewType(FilterConfig config);

    @SuppressWarnings("rawtypes")
    FilterConfigViewHolder createViewHolder(@NonNull ViewGroup parent, int viewType);

}
