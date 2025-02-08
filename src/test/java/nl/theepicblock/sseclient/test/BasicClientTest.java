package nl.theepicblock.sseclient.test;

import nl.theepicblock.sseclient.SseClient;
import nl.theepicblock.sseclient.SseEvent;
import nl.theepicblock.sseclient.SseListener;
import nl.theepicblock.sseclient.test.util.Channel;
import nl.theepicblock.sseclient.test.util.TestServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

public class BasicClientTest {
    @Test
    public void basicTest() throws IOException {
        var server = new TestServer();

        var eventChannel = new Channel<SseEvent>();
        var client = new SseClient(HttpRequest.newBuilder().GET().uri(server.getUri()).timeout(Duration.ofSeconds(50)).build()) {
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
}
