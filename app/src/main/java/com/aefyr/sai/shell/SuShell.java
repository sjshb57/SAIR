package com.aefyr.sai.shell;

import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class SuShell implements Shell {
    private static final String TAG = "SuShell";

    /**
     * Printed by the session shell after every command so we know where its output ends and can
     * recover the exit code without spawning a new su process per command.
     */
    private static final String MARKER = "__SAI_CMD_DONE__";

    private static SuShell sInstance;

    private final Object mLock = new Object();

    private Process mSession;
    private Writer mSessionIn;
    private BufferedReader mSessionOut;
    private BufferedReader mSessionErr;

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
        synchronized (mLock) {
            try {
                return ensureSession();
            } catch (Exception e) {
                Log.w(TAG, "Unable to acquire root access", e);
                return false;
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return requestRoot();
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

    /**
     * Commands that pipe data through stdin cannot share the session shell, since its stdin is
     * reserved for the command stream. Those get a dedicated su process; everything else reuses
     * the long-lived session, which avoids a su cold start (often 100-500ms on Magisk) per command.
     */
    private Result execInternal(Command command, @Nullable InputStream inputPipe) {
        if (inputPipe != null)
            return execWithStdin(command, inputPipe);

        synchronized (mLock) {
            try {
                if (!ensureSession())
                    return new Result(command, -1, "", "<!> SAI SuShell: unable to start su session");

                return execInSession(command);
            } catch (Exception e) {
                Log.w(TAG, "Session command failed, dropping session", e);
                closeSession();
                return new Result(command, -1, "", "<!> SAI SuShell Java exception: " + Utils.throwableToString(e));
            }
        }
    }

    private boolean ensureSession() throws IOException {
        if (mSession != null && isSessionAlive())
            return true;

        closeSession();

        mSession = Runtime.getRuntime().exec(new String[]{"su"});
        mSessionIn = new OutputStreamWriter(mSession.getOutputStream(), StandardCharsets.UTF_8);
        mSessionOut = new BufferedReader(new InputStreamReader(mSession.getInputStream(), StandardCharsets.UTF_8));
        mSessionErr = new BufferedReader(new InputStreamReader(mSession.getErrorStream(), StandardCharsets.UTF_8));

        Result probe = execInSession(new Command("id", "-u"));
        if (!probe.isSuccessful() || !probe.out.trim().equals("0")) {
            Log.w(TAG, "su session did not yield uid 0: " + probe.out);
            closeSession();
            return false;
        }

        return true;
    }

    private boolean isSessionAlive() {
        try {
            mSession.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    private Result execInSession(Command command) throws IOException {
        mSessionIn.write(command.toString());
        mSessionIn.write("\n");
        mSessionIn.write("echo " + MARKER + " $?\n");
        mSessionIn.flush();

        StringBuilder out = new StringBuilder();
        int exitCode = -1;
        String line;
        while ((line = mSessionOut.readLine()) != null) {
            if (line.startsWith(MARKER)) {
                try {
                    exitCode = Integer.parseInt(line.substring(MARKER.length()).trim());
                } catch (NumberFormatException ignored) {
                }
                break;
            }
            if (out.length() > 0)
                out.append('\n');
            out.append(line);
        }

        if (line == null)
            throw new IOException("su session closed unexpectedly");

        return new Result(command, exitCode, out.toString().trim(), drainStderr());
    }

    private String drainStderr() {
        StringBuilder err = new StringBuilder();
        try {
            while (mSessionErr.ready()) {
                String line = mSessionErr.readLine();
                if (line == null)
                    break;
                if (err.length() > 0)
                    err.append('\n');
                err.append(line);
            }
        } catch (IOException ignored) {
        }
        return err.toString().trim();
    }

    private void closeSession() {
        IOUtils.closeSilently(mSessionIn);
        IOUtils.closeSilently(mSessionOut);
        IOUtils.closeSilently(mSessionErr);

        if (mSession != null)
            mSession.destroy();

        mSessionIn = null;
        mSessionOut = null;
        mSessionErr = null;
        mSession = null;
    }

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
                process.destroyForcibly();
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
