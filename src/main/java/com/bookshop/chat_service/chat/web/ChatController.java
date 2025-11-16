package com.bookshop.chat_service.chat.web;

import com.bookshop.chat_service.chat.domain.ChatMessage;
import com.bookshop.chat_service.chat.domain.ChatRoom;
import com.bookshop.chat_service.chat.domain.ChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("chats")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {

    ChatService chatService;

    @GetMapping("/rooms")
    public Flux<ChatRoom> getChatRooms() {
        return chatService.getChatRooms();
    }

    @GetMapping("/rooms/{roomId}")
    public Mono<ChatRoom> getChatRoom(@PathVariable Long roomId) {
        return chatService.getChatRoom(roomId);
    }

    @GetMapping("/messages/{roomId}")
    public Flux<ChatMessage> getChatMessageByRoomId(@PathVariable Long roomId) {
        return chatService.getRoomMessages(roomId);
    }

    @PostMapping("/rooms")
    public Mono<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        return chatService.createChatRoom(request.getCustomerId());
    }

    @PutMapping("/rooms/{roomId}")
    public Mono<ChatRoom> updateRoom(
            @PathVariable Long roomId,
            @RequestBody ChatRoomUpdateRequest request
    ) {
        return chatService.updateRoom(roomId, request);
    }

    @PutMapping("/rooms/{roomId}/assign")
    public Mono<ChatRoom> assignEmployee(
            @PathVariable Long roomId,
            @RequestBody AssignEmployeeRequest request) {
        return chatService.assignEmployee(roomId, request.getEmployeeId());
    }

    @GetMapping("/rooms/{roomId}/messages")
    public Flux<ChatMessage> getMessages(@PathVariable Long roomId) {
        return chatService.getRoomMessages(roomId);
    }

    @GetMapping("/rooms/customer/{customerId}")
    public Flux<ChatRoom> getCustomerRooms(@PathVariable String customerId) {
        return chatService.getCustomerRooms(customerId);
    }

    @GetMapping("/rooms/employee/{employeeId}")
    public Flux<ChatRoom> getEmployeeRooms(@PathVariable String employeeId) {
        return chatService.getEmployeeRooms(employeeId);
    }

    @PutMapping("/rooms/{roomId}/read")
    public Mono<Void> markAsRead(
            @PathVariable Long roomId,
            @RequestParam String readerId) {
        return chatService.markMessagesAsRead(roomId, readerId);
    }
}


