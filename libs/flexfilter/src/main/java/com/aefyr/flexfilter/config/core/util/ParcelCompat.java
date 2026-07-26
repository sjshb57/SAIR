package com.aefyr.flexfilter.config.core.util;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Parcel.writeParcelableList / readParcelableList only exist from API 29 onwards, so on older
 * releases the list is marshalled manually with the same wire format.
 */
public class ParcelCompat {

    public static <T extends Parcelable> void writeParcelableList(Parcel parcel, @Nullable List<T> val, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            parcel.writeParcelableList(val, flags);
            return;
        }

        if (val == null) {
            parcel.writeInt(-1);
            return;
        }

        int size = val.size();
        parcel.writeInt(size);
        for (int i = 0; i < size; i++) {
            parcel.writeParcelable(val.get(i), flags);
        }
    }

    public static <T extends Parcelable> List<T> readParcelableList(Parcel parcel, @NonNull List<T> list, @Nullable ClassLoader cl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return parcel.readParcelableList(list, cl);
        }

        final int incoming = parcel.readInt();
        if (incoming == -1) {
            list.clear();
            return list;
        }

        final int existing = list.size();
        int i = 0;
        for (; i < existing && i < incoming; i++) {
            list.set(i, parcel.readParcelable(cl));
        }
        for (; i < incoming; i++) {
            list.add(parcel.readParcelable(cl));
        }
        for (; i < existing; i++) {
            list.remove(incoming);
        }
        return list;
    }
}
