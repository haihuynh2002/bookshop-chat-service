package com.bookshop.chat_service.chat.domain;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table("chat_message")
public class ChatMessage {
    @Id
    Long id;
    Long roomId;
    String senderId;
    SenderType senderType;
    String content;
    MessageType messageType;
    Instant timestamp;
    Boolean read;
}