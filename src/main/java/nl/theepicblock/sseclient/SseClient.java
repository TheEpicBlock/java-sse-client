package nl.theepicblock.sseclient;

import org.jspecify.annotations.NonNull;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateExpiredException;
import java.util.concurrent.Flow;

public class SseClient {
    @NonNull
    private SseListener listener;
    @NonNull
    private HttpClient client;
    @NonNull
    private HttpRequest targetUrl;

    public SseClient(@NonNull SseListener listener, @NonNull HttpRequest targetUrl) {
        this(listener, targetUrl, HttpClient.newHttpClient());
    }

    public SseClient(@NonNull SseListener listener, @NonNull HttpRequest targetUrl, @NonNull HttpClient client) {
        this.listener = listener;
        this.targetUrl = targetUrl;
        this.client = client;

        connect();
    }

    private void connect() {
        var sub = new SseBodyHandler();
        var response = this.client.sendAsync(
                this.targetUrl,
                HttpResponse.BodyHandlers.fromLineSubscriber(sub)
        );
        response.exceptionally((a) -> {
            // TODO
            return null;
        });
        response.thenAccept(e -> {
            // TODO
        });
    }

    private boolean isValidResponse(HttpResponse.ResponseInfo info) {
        return true;
    }

    private class SseBodyHandler implements Flow.Subscriber<String> {
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String item) {
            listener.onEvent(new SseEvent(
                    item,
                    null,
                    null
            ));
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
