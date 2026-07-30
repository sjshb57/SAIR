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

package com.github.angads25.filepicker.view;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.angads25.filepicker.R;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.controller.adapters.FileListAdapter;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.model.FileListItem;
import com.github.angads25.filepicker.model.MarkedItemList;
import com.github.angads25.filepicker.utils.ExtensionFilter;
import com.github.angads25.filepicker.utils.Utility;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.File;
import java.util.ArrayList;
import android.annotation.SuppressLint;

/**
 * <p>
 * Created by Angad Singh on 09-07-2016.
 * </p >
 */
public class FilePickerDialog extends Dialog implements AdapterView.OnItemClickListener {
    private final Context context;
    private ListView listView;
    private TextView dname, dir_path, title;
    private DialogProperties properties;
    private DialogSelectionListener callbacks;
    private ArrayList<FileListItem> internalList;
    private ExtensionFilter filter;
    private FileListAdapter mFileListAdapter;
    private Button select;
    private String titleStr = null;
    private String positiveBtnNameStr = null;
    private String negativeBtnNameStr = null;
    private File currentDirectory;

    public static final int EXTERNAL_READ_PERMISSION_GRANT = 112;

    public FilePickerDialog(Context context) {
        this(context, new DialogProperties(), R.style.FilePickerDialog_DefaultTheme);
    }

    public FilePickerDialog(Context context, DialogProperties properties) {
        this(context, properties, R.style.FilePickerDialog_DefaultTheme);
    }

    public FilePickerDialog(Context context, DialogProperties properties, int themeResId) {
        super(context, themeResId);
        this.context = context;
        this.properties = properties;
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_main);
        applySystemBarInsets();
        listView = findViewById(R.id.fileList);
        select = findViewById(R.id.select);
        int size = MarkedItemList.getFileCount();
        if (size == 0 && !(properties.selection_mode == DialogConfigs.SINGLE_MODE &&
                (properties.selection_type == DialogConfigs.DIR_SELECT ||
                        properties.selection_type == DialogConfigs.FILE_AND_DIR_SELECT))) {
            select.setEnabled(false);
        }
        if (properties.selection_mode == DialogConfigs.SINGLE_MODE &&
                properties.selection_type == DialogConfigs.FILE_SELECT) {
            select.setVisibility(View.GONE);
        } else {
            select.setVisibility(View.VISIBLE);
        }

        dname = findViewById(R.id.dname);
        title = findViewById(R.id.title);
        dir_path = findViewById(R.id.dir_path);
        Button cancel = findViewById(R.id.cancel);
        if (negativeBtnNameStr != null) {
            cancel.setText(negativeBtnNameStr);
        }

