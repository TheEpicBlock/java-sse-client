package nl.theepicblock.sseclient;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public abstract class SseClient {
    @NonNull
    private final HttpClient client;
    protected volatile SseBodyHandler currentHandler;
    /**
     * Set to true to emit events with empty data
     */
    public boolean emitEmpty;
    /**
     * The retry delay advised by the server. Will be null if nothing was sent.
     */
    public Long retryDelayMillis = null;

    /**
     * lastEventId, is persisted across connections
     * @see <a href="https://www.w3.org/TR/2012/WD-eventsource-20120426/#concept-event-stream-last-event-id">The spec</a>
     */
    private @NonNull String id = "";

    public SseClient() {
        this(HttpClient.newHttpClient());
    }

    public SseClient(@NonNull HttpClient client) {
        this.client = client;

        connect();
    }

    public abstract void onEvent(SseEvent event);

    /**
     * Called when the client attempts to reconnect.
     * This might be after the connection disconnected. Or it might be after a
     * connection was attempted and failed.
     * <p>
     * See {@link #retryDelayMillis}, which stores the retry delay advised by the server. You should
     * also provide a default reconnection time if the server didn't provide one.
     * </p>
     * @return The time to wait until a new connection is attempted. Or null if the client should stop.
     */
    protected @Nullable Duration onReconnect(@NonNull ReconnectionInfo reconnectionInfo) {
        if (reconnectionInfo.connectionFailed()) {
            return null;
        } else {
            return Duration.ofMillis(retryDelayMillis == null ? 5000 : retryDelayMillis);
        }
    }

    public void onDisconnect() {

    };

    public void onConnect() {

    }

    public abstract void configureRequest(HttpRequest.Builder builder);

    private HttpRequest createRequest() {
        var b = HttpRequest.newBuilder();
        b.GET();
        b.setHeader("Accept", "text/event-stream");
        b.setHeader("Cache-Control", "no-cache");
        if (!id.isEmpty()) {
            b.setHeader("Last-Event-ID", id);
        }
        configureRequest(b);
        return b.build();
    }

    private void attemptReconnect(ReconnectionInfo info) {
        this.currentHandler.cancel();
        var timeout = onReconnect(info);
        if (timeout == null) return;
        submitTaskDelayed(timeout, this::connect);
    }

    private void submitTaskDelayed(@NonNull Duration delay, @NonNull Runnable task) {
        var e = client.executor().orElse(null);
        var de = e == null ?
                CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS) :
                CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS, e);
        de.execute(task);
    }

    private void connect() {
        var isInitial = this.currentHandler == null;
        var sub = new SseBodyHandler();
        var response = this.client.sendAsync(
                this.createRequest(),
                (e) -> {
                    if (isValidResponse(e.statusCode(), e.headers())) {
                        this.currentHandler = sub;
                        return HttpResponse.BodySubscribers.fromLineSubscriber(sub);
                    } else {
                        return HttpResponse.BodySubscribers.discarding();
                    }
                }
        );
        response.exceptionally((err) -> {
            onDisconnect();
            attemptReconnect(new ReconnectionInfo(
                    true,
                    isInitial,
                    null,
                    false,
                    err
            ));
            return null;
        });
        response.thenAccept(e -> {
            onDisconnect();
            attemptReconnect(new ReconnectionInfo(
                    false,
                    isInitial,
                    e.statusCode(),
                    isValidResponse(e.statusCode(), e.headers()),
                    null
            ));
        });
    }

    private boolean isValidResponse(int statusCode, HttpHeaders headers) {
        var contentType = headers.firstValue("Content-Type");
        if (contentType.isEmpty()) {
            return false;
        }
        if (!contentType.orElseThrow().startsWith("text/event-stream")) {
            return false;
        }
        if (statusCode < 200 || statusCode > 299) {
            return false;
        }
        return true;
    }

    protected class SseBodyHandler implements Flow.Subscriber<String> {
        private StringBuilder data = null;
        private String event = null;
        private Flow.Subscription subscription;
        private volatile boolean cancelled = false;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            onConnect();
            subscription.request(Long.MAX_VALUE);
            this.subscription = subscription;
        }

        @Override
        public void onNext(String line) {
            if (cancelled) {
                throw new IllegalStateException();
            }
            if (line.isEmpty()) {
                // Dispatch the event
                if (emitEmpty || (data != null && data.length() != 0)) {
                    onEvent(new SseEvent(
                            data == null ? null : data.toString(),
                            id,
                            event
                    ));
                }
                data = null;
                event = null;
            } else {
                var colonIndex = line.indexOf(':');

                String field;
                String value;
                if (colonIndex == -1) {
                    // the line is not empty but does not contain a U+003A COLON character (:)
                    // According to spec we must interpret the line as a field, and leave the value empty
                    field = line;
                    value = "";
                } else {
                    // the line contains a U+003A COLON character
                    field = line.substring(0, colonIndex);
                    var valueStart = colonIndex + 1;
                    if (valueStart < line.length() && line.charAt(valueStart) == ' ') valueStart++;
                    value = line.substring(valueStart);
                }

                // Process the field/value pair
                switch (field) {
                    case "event":
                        event = value;
                        break;
                    case "data":
                        if (data == null) data = new StringBuilder();
                        if (data.length() != 0) data.append("\n");
                        data.append(value);
                        break;
                    case "id":
                        id = value;
                        break;
                    case "retry":
                        try { retryDelayMillis = Long.parseLong(value); } catch (NumberFormatException ignored) {}
                        break;
                    // Ignore any other values
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }

        void cancel() {
            this.cancelled = true;
            if (this.subscription != null) this.subscription.cancel();
        }
    }
}
