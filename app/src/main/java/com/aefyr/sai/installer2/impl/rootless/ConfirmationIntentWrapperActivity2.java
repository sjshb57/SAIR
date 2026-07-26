package com.aefyr.sai.installer2.impl.rootless;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aefyr.sai.utils.Logs;
import androidx.core.content.IntentCompat;

public class ConfirmationIntentWrapperActivity2 extends AppCompatActivity {

    private static final String EXTRA_CONFIRMATION_INTENT = "confirmation_intent";
    public static final String EXTRA_SESSION_ID = "session_id";

    private boolean mFinishedProperly = false;

    private final ActivityResultLauncher<Intent> mConfirmationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                mFinishedProperly = true;
                finish();
            });

    private int mSessionId;
    private Intent mConfirmationIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        mSessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1);
        mConfirmationIntent = IntentCompat.getParcelableExtra(intent, EXTRA_CONFIRMATION_INTENT, Intent.class);

        if (savedInstanceState == null) {
            try {
                mConfirmationLauncher.launch(mConfirmationIntent);
            } catch (Exception e) {
                Logs.logException(e);
                sendErrorBroadcast(mSessionId, RootlessSaiPiBroadcastReceiver.STATUS_BAD_ROM);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing() && !mFinishedProperly)
            start(this, mSessionId, mConfirmationIntent); //Because if user doesn't confirm/cancel the installation, PackageInstaller session will hang

    }

    public static void start(Context c, int sessionId, Intent confirmationIntent) {
        Intent intent = new Intent(c, ConfirmationIntentWrapperActivity2.class);
        intent.putExtra(EXTRA_CONFIRMATION_INTENT, confirmationIntent);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        c.startActivity(intent);
    }

    private void sendErrorBroadcast(int sessionID, int status) {
        Intent statusIntent = new Intent(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT);
        // The receiver is registered as not-exported, so the broadcast has to be explicit.
        statusIntent.setPackage(getPackageName());
        statusIntent.putExtra(PackageInstaller.EXTRA_STATUS, status);
        statusIntent.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionID);

        sendBroadcast(statusIntent);
    }

}
