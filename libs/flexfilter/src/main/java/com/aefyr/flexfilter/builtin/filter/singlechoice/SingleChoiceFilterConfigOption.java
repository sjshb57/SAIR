package com.aefyr.flexfilter.builtin.filter.singlechoice;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

public class SingleChoiceFilterConfigOption implements Parcelable {

    private String mId;
    private CharSequence mName;
    private boolean mSelected;

    private SingleChoiceFilterConfig mFilter;

    SingleChoiceFilterConfigOption(String id, CharSequence name) {
        mId = id;
        mName = name;
    }

    void setFilter(SingleChoiceFilterConfig filter) {
        mFilter = filter;
    }

    public String id() {
        return mId;
    }

    public CharSequence name() {
        return mName;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected() {
        mFilter.setExclusiveOptionChecked(this);
    }

    void setSelectedInternal(boolean selected) {
        mSelected = selected;
    }

    protected SingleChoiceFilterConfigOption(Parcel in) {
        mId = in.readString();
        mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
    }

    public static final Creator<SingleChoiceFilterConfigOption> CREATOR = new Creator<SingleChoiceFilterConfigOption>() {
        @Override
        public SingleChoiceFilterConfigOption createFromParcel(Parcel in) {
            return new SingleChoiceFilterConfigOption(in);
        }

        @Override
        public SingleChoiceFilterConfigOption[] newArray(int size) {
            return new SingleChoiceFilterConfigOption[size];
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
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof SingleChoiceFilterConfigOption && id().equals(((SingleChoiceFilterConfigOption) obj).id());
    }
}
