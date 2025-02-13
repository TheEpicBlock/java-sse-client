package nl.theepicblock.sseclient;

import org.jspecify.annotations.Nullable;

public class ReconnectionInfo {
    private final boolean connectionFailed;
    private final boolean wasInitial;
    private final @Nullable Integer statusCode;
    private final boolean wasInvalid;
    private final @Nullable Throwable error;

    ReconnectionInfo(boolean connectionFailed, boolean wasInitial, @Nullable Integer statusCode, boolean wasInvalid, @Nullable Throwable error) {
        this.connectionFailed = connectionFailed;
        this.wasInitial = wasInitial;
        this.statusCode = statusCode;
        this.wasInvalid = wasInvalid;
        this.error = error;
    }

    /**
     * True if the connection failed to even establish. This might indicate some error with the server.
     * If it's false, it means that a connection was established, but it just broke down at some point.
     * This is not necessarily bad.
     */
    public boolean connectionFailed() {
        return this.connectionFailed;
    }

    public boolean wasInitialConnection() {
        return wasInitial;
    }

    public boolean firstConnectionFailed() {
        return wasInitialConnection() && connectionFailed();
    }

    public @Nullable Integer statusCode() {
        return statusCode;
    }

    public boolean wasConnectionInvalid() {
        return wasInvalid;
    }

    public @Nullable Throwable error() {
        return error;
    }
}
