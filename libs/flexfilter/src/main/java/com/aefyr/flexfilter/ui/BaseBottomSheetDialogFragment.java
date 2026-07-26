package com.aefyr.flexfilter.ui;

import com.aefyr.flexfilter.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetDialog mDialog;

    private Button mPositiveButton;
    private Button mNegativeButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //mDialog = new BottomSheetDialog(requireContext(), Theme.getInstance(requireContext()).getCurrentThemeDescriptor().isDark() ? R.style.SAIBottomSheetDialog_Backup : R.style.SAIBottomSheetDialog_Backup_Light);
        //TODO fix theme
        mDialog = new BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog);

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bottom_sheet_base, null);
        mPositiveButton = dialogView.findViewById(R.id.button_bottom_sheet_dialog_base_ok);
        mNegativeButton = dialogView.findViewById(R.id.button_bottom_sheet_dialog_base_cancel);
        mDialog.setContentView(dialogView);
        applyBottomInsets(dialogView);

        FrameLayout container = dialogView.findViewById(R.id.container_bottom_sheet_dialog_base_content);
        View contentView = onCreateContentView(LayoutInflater.from(requireContext()), container, savedInstanceState);
        if (contentView != null) {
            onContentViewCreated(contentView, savedInstanceState);
            container.addView(contentView);
        }

        return mDialog;
    }

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {

    }

    protected Button getPositiveButton() {
        return mPositiveButton;
    }

    protected Button getNegativeButton() {
        return mNegativeButton;
    }

    public void setTitle(@StringRes int title) {
        setTitle(getString(title));
    }

    public void setTitle(CharSequence title) {
        TextView titleView = mDialog.findViewById(R.id.tv_bottom_sheet_dialog_base_title);
        if (titleView != null)
            titleView.setText(title);
    }

    public void revealBottomSheet() {
        FrameLayout bottomSheet = mDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null)
            return;

        BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        Object parent = getParentFragment();
        if (parent == null)
            parent = requireActivity();

        if (parent instanceof OnDismissListener && getTag() != null)
            ((OnDismissListener) parent).onDismiss(getTag());

    }

    public interface OnDismissListener {

        void onDismiss(@NonNull String tag);

    }

    /**
     * Apps targeting Android 15 are laid out edge to edge, so the sheet has to inset itself out
     * from under the navigation bar and the keyboard.
     */
    private static void applyBottomInsets(View view) {
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            int bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    bottom + Math.max(bars, keyboard));
            return windowInsets;
        });
    }
}
