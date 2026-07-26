package com.aefyr.flexfilter.config.core;

import android.os.Parcelable;

public interface FilterConfig extends Parcelable {

    /**
     * Create a copy of this FilterConfig
     *
     * @return a copy of this FilterConfig
     */
    FilterConfig snapshot();

}
