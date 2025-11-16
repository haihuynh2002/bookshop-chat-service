package com.bookshop.chat_service.chat.domain;

public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(Long roomId) {
        super("Chat room not found with id: " + roomId);
    }
}
