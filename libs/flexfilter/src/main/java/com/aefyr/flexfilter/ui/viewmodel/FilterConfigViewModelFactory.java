package com.aefyr.flexfilter.ui.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.flexfilter.config.core.ComplexFilterConfig;

public class FilterConfigViewModelFactory implements ViewModelProvider.Factory {

    private ComplexFilterConfig mConfig;

    public FilterConfigViewModelFactory(ComplexFilterConfig config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            return modelClass.getConstructor(ComplexFilterConfig.class).newInstance(mConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
