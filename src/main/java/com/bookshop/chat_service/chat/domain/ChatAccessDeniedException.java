package com.bookshop.chat_service.chat.domain;

public class ChatAccessDeniedException extends RuntimeException {
    public ChatAccessDeniedException(Long roomId, String userId) {
        super("User " + userId + " does not have access to chat room: " + roomId);
    }
}