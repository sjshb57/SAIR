package com.aefyr.flexfilter.builtin;

import com.aefyr.flexfilter.applier.CustomFilter;
import com.aefyr.flexfilter.applier.CustomFilterFactory;
import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.config.core.FilterConfig;

public interface DefaultCustomFilterFactory<T> extends CustomFilterFactory<T> {

    @Override
    default CustomFilter<T> createCustomFilter(FilterConfig config) {

        if (config instanceof SingleChoiceFilterConfig)
            return createCustomSingleChoiceFilter((SingleChoiceFilterConfig) config);

        if (config instanceof SortFilterConfig)
            return createCustomSortFilter((SortFilterConfig) config);

        throw new IllegalArgumentException("Unsupported FilterConfig class: " + config.getClass().getCanonicalName());
    }

    default CustomFilter<T> createCustomSingleChoiceFilter(SingleChoiceFilterConfig config) {
        return null;
    }

    default CustomFilter<T> createCustomSortFilter(SortFilterConfig config) {
        return null;
    }
}
