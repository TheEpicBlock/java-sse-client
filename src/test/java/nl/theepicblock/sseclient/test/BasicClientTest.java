package nl.theepicblock.sseclient.test;

import nl.theepicblock.sseclient.SseClient;
import nl.theepicblock.sseclient.SseEvent;
import nl.theepicblock.sseclient.test.util.Channel;
import nl.theepicblock.sseclient.test.util.TestServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

public class BasicClientTest {
    @Test
    public void basicTest() throws IOException {
        var server = new TestServer();

        var eventChannel = new Channel<SseEvent>();
        var client = new SseClient() {
            @Override
            public void configureRequest(HttpRequest.Builder builder) {
                builder.uri(server.getUri());
            }

            @Override
            public void onEvent(SseEvent event) {
                eventChannel.push(event);
            }
        };

        server.waitForConnection();
        server.sendData("test123\n");
        var n = eventChannel.waitForNext();
        Assertions.assertEquals("test123", n.data);
    }

    @Test
    public void callbackOrder() throws IOException, InterruptedException {
        var server = new TestServer();

        var callbacks = new ArrayList<String>();
        var client = new SseClient() {
            @Override
            public void configureRequest(HttpRequest.Builder builder) {
                builder.uri(server.getUri());
            }

            @Override
            public void onEvent(SseEvent event) {
                callbacks.add("event");
            }

            @Override
            public void onDisconnect() {
                callbacks.add("disconnect");
            }

            @Override
            public void onConnect() {
                callbacks.add("connect");
            }
        };

        server.waitForConnection();
        server.sendData("test123\n");
        server.sendData("abcd\n");

        server.close();
        Thread.sleep(500);

        Assertions.assertIterableEquals(
                List.of("connect", "event", "disconnect"),
                callbacks
        );
    }
}
