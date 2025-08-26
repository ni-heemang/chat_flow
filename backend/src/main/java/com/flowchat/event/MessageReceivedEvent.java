package com.flowchat.event;

import com.flowchat.entity.ChatMessage;
import org.springframework.context.ApplicationEvent;

public class MessageReceivedEvent extends ApplicationEvent {
    private final ChatMessage message;
    private final Long roomId;
    private final String username;

    public MessageReceivedEvent(Object source, ChatMessage message, Long roomId, String username) {
        super(source);
        this.message = message;
        this.roomId = roomId;
        this.username = username;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getUsername() {
        return username;
    }
}