package com.bookshop.chat_service.chat.web;

import com.bookshop.chat_service.chat.domain.ChatRoomStatus;
import lombok.Data;

@Data
public class ChatRoomUpdateRequest {
    ChatRoomStatus status;
}
