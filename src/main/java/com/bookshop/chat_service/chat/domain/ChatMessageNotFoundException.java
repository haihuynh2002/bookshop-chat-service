package com.bookshop.chat_service.chat.domain;

public class ChatMessageNotFoundException extends RuntimeException {
    public ChatMessageNotFoundException(Long messageId) {
        super("Chat message not found with id: " + messageId);
    }
}
