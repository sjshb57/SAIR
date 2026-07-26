package com.aefyr.flexfilter.config.core;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.aefyr.flexfilter.config.core.util.ParcelCompat;

import java.util.ArrayList;
import java.util.List;

public class ComplexFilterConfig implements Parcelable {

    private List<FilterConfig> mFilterConfigs;

    public ComplexFilterConfig(List<FilterConfig> filterConfigs) {
        mFilterConfigs = filterConfigs;
    }

    public List<FilterConfig> filters() {
        return mFilterConfigs;
    }

    public ComplexFilterConfig snapshot() {
        long start = System.currentTimeMillis();

        ArrayList<FilterConfig> clonedFilters = new ArrayList<>();
        for (FilterConfig config : mFilterConfigs)
            clonedFilters.add(config.snapshot());

        Log.d("ComplexFilterConfig", String.format("Snapshot in %d ms.", (System.currentTimeMillis() - start)));

        return new ComplexFilterConfig(clonedFilters);
    }

    protected ComplexFilterConfig(Parcel in) {
        //TODO do something about classloader
        mFilterConfigs = ParcelCompat.readParcelableList(in, new ArrayList<>(), FilterConfig.class.getClassLoader());
    }

    public static final Creator<ComplexFilterConfig> CREATOR = new Creator<ComplexFilterConfig>() {
        @Override
        public ComplexFilterConfig createFromParcel(Parcel in) {
            return new ComplexFilterConfig(in);
        }

        @Override
        public ComplexFilterConfig[] newArray(int size) {
            return new ComplexFilterConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelCompat.writeParcelableList(dest, mFilterConfigs, flags);
    }
}
