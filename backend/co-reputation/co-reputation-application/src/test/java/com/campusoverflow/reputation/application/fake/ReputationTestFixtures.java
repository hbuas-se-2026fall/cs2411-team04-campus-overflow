package com.campusoverflow.reputation.application.fake;

import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 声誉上下文的手写测试替身。应用服务只依赖端口接口，所以不需要 Spring 容器也不需要数据库，
 * 单元测试可在毫秒级完成——这也是「可测试性」作为驱动性架构特征带来的收益。
 */
public final class ReputationTestFixtures {

    private ReputationTestFixtures() {
    }

    public static class InMemoryAccounts implements ReputationAccountRepository {
        private final Map<Long, ReputationAccount> store = new LinkedHashMap<>();

        /** 直接放入一个已存在的账户（模拟开户已完成）。 */
        public void put(ReputationAccount account) {
            store.put(account.userId(), account);
        }

        @Override
        public Optional<ReputationAccount> findByUserId(long userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public List<ReputationAccount> findAllByUserId(Collection<Long> userIds) {
            return userIds.stream().map(store::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public List<ReputationAccount> topByReputation(int limit) {
            return store.values().stream()
                    .sorted((a, b) -> Integer.compare(b.reputation(), a.reputation()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        public List<ReputationAccount> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public ReputationAccount save(ReputationAccount account) {
            store.put(account.userId(), account);
            return account;
        }
    }

    public static class InMemoryLedger implements LedgerRepository {
        public final List<LedgerEntry> entries = new ArrayList<>();

        @Override
        public boolean exists(String eventId, long userId, ReputationReason reason) {
            return entries.stream().anyMatch(e -> e.eventId().equals(eventId)
                    && e.userId() == userId && e.reason() == reason);
        }

        @Override
        public void append(LedgerEntry entry) {
            entries.add(entry);
        }

        @Override
        public PageResult<LedgerEntry> findByUser(long userId, PageRequest page) {
            List<LedgerEntry> mine = entries.stream()
                    .filter(e -> e.userId() == userId)
                    .collect(Collectors.toList());
            List<LedgerEntry> sliced = mine.stream()
                    .skip(page.offset())
                    .limit(page.size())
                    .collect(Collectors.toList());
            return new PageResult<>(sliced, mine.size(), page.page(), page.size());
        }

        @Override
        public long countByUserAndReason(long userId, ReputationReason reason) {
            return entries.stream().filter(e -> e.userId() == userId && e.reason() == reason).count();
        }

        @Override
        public int sumPositiveVoteDeltaSince(long userId, Instant since) {
            return entries.stream()
                    .filter(e -> e.userId() == userId && e.reason().isVote() && e.delta() > 0)
                    .filter(e -> !e.createdAt().isBefore(since))
                    .mapToInt(LedgerEntry::delta)
                    .sum();
        }

        @Override
        public Map<Long, Long> sumByUser() {
            return entries.stream().collect(Collectors.groupingBy(LedgerEntry::userId,
                    Collectors.summingLong(LedgerEntry::delta)));
        }
    }

    public static class InMemoryBadges implements BadgeRepository {
        public final List<BadgeAward> awards = new ArrayList<>();

        @Override
        public boolean has(long userId, BadgeType type) {
            return awards.stream().anyMatch(a -> a.userId() == userId && a.type() == type);
        }

        @Override
        public void award(BadgeAward award) {
            awards.add(award);
        }

        @Override
        public List<BadgeAward> findByUser(long userId) {
            return awards.stream().filter(a -> a.userId() == userId).collect(Collectors.toList());
        }
    }

    public static class RecordingEvents implements IntegrationEventPublisher {
        public final List<IntegrationEvent> published = new ArrayList<>();

        @Override
        public void publish(IntegrationEvent event) {
            published.add(event);
        }

        public <T extends IntegrationEvent> List<T> of(Class<T> type) {
            return published.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
        }
    }
}
