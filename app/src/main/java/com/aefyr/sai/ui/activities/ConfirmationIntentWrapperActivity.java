package com.aefyr.sai.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aefyr.sai.R;
import com.aefyr.sai.installer.rootless.RootlessSAIPIService;
import com.aefyr.sai.utils.Logs;
import androidx.core.content.IntentCompat;

public class ConfirmationIntentWrapperActivity extends AppCompatActivity {

    private static final String EXTRA_CONFIRMATION_INTENT = "confirmation_intent";

    /**
     * Used to send abort event when this activity is force closed due to MainActivity being started from launcher/open with.
     * I'm not really sure why this works (Why isn't onActivityResult called on force close, also why this activity even gets destroyed when MainActivity is started), but if it doesn't, the only bad thing that will happen is the thing this prevents, so that's fine.
     */
    private boolean mFinishedProperly = false;

    private final ActivityResultLauncher<Intent> mConfirmationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                mFinishedProperly = true;
                finish();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Intent confirmationIntent = IntentCompat.getParcelableExtra(intent, EXTRA_CONFIRMATION_INTENT, Intent.class);
        try {
            mConfirmationLauncher.launch(confirmationIntent);
        } catch (Exception e) {
            Logs.logException(e);
            sendErrorBroadcast(intent.getIntExtra(RootlessSAIPIService.EXTRA_SESSION_ID, -1), getString(R.string.installer_error_lidl_rom));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!mFinishedProperly) {
            Intent intent = getIntent();
            sendErrorBroadcast(intent.getIntExtra(RootlessSAIPIService.EXTRA_SESSION_ID, -1), getString(R.string.installer_error_aborted));
        }
    }

    public static void start(Context c, int sessionId, Intent confirmationIntent) {
        Intent intent = new Intent(c, ConfirmationIntentWrapperActivity.class);
        intent.putExtra(EXTRA_CONFIRMATION_INTENT, confirmationIntent);
        intent.putExtra(RootlessSAIPIService.EXTRA_SESSION_ID, sessionId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        c.startActivity(intent);
    }

    private void sendErrorBroadcast(int sessionID, String error) {
        Intent statusIntent = new Intent(RootlessSAIPIService.ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(RootlessSAIPIService.EXTRA_INSTALLATION_STATUS, RootlessSAIPIService.STATUS_FAILURE);
        statusIntent.putExtra(RootlessSAIPIService.EXTRA_SESSION_ID, sessionID);
        statusIntent.putExtra(RootlessSAIPIService.EXTRA_ERROR_DESCRIPTION, error);

        sendBroadcast(statusIntent);
    }

}
