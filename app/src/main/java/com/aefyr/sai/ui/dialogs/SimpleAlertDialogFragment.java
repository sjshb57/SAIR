package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;

public class SimpleAlertDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_SHOW_POSITIVE_BUTTON = "show_positive_button";

    private CharSequence mTitle;
    private CharSequence mMessage;
    private DialogInterface.OnClickListener mPositiveButtonListener;

    public static SimpleAlertDialogFragment newInstance(Context c, @StringRes int title, @StringRes int message) {
        return newInstance(c.getText(title), c.getText(message));
    }

    public static SimpleAlertDialogFragment newInstance(CharSequence title, CharSequence message) {
        return newInstance(title, message, null);
    }

    public static SimpleAlertDialogFragment newInstance(Context c, @StringRes int title, @StringRes int message,
                                                        DialogInterface.OnClickListener positiveButtonListener) {
        return newInstance(c.getText(title), c.getText(message), positiveButtonListener);
    }

    public static SimpleAlertDialogFragment newInstance(CharSequence title, CharSequence message,
                                                        DialogInterface.OnClickListener positiveButtonListener) {
        SimpleAlertDialogFragment fragment = new SimpleAlertDialogFragment();
        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_MESSAGE, message);
        args.putBoolean(ARG_SHOW_POSITIVE_BUTTON, positiveButtonListener != null);
        fragment.setArguments(args);
        fragment.mPositiveButtonListener = positiveButtonListener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;

        mTitle = args.getCharSequence(ARG_TITLE, "title");
        mMessage = args.getCharSequence(ARG_MESSAGE, "message");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(mTitle)
                .setMessage(mMessage);

        if (getArguments() != null && getArguments().getBoolean(ARG_SHOW_POSITIVE_BUTTON, false)) {
            builder.setPositiveButton(R.string.ok, mPositiveButtonListener);
        } else {
            builder.setPositiveButton(R.string.ok, null);
        }

        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        Object parent = getParentFragment();
        if (parent == null)
            parent = requireActivity();

        if (parent instanceof OnDismissListener && getTag() != null)
            ((OnDismissListener) parent).onDialogDismissed(getTag());
    }

    public interface OnDismissListener {
        void onDialogDismissed(@NonNull String dialogTag);
    }
}