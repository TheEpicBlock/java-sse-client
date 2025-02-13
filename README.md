# Java SSE Client
A client for [server-sent-event](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)s
for plain java. Makes use of the java 11+ HttpClient. It is intended to be used for long-lived connections,
and has built-in support for reconnecting.

To use, please extend `SseClient`.

This library can be installed via maven. It's available from the `https://maven.theepicblock.nl` maven repository as
`nl.theepicblock:java-sse-client`