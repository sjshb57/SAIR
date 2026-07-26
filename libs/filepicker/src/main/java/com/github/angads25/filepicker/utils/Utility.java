/*
 * Copyright (C) 2016 Angad Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.angads25.filepicker.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.util.Log;

import com.github.angads25.filepicker.R;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.model.FileListItem;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Created by Angad Singh on 11-07-2016.
 * </p >
 */
public class Utility {
    private static final String TAG = "FilePickerUtility";

    /**
     * Post Lollipop Devices require permissions on Runtime (Risky Ones), even though it has been
     * specified in the uses-permission tag of manifest. checkStorageAccessPermissions
     * method checks whether the READ EXTERNAL STORAGE permission has been granted to
     * the Application.
     *
     * @return a boolean value notifying whether the permission is granted or not.
     */
    public static boolean checkStorageAccessPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
                int res = context.checkCallingOrSelfPermission(permission);
                return (res == PackageManager.PERMISSION_GRANTED);
            } catch (Exception e) {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Prepares the list of Files and Folders inside 'inter' Directory.
     * The list can be filtered through extensions. 'filter' reference
     * is the FileFilter. A reference of ArrayList is passed, in case it
     * may contain the ListItem for parent directory. Returns the List of
     * Directories/files in the form of ArrayList.
     *
     * @param internalList ArrayList containing parent directory.
     * @param inter        The present directory to look into.
     * @param filter       Extension filter class reference, for filtering files.
     * @return ArrayList of FileListItem containing file info of current directory.
     */
    public static ArrayList<FileListItem> prepareFileListEntries(ArrayList<FileListItem> internalList, File inter, ExtensionFilter filter, Comparator<FileListItem> sorter) {
        try {
            File[] files = inter.listFiles(filter);
            if (files != null) {
                for (File name : files) {
                    if (name.canRead()) {
                        FileListItem item = new FileListItem();
                        item.setFilename(name.getName());
                        item.setDirectory(name.isDirectory());
                        item.setLocation(name.getAbsolutePath());
                        item.setTime(name.lastModified());
                        item.setSize(name.length());
                        internalList.add(item);
                    }
                }
                internalList.sort(sorter);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Error while preparing file list entries", e);
            internalList = new ArrayList<>();
        }
        return internalList;
    }

    public static Comparator<FileListItem> createFileListItemsComparator(DialogProperties properties) {
        final Comparator<FileListItem> comparator;
        final boolean reversed = properties.sortOrder == DialogConfigs.SORT_ORDER_REVERSE;

        switch (properties.sortBy) {
            case DialogConfigs.SORT_BY_LAST_MODIFIED:
                comparator = (item1, item2) -> {
                    if (item2.isDirectory() && item1.isDirectory()) {
                        if (item1.getFilename().equals("..."))
                            return -1;

                        if (item2.getFilename().equals("..."))
                            return 1;

                        return Long.compare(item2.getTime(), item1.getTime()) * (reversed ? -1 : 1);
                    } else if (!item2.isDirectory() && !item1.isDirectory()) {
                        return Long.compare(item2.getTime(), item1.getTime()) * (reversed ? -1 : 1);
                    } else if (item2.isDirectory() && !item1.isDirectory()) {
                        return 1;
                    } else {
                        return -1;
                    }
                };
                break;
            case DialogConfigs.SORT_BY_NAME:
                comparator = (item1, item2) -> {
                    if (item2.isDirectory() && item1.isDirectory()) {
                        if (item1.getFilename().equals("..."))
                            return -1;

                        if (item2.getFilename().equals("..."))
                            return 1;

                        return item1.getFilename().toLowerCase(Locale.getDefault()).compareTo(item2.getFilename().toLowerCase(Locale.getDefault())) * (reversed ? -1 : 1);
                    } else if (!item2.isDirectory() && !item1.isDirectory()) {
                        return item1.getFilename().toLowerCase(Locale.getDefault()).compareTo(item2.getFilename().toLowerCase(Locale.getDefault())) * (reversed ? -1 : 1);
                    } else if (item2.isDirectory() && !item1.isDirectory()) {
                        return 1;
                    } else {
                        return -1;
                    }
                };
                break;
            case DialogConfigs.SORT_BY_SIZE:
                comparator = (item1, item2) -> {
                    if (item2.isDirectory() && item1.isDirectory()) {
                        if (item1.getFilename().equals("..."))
                            return -1;

                        if (item2.getFilename().equals("..."))
                            return 1;

                        return item1.getFilename().toLowerCase(Locale.getDefault()).compareTo(item2.getFilename().toLowerCase(Locale.getDefault()));
                    } else if (!item2.isDirectory() && !item1.isDirectory()) {
                        return Long.compare(item2.getSize(), item1.getSize()) * (reversed ? -1 : 1);
                    } else if (item2.isDirectory() && !item1.isDirectory()) {
                        return 1;
                    } else {
                        return -1;
                    }
                };
                break;
            default:
                comparator = FileListItem::compareTo;
        }

        return comparator;
    }

    /**
     * DecimalFormat is not thread safe, so each thread gets its own instance instead of sharing
     * a lazily initialised static one.
     */
    private static final ThreadLocal<DecimalFormat> sSizeDecimalFormat = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("#.##");
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        return format;
    });

    public static String formatSize(Context c, long bytes) {
        DecimalFormat sizeFormat = Objects.requireNonNull(sSizeDecimalFormat.get());

        String[] units = c.getResources().getStringArray(R.array.size_units);

        for (int i = 0; i < units.length; i++) {
            float size = (float) bytes / (float) Math.pow(1024, i);
            if (size < 1024)
                return String.format("%s %s", sizeFormat.format(size), units[i]);
        }

        return bytes + " B";
    }
}