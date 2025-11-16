package com.bookshop.chat_service.chat.domain;

import com.bookshop.chat_service.chat.web.ChatRoomUpdateRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatService {

    ChatRoomRepository roomRepository;
    ChatMessageRepository messageRepository;

    public Flux<ChatRoom> getChatRooms() {
        return roomRepository.findAll();
    }

    public Mono<ChatRoom> getChatRoom(Long id) {
        return roomRepository.findById(id)
                .switchIfEmpty(Mono.error(new ChatRoomNotFoundException(id)));
    }

    public Flux<ChatRoom> getCustomerRooms(String customerId) {
        return roomRepository.findByCustomerId(customerId);
    }

    public Flux<ChatRoom> getEmployeeRooms(String employeeId) {
        return roomRepository.findByEmployeeId(employeeId);
    }

    public Mono<ChatRoom> createChatRoom(String customerId) {
        ChatRoom room = ChatRoom.builder()
                .customerId(customerId)
                .status(ChatRoomStatus.OPEN)
                .build();

        return roomRepository.save(room);
    }

    public Mono<ChatRoom> assignEmployee(Long roomId, String employeeId) {
        return roomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new ChatRoomNotFoundException(roomId)))
                .map(room -> updateRoomWithEmployee(room, employeeId))
                .flatMap(roomRepository::save);
    }

    public Mono<ChatRoom> updateRoom(Long id, ChatRoomUpdateRequest request) {
        return roomRepository.findById(id)
                .switchIfEmpty(Mono.error(new ChatRoomNotFoundException(id)))
                .map(room -> updateRoomStatus(room, request))
                .flatMap(roomRepository::save);
    }

    public Mono<ChatMessage> sendMessage(ChatMessage message, String senderId) {
        return validateMessageRoom(message.getRoomId())
                .then(Mono.defer(() -> {
                    message.setTimestamp(Instant.now());
                    message.setRead(false);
                    message.setSenderId(senderId);
                    return messageRepository.save(message);
                }))
                .flatMap(savedMessage -> updateRoomLastModified(savedMessage.getRoomId())
                        .thenReturn(savedMessage));
    }

    public Flux<ChatMessage> getRoomMessages(Long roomId) {
        return validateRoomExists(roomId)
                .thenMany(messageRepository.findByRoomIdOrderByTimestamp(roomId));
    }

    public Flux<ChatMessage> getRoomMessagesSince(Long roomId, Instant since) {
        return validateRoomExists(roomId)
                .thenMany(messageRepository.findByRoomIdAndTimestampAfter(roomId, since));
    }

    public Mono<Void> markMessagesAsRead(Long roomId, String readerId) {
        return messageRepository.findByRoomIdOrderByTimestamp(roomId)
                .filter(message -> !message.getRead() &&
                        !message.getSenderId().equals(readerId))
                .flatMap(this::markMessageAsRead)
                .then();
    }

    private ChatRoom updateRoomWithEmployee(ChatRoom room, String employeeId) {
        room.setEmployeeId(employeeId);
        room.setStatus(ChatRoomStatus.ASSIGNED);
        return room;
    }

    private ChatRoom updateRoomStatus(ChatRoom room, ChatRoomUpdateRequest request) {
        room.setStatus(request.getStatus());
        return room;
    }

    private Mono<ChatRoom> updateRoomLastModified(Long roomId) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    room.setLastModifiedDate(Instant.now());
                    return room;
                })
                .flatMap(roomRepository::save);
    }

    private Mono<ChatMessage> markMessageAsRead(ChatMessage message) {
        message.setRead(true);
        return messageRepository.save(message);
    }

    private Mono<Void> validateMessageRoom(Long roomId) {
        return roomRepository.existsById(roomId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ChatRoomNotFoundException(roomId));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateRoomExists(Long roomId) {
        return roomRepository.existsById(roomId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ChatRoomNotFoundException(roomId));
                    }
                    return Mono.empty();
                });
    }
}