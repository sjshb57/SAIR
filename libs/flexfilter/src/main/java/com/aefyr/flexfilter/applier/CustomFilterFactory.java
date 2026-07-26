package com.aefyr.flexfilter.applier;

import com.aefyr.flexfilter.config.core.FilterConfig;

public interface CustomFilterFactory<T> {

    CustomFilter<T> createCustomFilter(FilterConfig filterConfig);

}
