package nl.theepicblock.sseclient;

public class SseEvent {
    public final String data;
    public final String id;
    public final String event;

    public SseEvent(String data, String id, String event) {
        this.data = data;
        this.id = id;
        this.event = event;
    }
}
