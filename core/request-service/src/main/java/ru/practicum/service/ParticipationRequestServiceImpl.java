package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventDtoOut;
import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.enums.EventState;
import ru.practicum.enums.RequestStatus;
import ru.practicum.exception.ConditionNotMetException;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.exception.NoAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static ru.practicum.enums.RequestStatus.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestRepository requestRepository;

//    @Override
//    @Transactional
//    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
//        log.info("Пользователь {} пытается создать запрос участия для события {}", userId, eventId);
//
//        // Проверяем существование пользователя - как в монолите
//        if (!userExists(userId)) {
//            throw new NotFoundException("User", userId);
//        }
//
//        // Получаем событие
//        EventDtoOut event = eventClient.getEventById(eventId);
//        if (event == null) {
//            throw new NotFoundException("Event", eventId);
//        }
//
//        // Проверки из монолита:
//        checkRequestNotExists(userId, eventId);
//        checkNotEventInitiator(userId, event);
//        checkEventIsPublished(event);
//        checkParticipantLimit(event, eventId);
//
//        RequestStatus status = determineRequestStatus(event);
//
//        ParticipationRequest request = new ParticipationRequest();
//        request.setRequesterId(userId);
//        request.setEventId(eventId);
//        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
//        request.setStatus(status);
//
//        ParticipationRequest saved = requestRepository.save(request);
//
//        log.info("Создана заявка от пользователя {} на событие {} со статусом {}", userId, eventId, status);
//        return ParticipationRequestMapper.toDto(saved);
//    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Пользователь {} пытается создать запрос участия для события {}", userId, eventId);

        // Проверяем существование пользователя
        if (!userExists(userId)) {
            throw new NotFoundException("User", userId);
        }

        // Получаем событие
        EventDtoOut event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }

        // Проверки из монолита:
        checkRequestNotExists(userId, eventId);
        checkNotEventInitiator(userId, event);
        checkEventIsPublished(event);
        checkParticipantLimit(event, eventId);

        RequestStatus status = determineRequestStatus(event);

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);      // ← В микросервисе: только ID
        request.setEventId(eventId);         // ← В микросервисе: только ID
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        request.setStatus(status);

        ParticipationRequest saved = requestRepository.save(request);

        log.info("Создана заявка от пользователя {} на событие {} со статусом {}", userId, eventId, status);
        return ParticipationRequestMapper.toDto(saved);
    }

