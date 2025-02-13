package nl.theepicblock.sseclient;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class SseEvent {
    /**
     * Can only be null if {@link SseClient#emitEmpty} is set to true
     */
    public final String data;
    @NonNull
    public final String id;
    public final String event;

    public SseEvent(String data, String id, String event) {
        this.data = data;
        this.id = id;
        this.event = event;
    }

    @Override
    public String toString() {
        return "SseEvent{" +
                "data='" + data + '\'' +
                ", id='" + id + '\'' +
                ", event='" + event + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        SseEvent sseEvent = (SseEvent)object;
        return Objects.equals(data, sseEvent.data) && Objects.equals(id, sseEvent.id) && Objects.equals(event, sseEvent.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, id, event);
    }
}
