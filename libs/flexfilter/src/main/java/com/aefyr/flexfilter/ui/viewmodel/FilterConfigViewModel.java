package com.aefyr.flexfilter.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.config.core.FilterConfig;

import java.util.List;

public class FilterConfigViewModel extends ViewModel {

    private final ComplexFilterConfig mConfig;

    private final MutableLiveData<List<FilterConfig>> mFilters;

    public FilterConfigViewModel(ComplexFilterConfig config) {
        mConfig = config;

        mFilters = new MutableLiveData<>(mConfig.filters());
    }

    public LiveData<List<FilterConfig>> getFilters() {
        return mFilters;
    }

    public ComplexFilterConfig getConfig() {
        return mConfig;
    }

}
