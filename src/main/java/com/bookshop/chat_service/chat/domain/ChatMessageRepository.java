package com.bookshop.chat_service.chat.domain;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface ChatMessageRepository extends R2dbcRepository<ChatMessage, Long> {
    Flux<ChatMessage> findByRoomIdOrderByTimestamp(Long roomId);
    Flux<ChatMessage> findByRoomIdAndTimestampAfter(Long roomId, Instant since);
    Flux<ChatMessage> findByRoomIdAndReadFalseAndSenderIdNot(Long roomId, String senderId);
}