package com.aefyr.sai.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.signing.SigningSchemes;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;

public class SignatureSchemesDialogFragment extends BaseBottomSheetDialogFragment {

    public interface OnSchemesChangedListener {
        void onSchemesChanged(SigningSchemes schemes);
    }

    private CheckBox mV1;
    private CheckBox mV2;
    private CheckBox mV3;

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_signature_schemes, container, false);
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_main_signature_schemes);

        mV1 = view.findViewById(R.id.cb_scheme_v1);
        mV2 = view.findViewById(R.id.cb_scheme_v2);
        mV3 = view.findViewById(R.id.cb_scheme_v3);

        if (!SigningSchemes.isSupportedByThisDevice(SigningSchemes.SCHEME_V3)) {
            mV3.setEnabled(false);
            ((TextView) view.findViewById(R.id.tv_scheme_v3_summary))
                    .setText(R.string.signing_schemes_v3_unsupported);
        }

        apply(PreferencesHelper.getInstance(requireContext()).getSigningSchemes());

        getNegativeButton().setOnClickListener(v -> dismiss());
        getPositiveButton().setOnClickListener(v -> {
            SigningSchemes schemes = collect();
            PreferencesHelper.getInstance(requireContext()).setSigningSchemes(schemes);

            OnSchemesChangedListener listener = Utils.getParentAs(this, OnSchemesChangedListener.class);
            if (listener != null)
                listener.onSchemesChanged(schemes);

            dismiss();
        });

        revealBottomSheet();
    }

    private void apply(SigningSchemes schemes) {
        mV1.setChecked(schemes.has(SigningSchemes.SCHEME_V1));
        mV2.setChecked(schemes.has(SigningSchemes.SCHEME_V2));
        mV3.setChecked(mV3.isEnabled() && schemes.has(SigningSchemes.SCHEME_V3));
    }

    private SigningSchemes collect() {
        int flags = 0;
        if (mV1.isChecked())
            flags |= SigningSchemes.SCHEME_V1;
        if (mV2.isChecked())
            flags |= SigningSchemes.SCHEME_V2;
        if (mV3.isChecked())
            flags |= SigningSchemes.SCHEME_V3;

        return new SigningSchemes(flags);
    }
}
