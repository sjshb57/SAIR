package com.aefyr.sai.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.aefyr.sai.R;
import com.aefyr.sai.shell.SuShell;
import com.aefyr.sai.ui.activities.AboutActivity;
import com.aefyr.sai.ui.activities.ApkActionViewProxyActivity;
import com.aefyr.sai.ui.activities.BackupSettingsActivity;
import com.aefyr.sai.ui.activities.DonateActivity;
import com.aefyr.sai.ui.dialogs.DarkLightThemeSelectionDialogFragment;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;
import com.aefyr.sai.ui.dialogs.SingleChoiceListDialogFragment;
import com.aefyr.sai.ui.dialogs.ThemeSelectionDialogFragment;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.PreferencesKeys;
import com.aefyr.sai.utils.PreferencesValues;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;

import rikka.shizuku.Shizuku;

public class PreferencesFragment extends PreferenceFragmentCompat implements FilePickerDialogFragment.OnFilesSelectedListener, SingleChoiceListDialogFragment.OnItemSelectedListener, BaseBottomSheetDialogFragment.OnDismissListener, SharedPreferences.OnSharedPreferenceChangeListener, DarkLightThemeSelectionDialogFragment.OnDarkLightThemesChosenListener, Shizuku.OnRequestPermissionResultListener {

    private PreferencesHelper mHelper;
    private PackageManager mPm;
    private Preference mHomeDirPref;
    private Preference mFilePickerSortPref;
    private Preference mInstallerPref;
    private Preference mThemePref;
    private SwitchPreference mAutoThemeSwitch;
    private Preference mAutoThemePicker;
    private FilePickerDialogFragment mPendingFilePicker;

