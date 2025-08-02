package nl.theepicblock.sseclient.test;

import nl.theepicblock.sseclient.SseClient;
import nl.theepicblock.sseclient.SseEvent;
import nl.theepicblock.sseclient.test.util.Channel;
import nl.theepicblock.sseclient.test.util.TestServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

public class TimeoutTest {
    @Test
    @Disabled("This test takes 120 seconds to wait for the timeout. Uncomment this at your own peril")
    @Timeout(value = 150, unit = TimeUnit.SECONDS)
    public void donttimeoutpls() throws IOException, InterruptedException {
        var testStart = System.currentTimeMillis();
        var server = new TestServer();

        var eventChannel = new Channel<SseEvent>();
        final long[] disconnectTime = { -1L };
        var client = new SseClient() {
            {
                retryDelayMillis = 10L;
                this.connect();
            }

            @Override
            public void configureRequest(HttpRequest.Builder builder) {
                builder.uri(server.getUri());
            }

            @Override
            public void onEvent(SseEvent event) {
                eventChannel.push(event);
            }

            @Override
            public void onDisconnect() {
                disconnectTime[0] = System.currentTimeMillis();
            }
        };

        server.waitForConnection();

        Thread.sleep(120_000);

        if (disconnectTime[0] != -1L) {
            throw new RuntimeException("Client disconnected after "+(disconnectTime[0]-testStart)/1000+" seconds");
        }

        server.sendData("Test");
        Assertions.assertEquals("Test", eventChannel.waitForNext().data);

        server.close();
        client.close();
    }
}
