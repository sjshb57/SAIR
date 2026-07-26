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
    protected final <T> T getSharedObject() {
        if (mSharedObjects == null)
            return null;

        return (T) mSharedObjects.get(getNamespacedSharedObjectId("pool"));
    }

    protected final void putSharedObject(Object object) {
        if (mSharedObjects != null)
            mSharedObjects.put(getNamespacedSharedObjectId("pool"), object);
    }

    private String getNamespacedSharedObjectId(String id) {
        return getClass().getCanonicalName() + ":" + id;
    }

}
