package nl.theepicblock.sseclient;

import org.jspecify.annotations.Nullable;

public class ReconnectionInfo {
    private final boolean connectionFailed;
    private final boolean wasInitial;
    private final @Nullable Integer statusCode;
    private final boolean wasInvalid;
    private final @Nullable Throwable error;
    private final int numberOfRetries;

    ReconnectionInfo(boolean connectionFailed, boolean wasInitial, @Nullable Integer statusCode, boolean wasInvalid, @Nullable Throwable error, int numberOfRetries) {
        this.connectionFailed = connectionFailed;
        this.wasInitial = wasInitial;
        this.statusCode = statusCode;
        this.wasInvalid = wasInvalid;
        this.error = error;
        this.numberOfRetries = numberOfRetries;
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

    /**
     * Which retry we're on. Always ≥1
     */
    public int numberOfRetries() {
        return numberOfRetries;
    }
}
