// request-service/src/main/java/ru/practicum/controller/PrivateEventRequestController.java
package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Validated
public class PrivateEventRequestController {
    private final ParticipationRequestService requestService;

    @PatchMapping
    public EventRequestStatusUpdateResult updateRequestStatuses(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests with body {}", userId, eventId, request);
        return requestService.updateRequestStatuses(userId, eventId, request);
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequests(@PathVariable @Min(1) Long userId,
                                                     @PathVariable @Min(1) Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return requestService.getRequestsForEvent(eventId, userId);
    }
}