    private final ActivityResultLauncher<String[]> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean isGranted : result.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted && mPendingFilePicker != null) {
                    mPendingFilePicker.show(Objects.requireNonNull(getChildFragmentManager()), null);
                    mPendingFilePicker = null;
                } else if (!allGranted) {
                    AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_storage);
                }
            });

    private final ActivityResultLauncher<String> shizukuPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    mHelper.setInstaller(PreferencesValues.INSTALLER_SHIZUKU);
                    updateInstallerSummary();
                } else {
                    AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_shizuku);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mHelper = PreferencesHelper.getInstance(requireContext());
        mPm = requireContext().getPackageManager();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(PreferencesKeys.AUTO_THEME, Theme.getInstance(requireContext()).getThemeMode() == Theme.Mode.AUTO_LIGHT_DARK).apply();

        int apkProxyActivityState = mPm.getComponentEnabledSetting(ApkActionViewProxyActivity.getComponentName(requireContext()));
        boolean isApkProxyActivityEnabled = apkProxyActivityState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                apkProxyActivityState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        prefsEditor.putBoolean(PreferencesKeys.ENABLE_APK_ACTION_VIEW, isApkProxyActivityEnabled);
        prefsEditor.apply();

        if (Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
            Shizuku.addRequestPermissionResultListener(this);
        }

        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

        Preference aboutPref = findPreference("about");
        if (aboutPref != null) {
            aboutPref.setOnPreferenceClickListener((p) -> {
                startActivity(new Intent(getContext(), AboutActivity.class));
                return true;
            });
        }

        Preference donatePref = findPreference("donate");
        if (donatePref != null) {
            donatePref.setOnPreferenceClickListener(p -> {
                startActivity(new Intent(requireContext(), DonateActivity.class));
                return true;
            });
            donatePref.setVisible(false);
        }

        mHomeDirPref = findPreference("home_directory");
        if (mHomeDirPref != null) {
            updateHomeDirPrefSummary();
            mHomeDirPref.setOnPreferenceClickListener((p) -> {
                selectHomeDir();
                return true;
            });
        }

        mFilePickerSortPref = findPreference("file_picker_sort");
        if (mFilePickerSortPref != null) {
            updateFilePickerSortSummary();
            mFilePickerSortPref.setOnPreferenceClickListener((p) -> {
                SingleChoiceListDialogFragment.newInstance(getText(R.string.settings_main_file_picker_sort), R.array.file_picker_sort_variants, mHelper.getFilePickerRawSort()).show(getChildFragmentManager(), "sort");
                return true;
            });
        }

        mInstallerPref = findPreference("installer");
        if (mInstallerPref != null) {
            updateInstallerSummary();
            mInstallerPref.setOnPreferenceClickListener((p -> {
                SingleChoiceListDialogFragment.newInstance(getText(R.string.settings_main_installer), R.array.installers, mHelper.getInstaller()).show(getChildFragmentManager(), "installer");
                return true;
            }));
        }

        Preference backupSettingsPref = findPreference(PreferencesKeys.BACKUP_SETTINGS);
        if (backupSettingsPref != null) {
            backupSettingsPref.setOnPreferenceClickListener(p -> {
                startActivity(new Intent(requireContext(), BackupSettingsActivity.class));
                return true;
            });
        }

        mThemePref = findPreference(PreferencesKeys.THEME);
        if (mThemePref != null) {
            updateThemeSummary();
            mThemePref.setOnPreferenceClickListener(p -> {
                ThemeSelectionDialogFragment.newInstance(requireContext()).show(getChildFragmentManager(), "theme");
                return true;
            });
            if (Theme.getInstance(requireContext()).getThemeMode() != Theme.Mode.CONCRETE) {
                mThemePref.setVisible(false);
            }
        }

        mAutoThemeSwitch = findPreference(PreferencesKeys.AUTO_THEME);
        mAutoThemePicker = findPreference(PreferencesKeys.AUTO_THEME_PICKER);
        if (mAutoThemeSwitch != null && mAutoThemePicker != null) {
            updateAutoThemePickerSummary();

            mAutoThemeSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean value = (boolean) newValue;
                if (value) {
                    if (!Utils.apiIsAtLeast(Build.VERSION_CODES.Q)) {
                        SimpleAlertDialogFragment.newInstance(requireContext(), R.string.settings_main_auto_theme, R.string.settings_main_auto_theme_pre_q_warning).show(getChildFragmentManager(), null);
                    }
                    Theme.getInstance(requireContext()).setMode(Theme.Mode.AUTO_LIGHT_DARK);
                } else {
                    Theme.getInstance(requireContext()).setMode(Theme.Mode.CONCRETE);
                }
                requireActivity().recreate();
                return true;
            });

            mAutoThemePicker.setOnPreferenceClickListener(pref -> {
                DarkLightThemeSelectionDialogFragment.newInstance().show(getChildFragmentManager(), null);
                return true;
            });

            if (Theme.getInstance(requireContext()).getThemeMode() != Theme.Mode.AUTO_LIGHT_DARK) {
                mAutoThemePicker.setVisible(false);
            }
        }

        SwitchPreference enableApkActionViewPref = findPreference(PreferencesKeys.ENABLE_APK_ACTION_VIEW);
        if (enableApkActionViewPref != null) {
            enableApkActionViewPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (boolean) newValue;
                mPm.setComponentEnabledSetting(ApkActionViewProxyActivity.getComponentName(requireContext()),
                        enabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                return true;
            });
        }

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDividerHeight(0);
    }

    private void openFilePicker(FilePickerDialogFragment filePicker) {
        if (PermissionsUtils.checkAndRequestStoragePermissions(this, storagePermissionLauncher)) {
            mPendingFilePicker = filePicker;
            return;
        }
        filePicker.show(Objects.requireNonNull(getChildFragmentManager()), null);
    }

    private void selectHomeDir() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = Environment.getExternalStorageDirectory();

        openFilePicker(FilePickerDialogFragment.newInstance("home", getString(R.string.settings_main_pick_dir), properties));
    }

    private void updateHomeDirPrefSummary() {
        if (mHomeDirPref != null) {
            mHomeDirPref.setSummary(getString(R.string.settings_main_home_directory_summary, mHelper.getHomeDirectory()));
        }
    }

    private void updateFilePickerSortSummary() {
        if (mFilePickerSortPref != null) {
            mFilePickerSortPref.setSummary(getString(R.string.settings_main_file_picker_sort_summary,
                    getResources().getStringArray(R.array.file_picker_sort_variants)[mHelper.getFilePickerRawSort()]));
        }
    }

    private void updateInstallerSummary() {
        if (mInstallerPref != null) {
            mInstallerPref.setSummary(getString(R.string.settings_main_installer_summary,
                    getResources().getStringArray(R.array.installers)[mHelper.getInstaller()]));
        }
    }

    private void updateThemeSummary() {
        if (mThemePref != null) {
            mThemePref.setSummary(Theme.getInstance(requireContext()).getConcreteTheme().getName(requireContext()));
        }
    }

    private void updateAutoThemePickerSummary() {
        if (mAutoThemePicker != null) {
            Theme theme = Theme.getInstance(requireContext());
            mAutoThemePicker.setSummary(getString(R.string.settings_main_auto_theme_picker_summary,
                    theme.getLightTheme().getName(requireContext()), theme.getDarkTheme().getName(requireContext())));
        }
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        if ("home".equals(tag) && !files.isEmpty()) {
            mHelper.setHomeDirectory(files.get(0).getAbsolutePath());
            updateHomeDirPrefSummary();
        }
    }

    @Override
    public void onItemSelected(String dialogTag, int selectedItemIndex) {
        if ("sort".equals(dialogTag)) {
            mHelper.setFilePickerRawSort(selectedItemIndex);
            switch (selectedItemIndex) {
                case 0:
                    mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_NAME);
                    mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_NORMAL);
                    break;
                case 1:
                    mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_NAME);
                    mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_REVERSE);
                    break;
                case 2:
                    mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_LAST_MODIFIED);
                    mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_NORMAL);
                    break;
                case 3:
                    mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_LAST_MODIFIED);
                    mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_REVERSE);
                    break;
                case 4:
                    mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_SIZE);
                    mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_REVERSE);
                    break;
                case 5:
                    mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_SIZE);
                    mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_NORMAL);
                    break;
            }
            updateFilePickerSortSummary();
        } else if ("installer".equals(dialogTag)) {
            boolean installerSet = false;
            switch (selectedItemIndex) {
                case PreferencesValues.INSTALLER_ROOTLESS:
                    installerSet = true;
                    break;
                case PreferencesValues.INSTALLER_ROOTED:
                    if (!SuShell.getInstance().requestRoot()) {
                        AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_use_root_error);
                        return;
                    }
                    installerSet = true;
                    break;
                case PreferencesValues.INSTALLER_SHIZUKU:
                    if (!Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
                        AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_installer_error_shizuku_pre_m);
                        return;
                    }

                    if (!Shizuku.pingBinder()) {
                        AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_installer_error_no_shizuku);
                        return;
                    }

                    if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
                        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                            installerSet = true;
                        } else {
                            Shizuku.requestPermission(111);
                        }
                    } else {
                        installerSet = PermissionsUtils.checkAndRequestShizukuPermissions(this, shizukuPermissionLauncher);
                    }
                    break;
            }
            if (installerSet) {
                mHelper.setInstaller(selectedItemIndex);
                updateInstallerSummary();
            }
        }
    }

    @Override
    public void onDialogDismissed(@NonNull String dialogTag) {
        if ("theme".equals(dialogTag)) {
            updateThemeSummary();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        if (Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
            Shizuku.removeRequestPermissionResultListener(this);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (PreferencesKeys.USE_OLD_INSTALLER.equals(key)) {
            prefs.edit().putBoolean(PreferencesKeys.USE_OLD_INSTALLER, prefs.getBoolean(PreferencesKeys.USE_OLD_INSTALLER, false)).commit();
            Utils.hardRestartApp(requireContext());
        }
    }

    @Override
    public void onThemesChosen(@Nullable String tag, Theme.ThemeDescriptor lightTheme, Theme.ThemeDescriptor darkTheme) {
        Theme theme = Theme.getInstance(requireContext());
        theme.setLightTheme(lightTheme);
        theme.setDarkTheme(darkTheme);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, int grantResult) {
        if (requestCode == 111) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_shizuku);
            } else {
                mHelper.setInstaller(PreferencesValues.INSTALLER_SHIZUKU);
                updateInstallerSummary();
            }
        }
    }
}