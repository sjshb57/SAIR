package com.aefyr.flexfilter.config.core.util;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ParcelCompat {

    public static <T extends Parcelable> void writeParcelableList(Parcel parcel, @Nullable List<T> val, int flags) {
        try {
            parcel.writeParcelableList(val, flags);
        } catch (NoSuchMethodError e) {
            if (val == null) {
                parcel.writeInt(-1);
                return;
            }

            int N = val.size();
            int i = 0;
            parcel.writeInt(N);
            while (i < N) {
                parcel.writeParcelable(val.get(i), flags);
                i++;
            }
        }
    }

    public static <T extends Parcelable> List<T> readParcelableList(Parcel parcel, @NonNull List<T> list, @Nullable ClassLoader cl) {
        try {
            return parcel.readParcelableList(list, cl);
        } catch (NoSuchMethodError e) {
            final int N = parcel.readInt();
            if (N == -1) {
                list.clear();
                return list;
            }

            final int M = list.size();
            int i = 0;
            for (; i < M && i < N; i++) {
                list.set(i, parcel.readParcelable(cl));
            }
            for (; i < N; i++) {
                list.add(parcel.readParcelable(cl));
            }
            for (; i < M; i++) {
                list.remove(N);
            }
            return list;
        }
    }

}
