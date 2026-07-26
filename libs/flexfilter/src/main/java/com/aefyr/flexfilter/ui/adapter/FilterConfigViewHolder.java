package com.aefyr.flexfilter.ui.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.flexfilter.config.core.FilterConfig;

import java.util.HashMap;

public abstract class FilterConfigViewHolder<F extends FilterConfig> extends RecyclerView.ViewHolder {

    private HashMap<String, Object> mSharedObjects;

    private FilterConfig mConfig;

    public FilterConfigViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    protected final FilterConfig getFilterConfig() {
        return mConfig;
    }

    void init(HashMap<String, Object> sharedObjects) {
        mSharedObjects = sharedObjects;
        onCreate(itemView);
    }

    protected abstract void onCreate(View itemView);

    //TODO call this somewhere
    void destroy() {
        onDestroy();
        mSharedObjects = null;
    }

    protected void onDestroy() {

    }

    void bind(F config) {
        mConfig = config;
        onBind(config);
    }

    protected abstract void onBind(F config);

    void unbind() {
        onUnbind();
        mConfig = null;
    }

    protected void onUnbind() {

    }

    @Nullable
    protected final <T> T getSharedObject(String id) {
        if (mSharedObjects == null)
            return null;

        return (T) mSharedObjects.get(getNamespacedSharedObjectId(id));
    }

    protected final void putSharedObject(String id, Object object) {
        if (mSharedObjects != null)
            mSharedObjects.put(getNamespacedSharedObjectId(id), object);
    }

    private String getNamespacedSharedObjectId(String id) {
        return getClass().getCanonicalName() + ":" + id;
    }

}
