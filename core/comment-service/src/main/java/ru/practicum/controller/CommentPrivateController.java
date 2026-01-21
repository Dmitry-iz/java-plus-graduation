// comment-service/src/main/java/ru/practicum/controller/CommentPrivateController.java
package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentCreateDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CommentUpdateDto;
import ru.practicum.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
public class CommentPrivateController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable @Min(1) Long userId,
                                    @PathVariable @Min(1) Long eventId,
                                    @RequestBody @Valid CommentCreateDto commentCreateDto) {
        return commentService.createComment(userId, eventId, commentCreateDto);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentDto updateComment(@PathVariable @Min(1) Long userId,
                                    @PathVariable @Min(1) Long commentId,
                                    @RequestBody @Valid CommentUpdateDto commentUpdateDto) {
        return commentService.updateComment(userId, commentId, commentUpdateDto);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Min(1) Long userId,
                              @PathVariable @Min(1) Long commentId) {
        commentService.deleteCommentByUser(userId, commentId);
    }

    @GetMapping("/comments")
    public List<CommentDto> getUserComments(@PathVariable @Min(1) Long userId,
                                            @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return commentService.getUserComments(userId, pageable);
    }
}