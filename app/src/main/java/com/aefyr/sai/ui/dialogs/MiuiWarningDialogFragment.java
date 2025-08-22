package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.PreferencesKeys;

public class MiuiWarningDialogFragment extends DialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.installer_miui_warning_title)
                .setMessage(R.string.installer_miui_warning_message)
                .setPositiveButton(R.string.installer_miui_warning_open_dev_settings, (d, w) -> {
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit()
                            .putBoolean(PreferencesKeys.MIUI_WARNING_SHOWN, true)
                            .apply();

                    try {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    } catch (Exception e) {
                        SimpleAlertDialogFragment.newInstance(
                                        requireContext().getString(R.string.error),
                                        requireContext().getString(R.string.installer_miui_warning_oof))
                                .show(requireActivity().getSupportFragmentManager(), "alert_oof");
                    }

                    dismiss();
                })
                .create();
    }
}