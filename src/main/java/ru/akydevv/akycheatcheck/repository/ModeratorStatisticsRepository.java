package ru.akydevv.akycheatcheck.repository;

import ru.akydevv.akycheatcheck.model.ModeratorStatistics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ModeratorStatisticsRepository {
    CompletableFuture<Optional<ModeratorStatistics>> findDaily(UUID moderatorId, LocalDate day);
    CompletableFuture<List<ModeratorStatistics>> findAllForModerator(UUID moderatorId);
    CompletableFuture<List<ModeratorStatistics>> findAllDaily(LocalDate day);
    CompletableFuture<Void> save(ModeratorStatistics statistics);
    void close();
}
