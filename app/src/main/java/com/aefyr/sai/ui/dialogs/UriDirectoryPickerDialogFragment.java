package com.aefyr.sai.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.Utils;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class UriDirectoryPickerDialogFragment extends SingleChoiceListDialogFragment implements FilePickerDialogFragment.OnFilesSelectedListener {
    private static final int BACKUP_DIR_SELECTION_METHOD_INTERNAL = 0;
    private static final int BACKUP_DIR_SELECTION_METHOD_SAF = 1;
    private static final String BACKUP_DIR_TAG = "backup_dir";

    private FilePickerDialogFragment mPendingFilePicker;
    private final ActivityResultLauncher<Intent> safDirectoryPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleSafDirectoryResult(result.getData());
                }
            });

    private final ActivityResultLauncher<String[]> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted && mPendingFilePicker != null) {
                    openFilePicker(mPendingFilePicker);
                    mPendingFilePicker = null;
                } else if (!allGranted) {
                    AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_storage);
                }
            });

    public static UriDirectoryPickerDialogFragment newInstance(Context context) {
        UriDirectoryPickerDialogFragment fragment = new UriDirectoryPickerDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAMS, new DialogParams(context.getText(R.string.settings_main_backup_backup_dir_dialog), R.array.backup_dir_selection_methods));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void deliverSelectionResult(String tag, int selectedItemIndex) {
        if (selectedItemIndex == BACKUP_DIR_SELECTION_METHOD_INTERNAL) {
            DialogProperties properties = new DialogProperties();
            properties.selection_mode = DialogConfigs.SINGLE_MODE;
            properties.selection_type = DialogConfigs.DIR_SELECT;
            properties.root = Environment.getExternalStorageDirectory();

            openFilePicker(FilePickerDialogFragment.newInstance(BACKUP_DIR_TAG, getString(R.string.settings_main_pick_dir), properties));
        } else if (selectedItemIndex == BACKUP_DIR_SELECTION_METHOD_SAF) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            safDirectoryPickerLauncher.launch(Intent.createChooser(intent, getString(R.string.installer_pick_apks)));
        }
    }

    private void openFilePicker(FilePickerDialogFragment filePicker) {
        if (PermissionsUtils.checkAndRequestStoragePermissions(this, storagePermissionLauncher)) {
            mPendingFilePicker = filePicker;
            return;
        }
        filePicker.show(getChildFragmentManager(), null);
    }

    private void handleSafDirectoryResult(Intent data) {
        Uri backupDirUri = Objects.requireNonNull(data.getData());
        requireContext().getContentResolver().takePersistableUriPermission(backupDirUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        onDirectoryPicked(backupDirUri);
    }

    private void onDirectoryPicked(Uri dirUri) {
        OnDirectoryPickedListener listener = Utils.getParentAs(this, OnDirectoryPickedListener.class);
        if (listener != null) {
            listener.onDirectoryPicked(getTag(), dirUri);
        }
        dismiss();
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        if (BACKUP_DIR_TAG.equals(tag) && !files.isEmpty()) {
            onDirectoryPicked(new Uri.Builder()
                    .scheme("file")
                    .path(files.get(0).getAbsolutePath())
                    .build());
        }
    }

    public interface OnDirectoryPickedListener {
        void onDirectoryPicked(@Nullable String tag, Uri dirUri);
    }
}