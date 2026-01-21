package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;

@UtilityClass
public class ParticipationRequestMapper {
    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())       // ← Используем eventId
                .requester(request.getRequesterId()) // ← Используем requesterId
                .status(request.getStatus().name())
                .build();
    }
}