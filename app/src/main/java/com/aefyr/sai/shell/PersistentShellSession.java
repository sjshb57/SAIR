package com.aefyr.sai.shell;

import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * A long-lived shell that commands are fed into, so a shell is spawned once instead of per command
 * - a su cold start or a Shizuku process is expensive.
 * <p>
 * Commands that pipe data through stdin cannot use this, since stdin carries the command stream.
 */
class PersistentShellSession {

    /** Echoed after every command to mark where its output ends and carry the exit code. */
    private static final String MARKER = "__SAI_CMD_DONE__";

    private final String mTag;
    private final ProcessFactory mProcessFactory;

    private Process mProcess;
    private Writer mIn;
    private BufferedReader mOut;
    private BufferedReader mErr;

    interface ProcessFactory {
        Process start() throws Exception;
    }

    PersistentShellSession(String tag, ProcessFactory processFactory) {
        mTag = tag;
        mProcessFactory = processFactory;
    }

    /** @param validator run once after the shell starts; the session is discarded if it fails */
    synchronized boolean ensureStarted(@Nullable Validator validator) {
        if (mProcess != null && isAlive())
            return true;

        close();

        try {
            mProcess = mProcessFactory.start();
            mIn = new OutputStreamWriter(mProcess.getOutputStream(), StandardCharsets.UTF_8);
            mOut = new BufferedReader(new InputStreamReader(mProcess.getInputStream(), StandardCharsets.UTF_8));
            mErr = new BufferedReader(new InputStreamReader(mProcess.getErrorStream(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(mTag, "Unable to start shell session", e);
            close();
            return false;
        }

        if (validator != null) {
            try {
                if (!validator.isValid(this)) {
                    close();
                    return false;
                }
            } catch (Exception e) {
                Log.w(mTag, "Shell session validation failed", e);
                close();
                return false;
            }
        }

        return true;
    }

    synchronized Shell.Result exec(Shell.Command command) throws IOException {
        mIn.write(command.toString());
        mIn.write("\n");
        mIn.write("echo " + MARKER + " $?\n");
        mIn.flush();

        StringBuilder out = new StringBuilder();
        int exitCode = -1;
        String line;
        while ((line = mOut.readLine()) != null) {
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
            throw new IOException("Shell session closed unexpectedly");

        return new Shell.Result(command, exitCode, out.toString().trim(), drainStderr());
    }

    private String drainStderr() {
        StringBuilder err = new StringBuilder();
        try {
            while (mErr.ready()) {
                String line = mErr.readLine();
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

    private boolean isAlive() {
        try {
            mProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    synchronized void close() {
        IOUtils.closeSilently(mIn);
        IOUtils.closeSilently(mOut);
        IOUtils.closeSilently(mErr);

        if (mProcess != null)
            mProcess.destroy();

        mIn = null;
        mOut = null;
        mErr = null;
        mProcess = null;
    }

    interface Validator {
        boolean isValid(PersistentShellSession session) throws Exception;
    }
}
