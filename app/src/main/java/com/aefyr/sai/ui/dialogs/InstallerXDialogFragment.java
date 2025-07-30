package com.aefyr.sai.ui.dialogs;

import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.SplitApkSourceMetaAdapter;
import com.aefyr.sai.installerx.resolver.urimess.UriHostFactory;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.view.ViewSwitcherLayout;
import com.aefyr.sai.viewmodels.InstallerXDialogViewModel;
import com.aefyr.sai.viewmodels.factory.InstallerXDialogViewModelFactory;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstallerXDialogFragment extends BaseBottomSheetDialogFragment implements FilePickerDialogFragment.OnFilesSelectedListener, SimpleAlertDialogFragment.OnDismissListener {
    private static final String ARG_APK_SOURCE_URI = "apk_source_uri";
    private static final String ARG_URI_HOST_FACTORY = "uri_host_factory";
    private static final String PREF_FIRST_RUN = "first_run";
    private static final String DIALOG_TAG_STORAGE_PERMISSION = "storage_permission_dialog";
    private static final String DIALOG_TAG_Q_SAF_WARNING = "q_saf_warning";

    private InstallerXDialogViewModel mViewModel;
    private PreferencesHelper mHelper;

    private int mActionAfterGettingStoragePermissions;
    private static final int PICK_WITH_INTERNAL_FILEPICKER = 0;
    private static final int PICK_WITH_SAF = 1;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFilePickerResult(result.getData());
                }
            });

    private final ActivityResultLauncher<Intent> manageStorageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    AlertsUtils.showAlert(this, R.string.permission_required, R.string.storage_permission_denied_message);
                }
            });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    switch (mActionAfterGettingStoragePermissions) {
                        case PICK_WITH_INTERNAL_FILEPICKER:
                            showFilePicker();
                            break;
                        case PICK_WITH_SAF:
                            pickFilesWithSaf(true);
                            break;
                    }
                } else {
                    AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_storage);
                }
            });

    public static InstallerXDialogFragment newInstance(@Nullable Uri apkSourceUri, @Nullable Class<? extends UriHostFactory> uriHostFactoryClass) {
        Bundle args = new Bundle();
        if (apkSourceUri != null)
            args.putParcelable(ARG_APK_SOURCE_URI, apkSourceUri);

        if (uriHostFactoryClass != null)
            args.putString(ARG_URI_HOST_FACTORY, uriHostFactoryClass.getCanonicalName());

        InstallerXDialogFragment fragment = new InstallerXDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = PreferencesHelper.getInstance(requireContext());

        // Check if it's first run and handle permissions
        if (mHelper.getBoolean(PREF_FIRST_RUN, true)) {
            mHelper.putBoolean(PREF_FIRST_RUN, false);
            handleStoragePermissions();
        }

        Bundle args = getArguments();
        UriHostFactory uriHostFactory = null;
        if (args != null) {
            String uriHostFactoryClass = args.getString(ARG_URI_HOST_FACTORY);
            if (uriHostFactoryClass != null) {
                try {
                    uriHostFactory = (UriHostFactory) Class.forName(uriHostFactoryClass).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        mViewModel = new ViewModelProvider(this, new InstallerXDialogViewModelFactory(requireContext(), uriHostFactory)).get(InstallerXDialogViewModel.class);

        if (args == null)
            return;

        Uri apkSourceUri = args.getParcelable(ARG_APK_SOURCE_URI);
        if (apkSourceUri != null)
            mViewModel.setApkSourceUris(Collections.singletonList(apkSourceUri));
    }

    private void handleStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showManageExternalStorageDialog();
            }
        } else {
            if (!PermissionsUtils.hasStoragePermissions(requireContext())) {
                mActionAfterGettingStoragePermissions = PICK_WITH_INTERNAL_FILEPICKER;
                permissionLauncher.launch(PermissionsUtils.getStoragePermissions());
            }
        }
    }

    private void showManageExternalStorageDialog() {
        SimpleAlertDialogFragment.newInstance(requireContext(),
                R.string.permission_required,
                R.string.android_storage_permission_message,
                (dialog, which) -> {
                    try {
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        } else {
                            // For API < 30, use alternative approach
                            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        }
                        manageStorageLauncher.launch(intent);
                    } catch (Exception e) {
                        // Fallback for devices that don't support the intent
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        manageStorageLauncher.launch(intent);
                    }
                }).show(getChildFragmentManager(), DIALOG_TAG_STORAGE_PERMISSION);
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_installerx, container, false);
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);

        setTitle(R.string.installerx_dialog_title);
        getPositiveButton().setText(R.string.installerx_dialog_install);

        ViewSwitcherLayout viewSwitcher = view.findViewById(R.id.container_dialog_installerx);

        RecyclerView recycler = view.findViewById(R.id.rv_dialog_installerx_content);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.getRecycledViewPool().setMaxRecycledViews(SplitApkSourceMetaAdapter.VH_TYPE_SPLIT_PART, 16);

        SplitApkSourceMetaAdapter adapter = new SplitApkSourceMetaAdapter(mViewModel.getPartsSelection(), this, requireContext());
        recycler.setAdapter(adapter);

        getNegativeButton().setOnClickListener(v -> dismiss());
        getPositiveButton().setOnClickListener(v -> {
            mViewModel.enqueueInstallation();
            dismiss();
        });

        view.findViewById(R.id.button_installerx_fp_internal).setOnClickListener(v -> checkPermissionsAndPickFiles());
        view.findViewById(R.id.button_installerx_fp_saf).setOnClickListener(v -> pickFilesWithSaf(false));

        TextView warningTv = view.findViewById(R.id.tv_installerx_warning);
        mViewModel.getState().observe(this, state -> {
            switch (state) {
                case NO_DATA:
                    viewSwitcher.setShownView(R.id.container_installerx_no_data);
                    getPositiveButton().setVisibility(View.GONE);
                    break;
                case LOADING:
                    viewSwitcher.setShownView(R.id.container_installerx_loading);
                    getPositiveButton().setVisibility(View.GONE);
                    break;
                case LOADED:
                    viewSwitcher.setShownView(R.id.rv_dialog_installerx_content);
                    getPositiveButton().setVisibility(View.VISIBLE);
                    break;
                case WARNING:
                    viewSwitcher.setShownView(R.id.container_installerx_warning);
                    warningTv.setText(mViewModel.getWarning().message());
                    getPositiveButton().setVisibility(mViewModel.getWarning().canInstallAnyway() ? View.VISIBLE : View.GONE);
                    break;
                case ERROR:
                    viewSwitcher.setShownView(R.id.container_installerx_error);
                    getPositiveButton().setVisibility(View.VISIBLE);
                    break;
            }
            revealBottomSheet();
        });

        mViewModel.getMeta().observe(this, meta -> {
            adapter.setMeta(meta);
            revealBottomSheet();
        });

        view.requestFocus();
    }

    private void checkPermissionsAndPickFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showFilePicker();
                return;
            }
            showManageExternalStorageDialog();
        } else {
            if (!PermissionsUtils.hasStoragePermissions(requireContext())) {
                mActionAfterGettingStoragePermissions = PICK_WITH_INTERNAL_FILEPICKER;
                permissionLauncher.launch(PermissionsUtils.getStoragePermissions());
            } else {
                showFilePicker();
            }
        }
    }

    private void showFilePicker() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.offset = new File(mHelper.getHomeDirectory());
        properties.extensions = new String[]{"zip", "apks", "xapk", "apk", "apkm"};
        properties.sortBy = mHelper.getFilePickerSortBy();
        properties.sortOrder = mHelper.getFilePickerSortOrder();

        FilePickerDialogFragment.newInstance(null, getString(R.string.installer_pick_apks), properties)
                .show(getChildFragmentManager(), "dialog_files_picker");
    }

    private void pickFilesWithSaf(boolean ignorePermissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !ignorePermissions) {
            if (!Environment.isExternalStorageManager()) {
                showManageExternalStorageDialog();
                return;
            }
        }

        Intent getContentIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        getContentIntent.setType("application/vnd.android.package-archive");
        getContentIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.android.package-archive",
                "application/zip",
                "application/x-zip-compressed"
        });

        getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(
                Intent.createChooser(getContentIntent, getString(R.string.installer_pick_apks))
        );
    }

    private void handleFilePickerResult(Intent data) {
        if (data.getData() != null) {
            mViewModel.setApkSourceUris(Collections.singletonList(data.getData()));
            return;
        }

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            List<Uri> apkUris = new ArrayList<>(clipData.getItemCount());

            for (int i = 0; i < clipData.getItemCount(); i++)
                apkUris.add(clipData.getItemAt(i).getUri());

            mViewModel.setApkSourceUris(apkUris);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mViewModel.getState().getValue() == InstallerXDialogViewModel.State.LOADING)
            mViewModel.cancelParsing();
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        mViewModel.setApkSourceFiles(files);
    }

    @Override
    public void onDialogDismissed(@NonNull String dialogTag) {
        if (DIALOG_TAG_Q_SAF_WARNING.equals(dialogTag)) {
            mActionAfterGettingStoragePermissions = PICK_WITH_SAF;
            if (PermissionsUtils.hasStoragePermissions(requireContext())) {
                pickFilesWithSaf(false);
            }
        }
    }
}