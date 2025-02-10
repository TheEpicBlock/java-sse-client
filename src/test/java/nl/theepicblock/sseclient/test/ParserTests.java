package nl.theepicblock.sseclient.test;

import nl.theepicblock.sseclient.SseClient;
import nl.theepicblock.sseclient.SseEvent;
import nl.theepicblock.sseclient.test.util.TestServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserTests {
    @Test
    public void spaceHandling() throws IOException {
        testParser("data: abcd\n\n", out -> {
            assertEquals(1, out.size());
            assertEquals(
                    new SseEvent("abcd", "", null),
                    out.get(0)
            );
        });
        testParser("data:abcd\n\n", out -> {
            assertEquals(1, out.size());
            assertEquals(
                    new SseEvent("abcd", "", null),
                    out.get(0)
            );
        });
        testParser("data:  abcd\n\n", out -> {
            assertEquals(1, out.size());
            assertEquals(
                    new SseEvent(" abcd", "", null),
                    out.get(0)
            );
        });
    }

    @Test
    public void multiData() throws IOException {
        testParser("data: abcd\ndata: efgh\n\n", out -> {
            assertEquals(1, out.size());
            assertEquals(
                    new SseEvent("abcd\nefgh", "", null),
                    out.get(0)
            );
        });
    }

    @Test
    public void noEmpty() throws IOException {
        // Needs to have an empty newline to trigger the event. No events should be triggered
        testParser("data: abcd\n", out -> {
            assertEquals(0, out.size());
        });
    }

    @Test
    public void noData() throws IOException {
        testParser("id: test\n\n", out -> {
            assertEquals(0, out.size());
        });
    }

    @Test
    public void doubleEvent() throws IOException {
        testParser("event: test\nevent: test2\ndata: abcd\n\n", out -> {
            assertEquals(1, out.size());
            assertEquals(
                    new SseEvent("abcd", "", "test2"),
                    out.get(0)
            );
        });
    }

    @Test
    public void persistentId() throws IOException {
        // The id field will persist until explicitly changed again
        testParser("id: testid\ndata: test1\n\ndata: test2\n\n", out -> {
            assertEquals(2, out.size());
            assertEquals(
                    new SseEvent("test1", "testid", null),
                    out.get(0)
            );
            assertEquals(
                    new SseEvent("test2", "testid", null),
                    out.get(1)
            );
        });
    }

    @Test
    public void nonPersistentEvent() throws IOException {
        // The event field is not persistent
        testParser("event: abcd\ndata: test1\n\ndata: test2\n\n", out -> {
            assertEquals(2, out.size());
            assertEquals(
                    new SseEvent("test1", "", "abcd"),
                    out.get(0)
            );
            assertEquals(
                    new SseEvent("test2", "", null),
                    out.get(1)
            );
        });
    }

    @Test
    public void unknownField() throws IOException {
        // Unknown fields should be ignored
        testParser("data: abcd\nbuttocks: test123\n\n", out -> {
            assertEquals(1, out.size());
            assertEquals(
                    new SseEvent("abcd", "", null),
                    out.get(0)
            );
        });
    }

    public void testParser(String input, Consumer<List<SseEvent>> checker) throws IOException {
        var events = new ArrayList<SseEvent>();
        AtomicBoolean disconnected = new AtomicBoolean(false);
        var server = new TestServer();
        var client = new SseClient() {
            @Override
            public void configureRequest(HttpRequest.Builder builder) {
                builder.uri(server.getUri());
            }

            @Override
            public void onEvent(SseEvent event) {
                events.add(event);
            }

            @Override
            public void onDisconnect() {
                disconnected.set(true);
            }
        };
        server.waitForConnection();
        server.sendRaw(input);
        server.close();
        while (!disconnected.get()) {}
        checker.accept(events);
    }
}
