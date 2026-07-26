package com.aefyr.flexfilter.builtin.filter.sort;

import android.os.Parcel;
import android.text.TextUtils;

import com.aefyr.flexfilter.config.core.FilterConfig;
import com.aefyr.flexfilter.config.core.util.ParcelCompat;

import java.util.ArrayList;
import java.util.List;

public class SortFilterConfig implements FilterConfig {

    private String mId;
    private CharSequence mName;

    private List<SortFilterConfigOption> mOptions;

    public SortFilterConfig(String id, CharSequence name) {
        mId = id;
        mName = name;
        mOptions = new ArrayList<>();
    }

    protected SortFilterConfig(Parcel in) {
        mId = in.readString();
        mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        mOptions = ParcelCompat.readParcelableList(in, new ArrayList<>(), SortFilterConfigOption.class.getClassLoader());
        for (SortFilterConfigOption option : mOptions) {
            option.setFilter(this);
        }
    }

    public static final Creator<SortFilterConfig> CREATOR = new Creator<SortFilterConfig>() {
        @Override
        public SortFilterConfig createFromParcel(Parcel in) {
            return new SortFilterConfig(in);
        }

        @Override
        public SortFilterConfig[] newArray(int size) {
            return new SortFilterConfig[size];
        }
    };

    void setExclusiveOptionChecked(SortFilterConfigOption selectedOption) {
        for (SortFilterConfigOption option : mOptions) {
            option.setSelectedInternal(selectedOption == option);
        }
    }

    public String id() {
        return mId;
    }

    public CharSequence name() {
        return mName;
    }

    public List<SortFilterConfigOption> options() {
        return mOptions;
    }

    public SortFilterConfigOption getSelectedOption() {
        for (SortFilterConfigOption option : mOptions) {
            if (option.isSelected())
                return option;
        }

        throw new IllegalStateException("No option selected");
    }

    public SortFilterConfig addOption(String id, CharSequence name) {
        SortFilterConfigOption option = new SortFilterConfigOption(id, name);
        option.setFilter(this);
        mOptions.add(option);

        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        TextUtils.writeToParcel(mName, dest, flags);
        ParcelCompat.writeParcelableList(dest, mOptions, flags);
    }

    @Override
    public FilterConfig snapshot() {
        SortFilterConfig clone = new SortFilterConfig(mId, mName);
        for (SortFilterConfigOption option : mOptions) {
            clone.addOption(option.id(), option.name());

            SortFilterConfigOption cloneOption = clone.options().get(clone.options().size() - 1);
            if (option.isSelected())
                cloneOption.setSelectedInternal(true);

            cloneOption.setAscending(option.ascending());
        }

        return clone;
    }
}
