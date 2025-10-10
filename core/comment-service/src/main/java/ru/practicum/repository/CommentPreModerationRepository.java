package ru.practicum.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CommentPreModerationRepository {

    private final JdbcTemplate jdbcTemplate;

    public Set<String> forbiddenWordsByEventId(Long eventId) {
        String sql = "SELECT forbidden_word FROM comment_pre_moderation WHERE event_id = ?";

        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class, eventId));
    }

    public void updateForbiddenWords(Long eventId, Set<String> forbiddenWords) {
        if (forbiddenWords == null || forbiddenWords.isEmpty()) {
            return;
        }

        Set<String> strings = forbiddenWordsByEventId(eventId);
        forbiddenWords.addAll(strings);

        String deleteSql = "DELETE FROM comment_pre_moderation WHERE event_id = ?";
        jdbcTemplate.update(deleteSql, eventId);

        String insertSql = "INSERT INTO comment_pre_moderation (event_id, forbidden_word) VALUES (?, ?)";

        List<Object[]> batchArgs = forbiddenWords.stream()
                .map(word -> new Object[]{eventId, word})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }
}
