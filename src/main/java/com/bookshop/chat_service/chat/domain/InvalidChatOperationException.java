package com.bookshop.chat_service.chat.domain;

public class InvalidChatOperationException extends RuntimeException {
    public InvalidChatOperationException(String message) {
        super("Invalid chat operation: " + message);
    }
}
