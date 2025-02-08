package nl.theepicblock.sseclient;

public abstract class SseListener {
    public abstract void onEvent(SseEvent event);
}
