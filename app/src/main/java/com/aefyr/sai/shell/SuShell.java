package com.aefyr.sai.shell;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;

public class SuShell implements Shell {
    private static final String TAG = "SuShell";

    private static SuShell sInstance;

    private final PersistentShellSession mSession =
            new PersistentShellSession(TAG, () -> Runtime.getRuntime().exec(new String[]{"su"}));

    public static SuShell getInstance() {
        synchronized (SuShell.class) {
            if (sInstance == null)
                sInstance = new SuShell();

            return sInstance;
        }
    }

    private SuShell() {
    }

    public boolean requestRoot() {
        // Verifying uid also proves the shell really is root, not just that su exists.
        return mSession.ensureStarted(session -> session.exec(new Command("id", "-u")).out.trim().equals("0"));
    }

    @Override
    public boolean isAvailable() {
        return !requestRoot();
    }

    @Override
    public Result exec(Command command) {
        return execInternal(command, null);
    }

    @Override
    public Result exec(Command command, InputStream inputPipe) {
        return execInternal(command, inputPipe);
    }

    @Override
    public String makeLiteral(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private Result execInternal(Command command, @Nullable InputStream inputPipe) {
        if (inputPipe != null)
            return execWithStdin(command, inputPipe);

        if (!requestRoot())
            return new Result(command, -1, "", "<!> SAI SuShell: unable to start su session");

        try {
            return mSession.exec(command);
        } catch (Exception e) {
            Log.w(TAG, "Session command failed, dropping session", e);
            mSession.close();
            return new Result(command, -1, "", "<!> SAI SuShell Java exception: " + Utils.throwableToString(e));
        }
    }

    /**
     * stdin is reserved for the session's command stream, so piping data needs its own process.
     */
    private Result execWithStdin(Command command, InputStream inputPipe) {
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command.toString()});

            Thread stdOutD = IOUtils.writeStreamToStringBuilder(stdOutSb, process.getInputStream());
            Thread stdErrD = IOUtils.writeStreamToStringBuilder(stdErrSb, process.getErrorStream());

            try (OutputStream outputStream = process.getOutputStream(); InputStream inputStream = inputPipe) {
                IOUtils.copyStream(inputStream, outputStream);
            } catch (Exception e) {
                stdOutD.interrupt();
                stdErrD.interrupt();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    process.destroyForcibly();
                else
                    process.destroy();
                throw new RuntimeException(e);
            }

            process.waitFor();
            stdOutD.join();
            stdErrD.join();

            return new Result(command, process.exitValue(), stdOutSb.toString().trim(), stdErrSb.toString().trim());
        } catch (Exception e) {
            Log.w(TAG, "Unable to execute command", e);
            return new Result(command, -1, stdOutSb.toString().trim(),
                    stdErrSb + "\n\n<!> SAI SuShell Java exception: " + Utils.throwableToString(e));
        }
    }
}
