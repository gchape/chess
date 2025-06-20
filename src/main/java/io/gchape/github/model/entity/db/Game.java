package io.gchape.github.model.entity.db;

import java.time.Instant;

public record Game(
        int id,
        int playerWhiteId,
        int playerBlackId,
        Instant startTime,
        String gameplay
) {
}

