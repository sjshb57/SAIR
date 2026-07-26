package com.aefyr.flexfilter.ui.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.config.core.FilterConfig;

import java.util.HashMap;
import java.util.List;

public class ComplexFilterConfigAdapter extends RecyclerView.Adapter<FilterConfigViewHolder> {

    private FilterConfigViewHolderFactory mFactory;

    private List<FilterConfig> mFilters;

    /**
     * Objects shared between ViewHolders of the same type
     */
    private HashMap<String, Object> mSharedObjects;

    public ComplexFilterConfigAdapter(FilterConfigViewHolderFactory factory) {
        mFactory = factory;
        mSharedObjects = new HashMap<>();
    }

    public void setFilters(List<FilterConfig> filters) {
        mFilters = filters;
        notifyDataSetChanged();
    }

    public void recycle() {
        mSharedObjects.clear();
    }

    @Override
    public int getItemViewType(int position) {
        return mFactory.getViewType(mFilters.get(position));
    }

    @NonNull
    @Override
    public FilterConfigViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FilterConfigViewHolder holder = mFactory.createViewHolder(parent, viewType);
        holder.init(mSharedObjects);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull FilterConfigViewHolder holder, int position) {
        holder.bind(mFilters.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull FilterConfigViewHolder holder) {
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return mFilters == null ? 0 : mFilters.size();
    }
}
