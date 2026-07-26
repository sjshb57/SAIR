package com.aefyr.flexfilter.applier;

import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.config.core.FilterConfig;

import java.util.ArrayList;
import java.util.List;

public class ComplexCustomFilter<T> {

    private final ComplexFilterConfig mFilterConfig;
    private final CustomFilterFactory<T> mFilterFactory;

    private final List<CustomFilter<T>> mAdditionalFilters;

    private ComplexCustomFilter(ComplexFilterConfig config, CustomFilterFactory<T> factory, List<CustomFilter<T>> additionalFilters) {
        mFilterConfig = config;
        mFilterFactory = factory;
        mAdditionalFilters = additionalFilters;
    }

    public List<CustomFilter<T>> flatMap() {
        ArrayList<CustomFilter<T>> customFilters = new ArrayList<>();

        if (mFilterConfig != null) {
            for (FilterConfig filterConfig : mFilterConfig.filters()) {
                customFilters.add(mFilterFactory.createCustomFilter(filterConfig));
            }
        }

        customFilters.addAll(mAdditionalFilters);

        return customFilters;
    }

    public static class Builder<T> {

        private ComplexFilterConfig mFilterConfig;
        private CustomFilterFactory<T> mFilterFactory;

        private final List<CustomFilter<T>> mAdditionalFilters = new ArrayList<>();

        public Builder<T> with(ComplexFilterConfig config, CustomFilterFactory<T> factory) {
            mFilterConfig = config;
            mFilterFactory = factory;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder<T> withSnapshot(ComplexFilterConfig config, CustomFilterFactory<T> factory) {
            mFilterConfig = config.snapshot();
            mFilterFactory = factory;
            return this;
        }

        public Builder<T> add(CustomFilter<T> filter) {
            mAdditionalFilters.add(filter);
            return this;
        }

        public ComplexCustomFilter<T> build() {
            return new ComplexCustomFilter<>(mFilterConfig, mFilterFactory, mAdditionalFilters);
        }

    }

}