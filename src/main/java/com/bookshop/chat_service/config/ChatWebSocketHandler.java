package com.bookshop.chat_service.config;

import com.bookshop.chat_service.chat.domain.ChatMessage;
import com.bookshop.chat_service.chat.domain.ChatRoomStatus;
import com.bookshop.chat_service.chat.domain.ChatService;
import com.bookshop.chat_service.chat.domain.MessageType;
import com.bookshop.chat_service.chat.web.ChatRoomUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatWebSocketHandler implements WebSocketHandler {

    ChatService chatService;
    ObjectMapper objectMapper;

    @NonFinal
    final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @NonFinal
    final Map<Long, Set<String>> typingUsersByRoom = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = session.getHandshakeInfo().getUri().getQuery().split("=")[1];
        sessions.put(userId, session);
        log.info("User {} connected to WebSocket", userId);

        return session.receive()
                .map(webSocketMessage -> {
                    try {
                        return objectMapper.readValue(
                                webSocketMessage.getPayloadAsText(), WebSocketMessage.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing message", e);
                    }
                })
                .flatMap(message -> processMessage(message, session, userId))
                .doFinally(signal -> {
                    sessions.remove(userId);
                    cleanupTypingIndicators(userId);
                    log.info("User {} disconnected from WebSocket", userId);
                })
                .then();
    }

    private Mono<Void> processMessage(WebSocketMessage message, WebSocketSession session, String userId) {
        return chatService.getChatRoom(message.getRoomId())
                .flatMap(chatRoom -> {
                    if (ChatRoomStatus.CLOSED.equals(chatRoom.getStatus())) {
                        log.info("Room {} is CLOSED for user {}", message.getRoomId(), userId);
                        return processClose(message, session, userId);
                    }

                    try {
                        switch (message.getType()) {
                            case "SEND_MESSAGE":
                                return processSendMessage(message, userId);
                            case "TYPING":
                                return processTyping(message, userId);
                            case "STOP_TYPING":
                                return processStopTyping(message, userId);
                            case "ROOM_CLOSE":
                                return processClose(message, session, userId);
                            default:
                                return Mono.empty();
                        }
                    } catch (Exception e) {
                        log.error("Error processing message type {} from user {}: {}",
                                message.getType(), userId, e.getMessage(), e);
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error checking room status for room {}: {}",
                            message.getRoomId(), e.getMessage(), e);
                    return session.close(CloseStatus.SERVER_ERROR.withReason("Error checking room status"));
                });
    }


    private Mono<Void> processClose(WebSocketMessage message, WebSocketSession session, String userId) {
        Long roomId = message.getRoomId();

        ChatRoomUpdateRequest request = new ChatRoomUpdateRequest();
        request.setStatus(ChatRoomStatus.CLOSED);

        WebSocketMessage closeMessage = new WebSocketMessage();
        closeMessage.setType("ROOM_CLOSE");
        closeMessage.setRoomId(roomId);
        closeMessage.setSenderId(userId);
        closeMessage.setSenderType(message.getSenderType());
        closeMessage.setIsTyping(true);
        closeMessage.setTypingUserId(userId);
        closeMessage.setTimestamp(Instant.now());

        return chatService.updateRoom(roomId, request)
                .then(broadcastToRoomParticipants(roomId, closeMessage))
                .doOnSuccess(v -> log.debug("Room {} is  close", roomId))
                .then(session.close(CloseStatus.NORMAL))
                .then(Mono.never());
    }

    private Mono<Void> processTyping(WebSocketMessage message, String userId) {
        Long roomId = message.getRoomId();

        typingUsersByRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);

        WebSocketMessage typingMessage = new WebSocketMessage();
        typingMessage.setType("USER_TYPING");
        typingMessage.setRoomId(roomId);
        typingMessage.setSenderId(userId);
        typingMessage.setSenderType(message.getSenderType());
        typingMessage.setIsTyping(true);
        typingMessage.setTypingUserId(userId);
        typingMessage.setTimestamp(Instant.now());

        return broadcastToRoomParticipants(roomId, typingMessage)
                .doOnSuccess(v -> log.debug("User {} is typing in room {}", userId, roomId));
    }

    private Mono<Void> processStopTyping(WebSocketMessage message, String userId) {
        Long roomId = message.getRoomId();

        if (typingUsersByRoom.containsKey(roomId)) {
            typingUsersByRoom.get(roomId).remove(userId);
        }

        WebSocketMessage stopTypingMessage = new WebSocketMessage();
        stopTypingMessage.setType("USER_STOPPED_TYPING");
        stopTypingMessage.setRoomId(roomId);
        stopTypingMessage.setSenderId(userId);
        stopTypingMessage.setSenderType(message.getSenderType());
        stopTypingMessage.setIsTyping(false);
        stopTypingMessage.setTypingUserId(userId);
        stopTypingMessage.setTimestamp(Instant.now());

        // Broadcast to other participants in the room
        return broadcastToRoomParticipants(roomId, stopTypingMessage)
                .doOnSuccess(v -> log.debug("User {} stopped typing in room {}", userId, roomId));
    }


    private Mono<Void> processSendMessage(WebSocketMessage message, String userId) {
        if (message.getRoomId() == null) {
            log.error("❌ Room ID is null in message from user: {}", userId);
            return Mono.empty();
        }

        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            log.error("❌ Message content is empty from user: {}", userId);
            return Mono.empty();
        }

        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRoomId(message.getRoomId());
            chatMessage.setSenderId(userId);
            chatMessage.setSenderType(message.getSenderType());
            chatMessage.setContent(message.getContent().trim());
            chatMessage.setMessageType(MessageType.TEXT);

            return chatService.sendMessage(chatMessage, userId)
                    .flatMap(this::broadcastToRoomParticipants)
                    .then();

        } catch (Exception e) {
            log.error("Unexpected error in processSendMessage: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }

    private Mono<Void> broadcastToRoomParticipants(ChatMessage message) {
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setType("NEW_MESSAGE");
        wsMessage.setRoomId(message.getRoomId());
        wsMessage.setContent(message.getContent());
        wsMessage.setSenderId(message.getSenderId());
        wsMessage.setSenderType(message.getSenderType());
        wsMessage.setTimestamp(message.getTimestamp());

        return broadcastToRoomParticipants(message.getRoomId(), wsMessage);
    }

    private Mono<Void> broadcastToRoomParticipants(Long roomId, WebSocketMessage message) {
        return chatService.getChatRoom(roomId)
                .flatMapMany(room -> {
                    List<String> participants = new ArrayList<>();
                    if (room.getCustomerId() != null) {
                        participants.add(room.getCustomerId());
                    }
                    if (room.getEmployeeId() != null) {
                        participants.add(room.getEmployeeId());
                    }

                    return Flux.fromIterable(participants)
                            .filter(sessions::containsKey)
                            .flatMap(participantId -> {
                                WebSocketSession session = sessions.get(participantId);
                                if (session != null && session.isOpen()) {
                                    try {
                                        String messageJson = objectMapper.writeValueAsString(message);
                                        return session.send(Mono.just(session.textMessage(messageJson)));
                                    } catch (Exception e) {
                                        log.error("Error sending message to user {}", participantId, e);
                                        return Mono.empty();
                                    }
                                }
                                return Mono.empty();
                            });
                })
                .then();
    }

    private void cleanupTypingIndicators(String userId) {
        typingUsersByRoom.forEach((roomId, typingUsers) -> {
            if (typingUsers.remove(userId)) {
                WebSocketMessage stopTypingMessage = new WebSocketMessage();
                stopTypingMessage.setType("USER_STOPPED_TYPING");
                stopTypingMessage.setRoomId(roomId);
                stopTypingMessage.setTypingUserId(userId);
                stopTypingMessage.setIsTyping(false);
                stopTypingMessage.setTimestamp(Instant.now());

                broadcastToRoomParticipants(roomId, stopTypingMessage).subscribe();
            }
        });

        typingUsersByRoom.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public Set<String> getTypingUsers(Long roomId) {
        return typingUsersByRoom.getOrDefault(roomId, Collections.emptySet());
    }
}