//    @Override
//    public List<ParticipationRequestDto> getUserRequests(Long userId) {
//        // Проверяем существование пользователя - как в монолите
//        if (!userExists(userId)) {
//            throw new NotFoundException("User", userId);
//        }
//
//        return requestRepository.findAllByRequesterId(userId).stream()
//                .map(ParticipationRequestMapper::toDto)
//                .toList();
//    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        // Проверяем существование пользователя - как в монолите
        if (!userExists(userId)) {
            throw new NotFoundException("User", userId);
        }

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest request) {
        // Проверяем существование пользователя - как в монолите
        if (!userExists(userId)) {
            throw new NotFoundException("User", userId);
        }

        EventDtoOut event = getEventWithCheck(userId, eventId);
        List<ParticipationRequest> requests = getPendingRequestsOrThrow(request.getRequestIds());

        return switch (request.getStatus()) {
            case "CONFIRMED" -> confirmRequests(event, requests);
            case "REJECTED" -> rejectRequests(requests);
            default -> throw new IllegalArgumentException("Неправильный статус: " + request.getStatus());
        };
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long eventId, Long initiatorId) {
        log.debug("getRequestsForEvent: {} of user: {}", eventId, initiatorId);

        // Проверяем существование пользователя - как в монолите
        if (!userExists(initiatorId)) {
            throw new NotFoundException("User", initiatorId);
        }

        // Получаем событие
        EventDtoOut event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }

        // Проверяем, что пользователь - инициатор события
        // В монолите: if (!event.getInitiator().getId().equals(initiatorId))
        if (!event.getInitiator().getId().equals(initiatorId)) {
            throw new NoAccessException("Только инициатор может просматривать запросы на проведение мероприятия");
        }

        List<ParticipationRequest> allByEventId = requestRepository.findAllByEventId(eventId);
        return allByEventId.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Пользователь {} отменяет заявку с ID {}", userId, requestId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ParticipationRequest", requestId));

        // В монолите: if (!request.getRequester().getId().equals(userId))
        if (!request.getRequesterId().equals(userId)) {
            throw new ForbiddenException("Отменить его может только автор заявки.");
        }

        request.setStatus(CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        return ParticipationRequestMapper.toDto(saved);
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (аналогичные монолиту) ============

//    private boolean userExists(Long userId) {
//        Boolean exists = userClient.userExists(userId);
//        return exists != null && exists;
//    }

    private boolean userExists(Long userId) {
        Boolean exists = userClient.userExists(userId);
        return exists != null && exists;
    }

    private void checkRequestNotExists(Long userId, Long eventId) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConditionNotMetException("Заявка на участие уже отправлена.");
        }
    }

    private void checkNotEventInitiator(Long userId, EventDtoOut event) {
        // В монолите: if (event.getInitiator().getId().equals(userId))
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConditionNotMetException("Заявка на участие уже отправлена.");
        }
    }

    private void checkEventIsPublished(EventDtoOut event) {
        // В монолите: if (!event.getState().equals(EventState.PUBLISHED))
        // В микросервисе state приходит как String
        if (!EventState.PUBLISHED.name().equals(event.getState())) {
            throw new ConditionNotMetException("Невозможно принять участие в неопубликованном мероприятии.");
        }
    }

    private void checkParticipantLimit(EventDtoOut event, Long eventId) {
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConditionNotMetException("Лимит участников мероприятия достигнут.");
        }
    }

    private RequestStatus determineRequestStatus(EventDtoOut event) {
        return (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;
    }

    private EventDtoOut getEventWithCheck(Long userId, Long eventId) {
        EventDtoOut event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }

        // В монолите: if (!event.getInitiator().getId().equals(userId))
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Пользователь не является инициатором события");
        }

        // В монолите: if (!EventState.PUBLISHED.equals(event.getState()))
        if (!EventState.PUBLISHED.name().equals(event.getState())) {
            throw new ConditionNotMetException("Мероприятие должно быть опубликовано");
        }

        return event;
    }

    private List<ParticipationRequest> getPendingRequestsOrThrow(List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);

        // Проверяем, что все запросы имеют статус PENDING - как в монолите
        boolean hasNonPending = requests.stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.PENDING);

        if (hasNonPending) {
            throw new ConditionNotMetException("Запрос должен иметь статус ОЖИДАЮЩИЙ");
        }

        return requests;
    }

    private EventRequestStatusUpdateResult confirmRequests(EventDtoOut event, List<ParticipationRequest> requests) {
        checkIfLimitAvailableOrThrow(event);
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        int available = limit - (int) confirmedCount;

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (shouldAutoConfirm(event)) {
                confirmRequest(request, confirmed);
            } else if (available > 0) {
                confirmRequest(request, confirmed);
                available--;
            } else {
                rejectRequest(request, rejected);
            }
        }

        requestRepository.saveAll(requests);

        // Создаем DTO как в монолите
        List<ParticipationRequestDto> confirmedDtos = confirmed.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();

        List<ParticipationRequestDto> rejectedDtos = rejected.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();

        return new EventRequestStatusUpdateResult(confirmedDtos, rejectedDtos);
    }

    private void checkIfLimitAvailableOrThrow(EventDtoOut event) {
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (limit != 0 && Boolean.TRUE.equals(event.getRequestModeration()) && confirmedCount >= limit) {
            throw new ConditionNotMetException("Лимит участников мероприятия достигнет");
        }
    }

    private boolean shouldAutoConfirm(EventDtoOut event) {
        return event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration());
    }

    private void confirmRequest(ParticipationRequest request, List<ParticipationRequest> confirmed) {
        request.setStatus(RequestStatus.CONFIRMED);
        confirmed.add(request);
    }

    private void rejectRequest(ParticipationRequest request, List<ParticipationRequest> rejected) {
        request.setStatus(RequestStatus.REJECTED);
        rejected.add(request);
    }

    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        for (ParticipationRequest r : requests) {
            r.setStatus(RequestStatus.REJECTED);
        }

        requestRepository.saveAll(requests);

        // Создаем DTO как в монолите
        List<ParticipationRequestDto> rejectedDtos = requests.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();

        return new EventRequestStatusUpdateResult(List.of(), rejectedDtos);
    }
}