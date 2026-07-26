package com.aefyr.sai.shell;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ShizukuShell implements Shell {
    private static final String TAG = "ShizukuShell";

    private static ShizukuShell sInstance;

    private final PersistentShellSession mSession =
            new PersistentShellSession(TAG, () -> newRemoteProcess(new String[]{"sh"}));

    public static ShizukuShell getInstance() {
        synchronized (ShizukuShell.class) {
            if (sInstance == null)
                sInstance = new ShizukuShell();

            return sInstance;
        }
    }

    private ShizukuShell() {
    }

    @Override
    public boolean isAvailable() {
        if (!Shizuku.pingBinder())
            return false;

        return mSession.ensureStarted(session -> session.exec(new Command("echo", "test")).isSuccessful());
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

        if (!isAvailable())
            return new Result(command, -1, "", "<!> SAI ShizukuShell: unable to start shell session");

        try {
            return mSession.exec(command);
        } catch (Exception e) {
            Log.w(TAG, "Session command failed, dropping session", e);
            mSession.close();
            return new Result(command, -1, "", "<!> SAI ShizukuShell Java exception: " + Utils.throwableToString(e));
        }
    }

    /**
     * stdin is reserved for the session's command stream, so piping data needs its own process.
     */
    private Result execWithStdin(Command command, InputStream inputPipe) {
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();

        try {
            ShizukuRemoteProcess process = newRemoteProcess(new String[]{"sh", "-c", command.toString()});

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
                    stdErrSb + "\n\n<!> SAI ShizukuShell Java exception: " + Utils.throwableToString(e));
        }
    }

    /**
     * Shizuku.newProcess is hidden from the public API surface, so it has to be reached reflectively.
     */
    private static ShizukuRemoteProcess newRemoteProcess(String[] cmd) throws Exception {
        Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        method.setAccessible(true);
        return (ShizukuRemoteProcess) method.invoke(null, cmd, null, null);
    }
}
