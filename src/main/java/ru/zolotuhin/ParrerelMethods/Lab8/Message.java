package ru.zolotuhin.ParrerelMethods.Lab8;

public class Message {
    private final String from;
    private final String content;
    private final MessageType type;
    private final long timestamp;

    public Message(String from, String content, MessageType type) {
        this.from = from;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getFrom() { return from; }
    public String getContent() { return content; }
    public MessageType getType() { return type; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%tH:%tM:%tS] %s: %s",
                timestamp, timestamp, timestamp, from, content);
    }
}
