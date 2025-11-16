package com.bookshop.chat_service.chat.domain;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRoomRepository extends R2dbcRepository<ChatRoom, Long> {
    Flux<ChatRoom> findByCustomerId(String customerId);
    Flux<ChatRoom> findByEmployeeId(String employeeId);
    Mono<ChatRoom> findByCustomerIdAndStatus(String customerId, ChatRoomStatus status);
}