        select.setOnClickListener(view -> {
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE &&
                    (properties.selection_type == DialogConfigs.DIR_SELECT ||
                            properties.selection_type == DialogConfigs.FILE_AND_DIR_SELECT)) {
                FileListItem item = new FileListItem();
                item.setFilename(currentDirectory.getName());
                item.setDirectory(currentDirectory.isDirectory());
                item.setLocation(currentDirectory.getAbsolutePath());
                item.setTime(currentDirectory.lastModified());
                item.setSize(currentDirectory.length());
                MarkedItemList.addSingleFile(item);
            }
            finishSelection();
        });

        cancel.setOnClickListener(view -> cancel());

        mFileListAdapter = new FileListAdapter(internalList, context, properties);
        mFileListAdapter.setNotifyItemCheckedListener(() -> {
            positiveBtnNameStr = positiveBtnNameStr == null ?
                    context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
            int currentSize = MarkedItemList.getFileCount();
            if (currentSize == 0) {
                select.setEnabled(false);
                select.setText(positiveBtnNameStr);
            } else {
                select.setEnabled(true);
                String buttonLabel = positiveBtnNameStr + " (" + currentSize + ") ";
                select.setText(buttonLabel);
            }
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                mFileListAdapter.notifyDataSetChanged();
            }
        });

        listView.setAdapter(mFileListAdapter);
        setTitle();
    }

    private void setTitle() {
        if (title == null || dname == null) {
            return;
        }
        if (titleStr != null) {
            if (title.getVisibility() == View.INVISIBLE) {
                title.setVisibility(View.VISIBLE);
            }
            title.setText(titleStr);
            if (dname.getVisibility() == View.VISIBLE) {
                dname.setVisibility(View.INVISIBLE);
            }
        } else {
            if (title.getVisibility() == View.VISIBLE) {
                title.setVisibility(View.INVISIBLE);
            }
            if (dname.getVisibility() == View.INVISIBLE) {
                dname.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        positiveBtnNameStr = positiveBtnNameStr == null ?
                context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
        select.setText(positiveBtnNameStr);

        if (Utility.checkStorageAccessPermissions(context)) {
            internalList.clear();
            File rootDir = properties.root;
            File offsetDir = properties.offset;
            File errorDir = properties.error_dir;

            if (offsetDir != null && offsetDir.isDirectory() && validateOffsetPath()) {
                currentDirectory = new File(offsetDir.getAbsolutePath());
                addParentDirectoryItem(currentDirectory);
            } else if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
                currentDirectory = new File(rootDir.getAbsolutePath());
            } else if (errorDir != null) {
                currentDirectory = new File(errorDir.getAbsolutePath());
            } else {
                currentDirectory = new File("/");
            }

            updateDirectoryViews(currentDirectory);
            internalList = Utility.prepareFileListEntries(internalList, currentDirectory, filter,
                    Utility.createFileListItemsComparator(properties));
            mFileListAdapter.notifyDataSetChanged();
            listView.setOnItemClickListener(this);
        }
    }

    private void addParentDirectoryItem(File currentDir) {
        File parent = currentDir.getParentFile();
        if (parent != null) {
            FileListItem parentItem = new FileListItem();
            parentItem.setFilename(context.getString(R.string.label_parent_dir));
            parentItem.setDirectory(true);
            parentItem.setLocation(parent.getAbsolutePath());
            parentItem.setTime(currentDir.lastModified());
            parentItem.setSize(currentDir.length());
            internalList.add(parentItem);
        }
    }

    private void updateDirectoryViews(File directory) {
        if (directory != null) {
            dname.setText(directory.getName());
            dir_path.setText(directory.getAbsolutePath());
            setTitle();
        }
    }

    private boolean validateOffsetPath() {
        if (properties.offset == null || properties.root == null) {
            return false;
        }
        String offsetPath = properties.offset.getAbsolutePath();
        String rootPath = properties.root.getAbsolutePath();
        return !offsetPath.equals(rootPath) && offsetPath.contains(rootPath);
    }

    /**
     * The picker is themed with a full app theme, so its window is not floating and gets laid out
     * edge to edge from API 35 on, putting the header under the status bar.
     */
    private void applySystemBarInsets() {
        applyInsets(findViewById(R.id.header), true, false);
        applyInsets(findViewById(R.id.fileList), false, false);
        applyInsets(findViewById(R.id.footer), false, true);
    }

    /** Horizontal insets are always applied, for display cutouts and gesture bars in landscape. */
    private static void applyInsets(@Nullable View view, boolean top, boolean bottom) {
        if (view == null)
            return;

        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(paddingLeft + insets.left,
                    paddingTop + (top ? insets.top : 0),
                    paddingRight + insets.right,
                    paddingBottom + (bottom ? insets.bottom : 0));
            return windowInsets;
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!internalList.isEmpty() && internalList.size() > position) {
            FileListItem item = internalList.get(position);
            if (item.isDirectory()) {
                handleDirectoryClick(item);
            } else {
                handleFileClick(view, item);
            }
        }
    }

    private void handleDirectoryClick(@NonNull FileListItem item) {
        File dir = new File(item.getLocation());
        if (dir.canRead()) {
            currentDirectory = dir;
            updateDirectoryViews(currentDirectory);
            internalList.clear();

            if (!currentDirectory.getName().equals(properties.root.getName())) {
                addParentDirectoryItem(currentDirectory);
            }

            internalList = Utility.prepareFileListEntries(internalList, currentDirectory, filter,
                    Utility.createFileListItemsComparator(properties));
            mFileListAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(context, R.string.error_dir_access, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFileClick(View view, @NonNull FileListItem item) {
        if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
            MarkedItemList.addSingleFile(item);
            finishSelection();
        } else {
            MaterialCheckBox checkBox = view.findViewById(R.id.file_mark);
            checkBox.performClick();
        }
    }

    void setProperties(DialogProperties properties) {
        this.properties = properties;
        filter = new ExtensionFilter(properties);
    }

    public void setDialogSelectionListener(DialogSelectionListener callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void setTitle(CharSequence titleStr) {
        this.titleStr = titleStr != null ? titleStr.toString() : null;
        setTitle();
    }

    public void setPositiveBtnName(CharSequence positiveBtnNameStr) {
        this.positiveBtnNameStr = positiveBtnNameStr != null ? positiveBtnNameStr.toString() : null;
    }

    public void setNegativeBtnName(CharSequence negativeBtnNameStr) {
        this.negativeBtnNameStr = negativeBtnNameStr != null ? negativeBtnNameStr.toString() : null;
    }

    @Override
    public void show() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                !Utility.checkStorageAccessPermissions(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    ((Activity) context).requestPermissions(
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            EXTERNAL_READ_PERMISSION_GRANT);
                } catch (ClassCastException e) {
                    super.show();
                    updateSelectionUI();
                }
            }
        } else {
            super.show();
            updateSelectionUI();
        }
    }

    private void updateSelectionUI() {
        positiveBtnNameStr = positiveBtnNameStr == null ?
                context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
        int size = MarkedItemList.getFileCount();
        if (size == 0) {
            select.setText(positiveBtnNameStr);
        } else {
            String buttonLabel = positiveBtnNameStr + " (" + size + ") ";
            select.setText(buttonLabel);
        }
    }

    // Dialog.onBackPressed is still dispatched; the lint check targets Activity.
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        String currentDirName = dname.getText().toString();
        if (!internalList.isEmpty()) {
            FileListItem firstItem = internalList.get(0);
            File currentLocation = new File(firstItem.getLocation());

            if (currentDirName.equals(properties.root.getName()) || !currentLocation.canRead()) {
                super.onBackPressed();
            } else {
                updateDirectoryViews(currentLocation);
                internalList.clear();

                if (!currentLocation.getName().equals(properties.root.getName())) {
                    addParentDirectoryItem(currentLocation);
                }

                internalList = Utility.prepareFileListEntries(internalList, currentLocation, filter,
                        Utility.createFileListItemsComparator(properties));
                mFileListAdapter.notifyDataSetChanged();
            }
            setTitle();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void dismiss() {
        MarkedItemList.clearSelectionList();
        internalList.clear();
        super.dismiss();
    }

    private void finishSelection() {
        String[] selectedPaths = MarkedItemList.getSelectedPaths();
        if (callbacks != null) {
            callbacks.onSelectedFilePaths(selectedPaths);
        }
        dismiss();
    }

    @SuppressWarnings("unused")
    public void setShowHiddenFiles(boolean show) {
        properties.showHiddenFiles = show;
        filter = new ExtensionFilter(properties);
    }
}