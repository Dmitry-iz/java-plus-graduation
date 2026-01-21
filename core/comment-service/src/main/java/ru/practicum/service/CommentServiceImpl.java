// comment-service/src/main/java/ru/practicum/service/CommentServiceImpl.java
package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.comment.CommentCreateDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CommentUpdateDto;
import ru.practicum.dto.event.EventDtoOut;
import ru.practicum.dto.event.EventShortDtoOut;
import ru.practicum.dto.user.UserDtoOut;
import ru.practicum.enums.CommentStatus;
import ru.practicum.exception.ConditionNotMetException;
import ru.practicum.exception.NoAccessException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.repository.CommentRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, CommentCreateDto commentCreateDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        // Проверяем существование пользователя
        Boolean userExists = userClient.userExists(userId);
        if (userExists == null || !userExists) {
            throw new NotFoundException("User", userId);
        }

        // Получаем полную информацию о событии для проверки состояния
        EventDtoOut event = eventClient.getEventById(eventId);
        if (event == null) {
            throw new NotFoundException("Event", eventId);
        }

        // Проверяем, что событие опубликовано (как в монолите, но через строку)
        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConditionNotMetException("Нельзя оставлять комментарии к неопубликованному событию");
        }

        Comment comment = Comment.builder()
                .text(commentCreateDto.getText().trim())
                .userId(userId)
                .eventId(eventId)
                .status(CommentStatus.PUBLISHED)
                .build();

        Comment saved = commentRepository.save(comment);

        // Получаем данные автора для DTO
        UserDtoOut author = userClient.getUserById(userId);

        // Получаем краткую информацию о событии для DTO
        EventShortDtoOut eventShort = eventClient.getShortEventById(eventId);

        log.info("Создан комментарий ID: {} пользователем ID: {} к событию ID: {}",
                saved.getId(), userId, eventId);

        return CommentMapper.toDto(saved, eventShort, author);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, CommentUpdateDto commentUpdateDto) {
        log.info("Обновление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new NoAccessException("Редактировать можно только свои комментарии");
        }

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConditionNotMetException("Нельзя редактировать удаленный комментарий");
        }

        if (commentUpdateDto.getText() != null && !commentUpdateDto.getText().trim().isEmpty()) {
            comment.setText(commentUpdateDto.getText().trim());
            comment.setStatus(CommentStatus.EDITED);
        }

        Comment updated = commentRepository.save(comment);

        // Получаем данные для DTO
        EventShortDtoOut event = eventClient.getShortEventById(comment.getEventId());
        UserDtoOut author = userClient.getUserById(userId);

        return CommentMapper.toDto(updated, event, author);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new NoAccessException("Удалять можно только свои комментарии");
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Удаление комментария {} администратором", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Override
    public CommentDto getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", commentId));

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new NotFoundException("Comment", commentId);
        }

        // Получаем данные для DTO
        EventShortDtoOut event = eventClient.getShortEventById(comment.getEventId());
        UserDtoOut author = userClient.getUserById(comment.getUserId());

        return CommentMapper.toDto(comment, event, author);
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Pageable pageable) {
        log.info("Получение комментариев события {}", eventId);

        // Проверяем существование события
        Boolean eventExists = eventClient.eventExists(eventId);
        if (eventExists == null || !eventExists) {
            throw new NotFoundException("Event", eventId);
        }

        List<CommentStatus> activeStatuses = List.of(CommentStatus.PUBLISHED, CommentStatus.EDITED);
        Page<Comment> commentsPage = commentRepository
                .findByEventIdAndStatusInOrderByCreatedAtDesc(eventId, activeStatuses, pageable);

        List<Comment> comments = commentsPage.getContent();

        return enrichCommentsWithExternalData(comments);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Pageable pageable) {
        log.info("Получение комментариев пользователя {}", userId);

        // Проверяем существование пользователя
        Boolean userExists = userClient.userExists(userId);
        if (userExists == null || !userExists) {
            throw new NotFoundException("User", userId);
        }

        Page<Comment> commentsPage = commentRepository
                .findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, CommentStatus.DELETED, pageable);

        List<Comment> comments = commentsPage.getContent();

        return enrichCommentsWithExternalData(comments);
    }

    @Override
    public List<CommentDto> getCommentsAdmin(List<Long> events, List<Long> users, Pageable pageable) {
        log.info("Получение комментариев админом events: {}, users: {}", events, users);

        Page<Comment> commentsPage = commentRepository
                .findByEventIdInAndUserIdInOrderByCreatedAtDesc(events, users, pageable);

        List<Comment> comments = commentsPage.getContent();

        return enrichCommentsWithExternalData(comments);
    }

    @Override
    public Boolean commentExists(Long commentId) {
        return commentRepository.existsById(commentId);
    }

    @Override
    public Long getCountPublishedCommentsByEventId(Long eventId) {
        List<CommentStatus> publishedStatuses = List.of(CommentStatus.PUBLISHED, CommentStatus.EDITED);
        return commentRepository.countByEventIdAndStatusIn(eventId, publishedStatuses);
    }

    private List<CommentDto> enrichCommentsWithExternalData(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }

        // Собираем уникальные ID событий и пользователей
        List<Long> eventIds = comments.stream()
                .map(Comment::getEventId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // Получаем данные событий и пользователей
        Map<Long, EventShortDtoOut> eventsMap = eventClient.getEventsByIds(eventIds).stream()
                .collect(Collectors.toMap(EventShortDtoOut::getId, Function.identity()));

        Map<Long, UserDtoOut> usersMap = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserDtoOut::getId, Function.identity()));

        // Собираем DTO
        return comments.stream()
                .map(comment -> {
                    EventShortDtoOut event = eventsMap.get(comment.getEventId());
                    UserDtoOut author = usersMap.get(comment.getUserId());

                    if (event == null || author == null) {
                        log.warn("Не удалось получить данные для комментария {}: event={}, author={}",
                                comment.getId(), comment.getEventId(), comment.getUserId());
                        return null;
                    }

                    return CommentMapper.toDto(comment, event, author);
                })
                .filter(commentDto -> commentDto != null)
                .collect(Collectors.toList());
    }
}