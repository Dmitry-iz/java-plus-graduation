package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventDtoOut;
import ru.practicum.dto.event.EventShortDtoOut;
import ru.practicum.event.service.EventService;

import java.util.List;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class EventClientController implements EventClient {

    private final EventService eventService;

    @Override
    @GetMapping("/{eventId}")
    public EventDtoOut getEventById(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }

    @Override
    @GetMapping("/short/{eventId}")
    public EventShortDtoOut getShortEventById(@PathVariable Long eventId) {
        return eventService.getShortEventById(eventId);
    }

    @Override
    @GetMapping("/exists/{eventId}")
    public Boolean eventExists(@PathVariable Long eventId) {
        return eventService.eventExists(eventId);
    }

    @Override
    @GetMapping("/batch")
    public List<EventShortDtoOut> getEventsByIds(@RequestParam List<Long> eventIds) {
        return eventService.getEventsByIds(eventIds);
    }

    @Override
    @GetMapping("/batch/full")
    public List<EventDtoOut> getFullEventsByIds(@RequestParam List<Long> eventIds) {
        return eventService.getFullEventsByIds(eventIds);
    }

    @Override
    @PutMapping("/{eventId}/increment")
    public void incrementConfirmedRequests(@PathVariable Long eventId,
                                           @RequestParam(required = false, defaultValue = "1") int count) {
        // Реализация увеличения счетчика подтвержденных запросов
        log.info("Increment confirmed requests for event {} by {}", eventId, count);
    }
}