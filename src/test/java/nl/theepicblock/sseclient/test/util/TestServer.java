package nl.theepicblock.sseclient.test.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestServer {
    private final HttpServer server;
    private final List<HttpExchange> listeners = new ArrayList<>();
    private final List<BufferedWriter> listenerWriters = new ArrayList<>();

    public TestServer() throws IOException {
        this(0); // Let the OS choose
    }

    public TestServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/sse/", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("charset", "utf-8");
            exchange.getResponseHeaders().add("access-control-allow-origin", "*");
            exchange.sendResponseHeaders(200, 0);
            listeners.add(exchange);
            listenerWriters.add(new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody(), StandardCharsets.UTF_8)));
        });
        this.server.start();
    }

    /**
     * Send a single event containing only a data field
     */
    public void sendData(String data) {
        sendRaw("data: "+data+"\n\n");
    }

    public void sendRaw(String data) {
        listenerWriters.forEach(w -> {
            try {
                w.write(data);
                w.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void waitForConnection() {
        while (listeners.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public URI getUri() {
        return URI.create("http://127.0.0.1:"+getPort()+"/sse/");
    }

    public void close() {
        listeners.forEach(HttpExchange::close);
        server.stop(0);
    }
}
