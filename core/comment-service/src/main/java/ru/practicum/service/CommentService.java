// comment-service/src/main/java/ru/practicum/service/CommentService.java
package ru.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.comment.CommentCreateDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CommentUpdateDto;

import java.util.List;

public interface CommentService {

    // Основные методы (из монолита)
    CommentDto createComment(Long userId, Long eventId, CommentCreateDto commentCreateDto);

    CommentDto updateComment(Long userId, Long commentId, CommentUpdateDto commentUpdateDto);

    void deleteCommentByUser(Long userId, Long commentId);

    void deleteCommentByAdmin(Long commentId);

    CommentDto getComment(Long commentId);

    List<CommentDto> getEventComments(Long eventId, Pageable pageable);

    List<CommentDto> getUserComments(Long userId, Pageable pageable);

    List<CommentDto> getCommentsAdmin(List<Long> events, List<Long> users, Pageable pageable);

    // Методы для внутреннего использования (Feign клиенты)
    Boolean commentExists(Long commentId);

    Long getCountPublishedCommentsByEventId(Long eventId);
}