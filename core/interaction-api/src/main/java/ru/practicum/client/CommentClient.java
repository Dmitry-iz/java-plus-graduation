// interaction-api/src/main/java/ru/practicum/client/CommentClient.java
package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "comment-service", path = "/internal/comments")
public interface CommentClient {

    @GetMapping("/count/{eventId}")
    Long getCountPublishedCommentsByEventId(@PathVariable Long eventId);

    @GetMapping("/exists/{commentId}")
    Boolean commentExists(@PathVariable Long commentId);
}