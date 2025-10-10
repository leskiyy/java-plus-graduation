package ru.practicum.client;

import feign.FeignException;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.comment.CommentWithUserDto;
import ru.practicum.parameters.PageableSearchParam;

import java.util.List;

@FeignClient(name = "comment-service", path = "api/v1/comment")
public interface CommentClient {

    @GetMapping("/{eventId}")
    public List<CommentWithUserDto> getCommentsByEventId(@PathVariable Long eventId,
                                                         @RequestParam(defaultValue = "0") Integer from,
                                                         @RequestParam(defaultValue = "10") Integer size) throws FeignException;
}
