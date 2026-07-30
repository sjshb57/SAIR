package com.aefyr.sai.ui.dialogs;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.signing.SigningKey;
import com.aefyr.sai.signing.SigningKeyManager;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SigningKeyDialogFragment extends BaseBottomSheetDialogFragment {

    private static final String TAG = "SigningKeyDialog";

    public interface OnSigningKeyChangedListener {
        void onSigningKeyChanged();
    }

    /** RSA-2048 generation and keystore parsing are slow enough to stutter the UI thread. */
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private TextView mFingerprint;
    private TextInputEditText mPassword;
    private View mContent;

    private ActivityResultLauncher<String[]> mPickKeyStore;
    private ActivityResultLauncher<String[]> mPickPrivateKey;
    private ActivityResultLauncher<String[]> mPickCertificate;

    private byte[] mPendingPrivateKey;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPickKeyStore = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            byte[] bytes = read(uri);
            if (bytes == null)
                return;

            char[] password = password();
            runInBackground(() -> SigningKeyManager.getInstance(requireContext())
                    .importKeyStore(bytes, password));
        });

        mPickPrivateKey = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            mPendingPrivateKey = read(uri);
            if (mPendingPrivateKey == null)
                return;

            Toast.makeText(requireContext(), R.string.signing_key_pick_certificate, Toast.LENGTH_SHORT).show();
            mPickCertificate.launch(new String[]{"*/*"});
        });

        mPickCertificate = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            byte[] key = mPendingPrivateKey;
            mPendingPrivateKey = null;

            byte[] cert = read(uri);
            if (key == null || cert == null)
                return;

            char[] password = password();
            runInBackground(() -> SigningKeyManager.getInstance(requireContext())
                    .importKeyAndCertificate(key, cert, password));
        });
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_signing_key, container, false);
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_main_signing_key);

        mContent = view;
        mFingerprint = view.findViewById(R.id.tv_signing_key_fingerprint);
        mPassword = view.findViewById(R.id.et_signing_key_password);

        updateFingerprint();

        view.findViewById(R.id.button_signing_key_import_keystore)
                .setOnClickListener(v -> mPickKeyStore.launch(new String[]{"*/*"}));

        view.findViewById(R.id.button_signing_key_import_pair)
                .setOnClickListener(v -> mPickPrivateKey.launch(new String[]{"*/*"}));

        view.findViewById(R.id.button_signing_key_regenerate).setOnClickListener(v ->
                runInBackground(() -> SigningKeyManager.getInstance(requireContext()).generate()));

        getPositiveButton().setVisibility(View.GONE);
        getNegativeButton().setOnClickListener(v -> dismiss());

        revealBottomSheet();
    }

    private interface KeyOperation {
        void run() throws Exception;
    }

    private void runInBackground(KeyOperation operation) {
        setBusy(true);

        mExecutor.execute(() -> {
            Exception error = null;
            try {
                operation.run();
            } catch (Exception e) {
                error = e;
            }

            final Exception finalError = error;
            mHandler.post(() -> {
                if (!isAdded())
                    return;

                setBusy(false);

                if (finalError != null) {
                    showError(finalError);
                    return;
                }

                Toast.makeText(requireContext(), R.string.signing_key_updated, Toast.LENGTH_SHORT).show();
                updateFingerprint();
                notifyChanged();
            });
        });
    }

    private void setBusy(boolean busy) {
        if (mContent == null)
            return;

        mContent.findViewById(R.id.button_signing_key_import_keystore).setEnabled(!busy);
        mContent.findViewById(R.id.button_signing_key_import_pair).setEnabled(!busy);
        mContent.findViewById(R.id.button_signing_key_regenerate).setEnabled(!busy);
    }

    private char[] password() {
        CharSequence typed = mPassword.getText();
        return (typed == null ? "" : typed.toString()).toCharArray();
    }

    @Nullable
    private byte[] read(@Nullable Uri uri) {
        if (uri == null)
            return null;

        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null)
                throw new IllegalStateException("Unable to open the selected file");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copyStream(in, out);
            return out.toByteArray();
        } catch (Exception e) {
            showError(e);
            return null;
        }
    }

    private void updateFingerprint() {
        try {
            SigningKey key = SigningKeyManager.getInstance(requireContext()).get();
            mFingerprint.setText(key != null ? key.certificateSha256() : getString(R.string.signing_key_none));
        } catch (Exception e) {
            showError(e);
        }
    }

    private void notifyChanged() {
        OnSigningKeyChangedListener listener = Utils.getParentAs(this, OnSigningKeyChangedListener.class);
        if (listener != null)
            listener.onSigningKeyChanged();
    }

    private void showError(Exception e) {
        Log.w(TAG, "Signing key operation failed", e);

        String message = e.getLocalizedMessage();
        if (TextUtils.isEmpty(message))
            message = e.getClass().getSimpleName();

        mFingerprint.setText(getString(R.string.signing_key_error, message));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }
}
