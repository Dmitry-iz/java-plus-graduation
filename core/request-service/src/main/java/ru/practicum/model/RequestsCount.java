package ru.practicum.model;

public interface RequestsCount {
    Long getEventId();  // ← В монолите getId() возвращал eventId
    Integer getCount();
}