package com.aefyr.flexfilter.applier;

import java.util.ArrayList;
import java.util.List;

public class FilterApplier<T> {

    public List<T> apply(ComplexCustomFilter<T> filter, List<T> list) {
        List<T> filteredList = new ArrayList<>();

        List<CustomFilter<T>> customFilters = filter.flatMap();

        for (T t : list) {
            boolean filteredOut = false;

            for (CustomFilter<T> customFilter : customFilters) {
                if (customFilter.filterSimple(t)) {
                    filteredOut = true;
                    break;
                }
            }

            if (!filteredOut) {
                filteredList.add(t);
            }
        }

        for (CustomFilter<T> customFilter : customFilters) {
            filteredList = customFilter.filterComplex(filteredList);
        }

        return filteredList;
    }

}
