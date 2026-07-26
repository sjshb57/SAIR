package com.aefyr.flexfilter.builtin.filter.singlechoice;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.aefyr.flexfilter.config.core.FilterConfig;
import com.aefyr.flexfilter.config.core.util.ParcelCompat;

import java.util.ArrayList;
import java.util.List;

//TODO cleanup this and other builtin filters
public class SingleChoiceFilterConfig implements FilterConfig {
    private final String mId;
    private final CharSequence mName;

    private final List<SingleChoiceFilterConfigOption> mOptions;

    public SingleChoiceFilterConfig(String id, CharSequence name) {
        mId = id;
        mName = name;
        mOptions = new ArrayList<>();
    }

    void setExclusiveOptionChecked(SingleChoiceFilterConfigOption selectedOption) {
        for (SingleChoiceFilterConfigOption option : mOptions) {
            option.setSelectedInternal(selectedOption == option);
        }
    }

    public String id() {
        return mId;
    }

    public CharSequence name() {
        return mName;
    }

    public List<SingleChoiceFilterConfigOption> options() {
        return mOptions;
    }

    public SingleChoiceFilterConfig addOption(String id, CharSequence name) {
        SingleChoiceFilterConfigOption option = new SingleChoiceFilterConfigOption(id, name);
        option.setFilter(this);
        mOptions.add(option);
        return this;
    }

    public SingleChoiceFilterConfigOption getSelectedOption() {
        for (SingleChoiceFilterConfigOption option : mOptions) {
            if (option.isSelected())
                return option;
        }

        throw new IllegalStateException("No selected option");
    }

    protected SingleChoiceFilterConfig(Parcel in) {
        mId = in.readString();
        mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        mOptions = ParcelCompat.readParcelableList(in, new ArrayList<>(), SingleChoiceFilterConfigOption.class.getClassLoader());
        for (SingleChoiceFilterConfigOption option : mOptions) {
            option.setFilter(this);
        }
    }

    public static final Parcelable.Creator<SingleChoiceFilterConfig> CREATOR = new Parcelable.Creator<>() {
        @Override
        public SingleChoiceFilterConfig createFromParcel(Parcel in) {
            return new SingleChoiceFilterConfig(in);
        }

        @Override
        public SingleChoiceFilterConfig[] newArray(int size) {
            return new SingleChoiceFilterConfig[size];
        }
    };

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
        SingleChoiceFilterConfig clone = new SingleChoiceFilterConfig(mId, mName);
        for (SingleChoiceFilterConfigOption option : mOptions) {
            clone.addOption(option.id(), option.name());
            if (option.isSelected())
                clone.options().getLast().setSelectedInternal(true);
        }
        return clone;
    }
}
