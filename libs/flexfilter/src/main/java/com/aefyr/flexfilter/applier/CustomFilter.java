package com.aefyr.flexfilter.applier;

import java.util.List;

public interface CustomFilter<T> {

    /**
     * Simply filter out unwanted objects from a list
     *
     * @param t object to filter
     * @return true if object should be filtered out
     */
    default boolean filterSimple(T t) {
        return false;
    }

    /**
     * For complex filters that need to access/modify the whole list such as sort filters
     *
     * @param list list to filter
     * @return filtered modifiable list
     */
    default List<T> filterComplex(List<T> list) {
        return list;
    }

}
