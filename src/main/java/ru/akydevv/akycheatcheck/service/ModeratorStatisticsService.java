package ru.akydevv.akycheatcheck.service;

import org.bukkit.entity.Player;
import ru.akydevv.akycheatcheck.model.ModeratorStatistics;
import ru.akydevv.akycheatcheck.repository.ModeratorStatisticsRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModeratorStatisticsService {
    private final ModeratorStatisticsRepository repository;
    private final ZoneId zoneId;
    private final ConcurrentMap<String, ModeratorStatistics> dailyCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<ModeratorStatistics>> pendingUpdates = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> totalChecksCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> totalBansCache = new ConcurrentHashMap<>();
    private final Set<UUID> totalStatsLoading = ConcurrentHashMap.newKeySet();

    public ModeratorStatisticsService(ModeratorStatisticsRepository repository, ZoneId zoneId) {
        this.repository = repository;
        this.zoneId = zoneId;
    }

    public void recordCheck(Player moderator) {
        totalChecksCache.merge(moderator.getUniqueId(), 1, Integer::sum);
        updateToday(moderator, ModeratorStatistics::withAddedCheck);
    }

    public void recordBan(Player moderator) {
        totalBansCache.merge(moderator.getUniqueId(), 1, Integer::sum);
        updateToday(moderator, ModeratorStatistics::withAddedBan);
    }

    public void recordRelease(Player moderator) {
        updateToday(moderator, ModeratorStatistics::withAddedRelease);
    }

    public CompletableFuture<List<ModeratorStatistics>> findTotal(UUID moderatorId) {
        return repository.findAllForModerator(moderatorId)
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparing(ModeratorStatistics::getDay).reversed())
                        .toList());
    }

    public CompletableFuture<List<ModeratorStatistics>> findTodayLeaderboard() {
        return repository.findAllDaily(LocalDate.now(zoneId))
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparingInt(ModeratorStatistics::getChecks).reversed())
                        .toList());
    }

    public int getCachedTotalChecks(UUID moderatorId) {
        hydrateTotalCache(moderatorId);
        return totalChecksCache.getOrDefault(moderatorId, 0);
    }

    public int getCachedTotalBans(UUID moderatorId) {
        hydrateTotalCache(moderatorId);
        return totalBansCache.getOrDefault(moderatorId, 0);
    }

    public void flushCache() {
        CompletableFuture.allOf(pendingUpdates.values().toArray(CompletableFuture[]::new)).join();
        List<CompletableFuture<Void>> futures = dailyCache.values().stream().map(repository::save).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        dailyCache.clear();
        pendingUpdates.clear();
        totalStatsLoading.clear();
    }

    private void hydrateTotalCache(UUID moderatorId) {
        if (totalStatsLoading.contains(moderatorId)) {
            return;
        }
        totalStatsLoading.add(moderatorId);
        repository.findAllForModerator(moderatorId).thenAccept(stats -> {
            int loadedChecks = stats.stream().mapToInt(ModeratorStatistics::getChecks).sum();
            int loadedBans = stats.stream().mapToInt(ModeratorStatistics::getBans).sum();
            totalChecksCache.merge(moderatorId, loadedChecks, Math::max);
            totalBansCache.merge(moderatorId, loadedBans, Math::max);
        }).whenComplete((unused, exception) -> totalStatsLoading.remove(moderatorId));
    }

    private void updateToday(Player moderator, java.util.function.UnaryOperator<ModeratorStatistics> updater) {
        LocalDate day = LocalDate.now(zoneId);
        String key = key(moderator.getUniqueId(), day);
        pendingUpdates.compute(key, (ignored, previous) -> {
            CompletableFuture<ModeratorStatistics> baseFuture = previous == null
                    ? repository.findDaily(moderator.getUniqueId(), day).thenApply(optional -> optional.orElseGet(() ->
                            new ModeratorStatistics(moderator.getUniqueId(), moderator.getName(), day, 0, 0, 0)))
                    : previous;
            return baseFuture.thenApply(updater).thenCompose(updated -> {
                dailyCache.put(key, updated);
                return repository.save(updated).thenApply(unused -> updated);
            });
        });
    }

    private String key(UUID moderatorId, LocalDate day) {
        return moderatorId + ":" + day;
    }
}
