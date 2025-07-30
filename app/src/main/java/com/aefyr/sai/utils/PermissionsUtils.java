package com.aefyr.sai.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PermissionsUtils {

    public static boolean hasStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[0];
        }
        return new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }

    public static boolean checkAndRequestStoragePermissions(Fragment fragment, ActivityResultLauncher<String[]> launcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return !Environment.isExternalStorageManager();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : getStoragePermissions()) {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            launcher.launch(permissionsToRequest.toArray(new String[0]));
            return false;
        }
        return true;
    }

    public static boolean checkAndRequestShizukuPermissions(Fragment fragment, ActivityResultLauncher<String> launcher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        String permission = "moe.shizuku.manager.permission.API_V23";
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission);
            return false;
        }
        return true;
    }
}