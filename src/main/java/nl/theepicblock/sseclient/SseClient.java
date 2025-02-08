package nl.theepicblock.sseclient;

import org.jspecify.annotations.NonNull;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Flow;

public abstract class SseClient {
    @NonNull
    private HttpClient client;
    @NonNull
    private HttpRequest targetUrl;
    /**
     * Set to true to emit events with empty data
     */
    public boolean emitEmpty;
    public Long retryDelay;

    /**
     * lastEventId, is persisted across connections
     * @see <a href="https://www.w3.org/TR/2012/WD-eventsource-20120426/#concept-event-stream-last-event-id">The spec</a>
     */
    private @NonNull String id = "";

    public SseClient(@NonNull HttpRequest targetUrl) {
        this(targetUrl, HttpClient.newHttpClient());
    }

    public SseClient(@NonNull HttpRequest targetUrl, @NonNull HttpClient client) {
        this.targetUrl = targetUrl;
        this.client = client;

        connect();
    }

    public abstract void onEvent(SseEvent event);

    public void onDisconnect() {

    };

    public void onConnect() {

    }

    private void connect() {
        var sub = new SseBodyHandler();
        var response = this.client.sendAsync(
                this.targetUrl,
                HttpResponse.BodyHandlers.fromLineSubscriber(sub)
        );
        response.exceptionally((a) -> {
            onDisconnect();
            return null;
        });
        response.thenAccept(e -> {
            onDisconnect();
        });
    }

    private boolean isValidResponse(HttpResponse.ResponseInfo info) {
        return true;
    }

    private class SseBodyHandler implements Flow.Subscriber<String> {
        private StringBuilder data = null;
        private String event = null;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            onConnect();
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String line) {
            if (line.isEmpty()) {
                // Dispatch the event
                if (emitEmpty || data.length() != 0) {
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
                        try { retryDelay = Long.parseLong(value); } catch (NumberFormatException ignored) {}
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
    }

}
