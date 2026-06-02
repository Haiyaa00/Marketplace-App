package com.example.marketplace.model;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

public class ChatRoom {
    private String id;
    private List<String> participants;
    private String lastMessage;
    private long lastTimestamp;
    private String lastSenderId;

    @PropertyName("read")
    private boolean read;

    public ChatRoom() {}

    public ChatRoom(String id, List<String> participants, String lastMessage, long lastTimestamp, String lastSenderId, boolean read) {
        this.id = id;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.lastSenderId = lastSenderId;
        this.read = read;
    }

    // CÁC GETTER/SETTER:
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    @PropertyName("read")
    public boolean isRead() { return read; }

    @PropertyName("read")
    public void setRead(boolean read) { this.read = read; }
}