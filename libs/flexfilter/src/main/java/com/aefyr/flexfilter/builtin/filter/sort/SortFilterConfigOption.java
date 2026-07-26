package com.aefyr.flexfilter.builtin.filter.sort;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class SortFilterConfigOption implements Parcelable {

    private String mId;
    private CharSequence mName;
    private boolean mSelected;

    private SortFilterConfig mFilter;

    private boolean mAscending;

    SortFilterConfigOption(String id, CharSequence name) {
        mId = id;
        mName = name;
    }

    void setFilter(SortFilterConfig filter) {
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

    public void setAscending(boolean ascending) {
        mAscending = ascending;
    }

    public boolean ascending() {
        return mAscending;
    }

    void setSelectedInternal(boolean selected) {
        mSelected = selected;
    }

    protected SortFilterConfigOption(Parcel in) {
        mId = in.readString();
        mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mSelected = in.readByte() != 0;
        mAscending = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        TextUtils.writeToParcel(mName, dest, flags);
        dest.writeByte((byte) (mSelected ? 1 : 0));
        dest.writeByte((byte) (mAscending ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SortFilterConfigOption> CREATOR = new Creator<SortFilterConfigOption>() {
        @Override
        public SortFilterConfigOption createFromParcel(Parcel in) {
            return new SortFilterConfigOption(in);
        }

        @Override
        public SortFilterConfigOption[] newArray(int size) {
            return new SortFilterConfigOption[size];
        }
    };
}
