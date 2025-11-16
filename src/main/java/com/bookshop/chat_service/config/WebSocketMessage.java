package com.bookshop.chat_service.config;

import com.bookshop.chat_service.chat.domain.SenderType;
import lombok.Data;

import java.time.Instant;

@Data
public class WebSocketMessage {
    private String type;
    private Long roomId;
    private String senderId;
    private SenderType senderType;
    private String content;
    private Instant timestamp;
    private Boolean isTyping;
    private String typingUserId;
}
