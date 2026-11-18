package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReputationReadServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");
    private static final long USER_ID = 1L;
    private static final long OTHER_ID = 2L;
    private static final Actor OWNER = new Actor(USER_ID, "小明", Role.STUDENT);
    private static final Actor OTHER = new Actor(OTHER_ID, "小红", Role.STUDENT);
    private static final Actor ADMIN = new Actor(99L, "管理员", Role.ADMIN);

    private InMemoryAccounts accounts;
    private InMemoryLedger ledger;
    private InMemoryBadges badges;
    private ReputationQueryService queries;
    private ReputationApiImpl api;
    private RecordingLeaderboardStore store;
    private LeaderboardService leaderboard;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        accounts.put(ReputationAccount.reconstitute(USER_ID, 100, 80, 20, 0));
        accounts.put(ReputationAccount.reconstitute(OTHER_ID, 40, 0, 0, 0));
        ledger = new InMemoryLedger();
        badges = new InMemoryBadges();
        IdentityApi identity = new StubIdentity(Map.of(
                USER_ID, new UserSummary(USER_ID, "小明", Role.STUDENT, true, "计算机"),
                OTHER_ID, new UserSummary(OTHER_ID, "小红", Role.STUDENT, false, "计算机")));
        queries = new ReputationQueryService(accounts, ledger, badges, identity);
        api = new ReputationApiImpl(accounts);
        store = new RecordingLeaderboardStore();
        Set<String> processed = new HashSet<>();
        leaderboard = new LeaderboardService(store, accounts, identity,
                (eventId, consumer) -> processed.add(consumer + ":" + eventId),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void profileShowsPointsOnlyToOwnerAndAdmin() {
        badges.award(new BadgeAward(USER_ID, BadgeType.FIRST_ANSWER, NOW));

        ReputationProfileView own = queries.profile(USER_ID, OWNER);
        assertThat(own.displayName()).isEqualTo("小明");
        assertThat(own.availablePoints()).isEqualTo(80);
        assertThat(own.frozenPoints()).isEqualTo(20);
        assertThat(own.badges()).singleElement().satisfies(badge -> {
            assertThat(badge.code()).isEqualTo("FIRST_ANSWER");
            assertThat(badge.name()).isEqualTo("初来乍到");
            assertThat(badge.description()).isEqualTo("提交第一个回答");
            assertThat(badge.awardedAt()).isEqualTo(NOW);
        });
        assertThat(queries.profile(USER_ID, ADMIN).availablePoints()).isEqualTo(80);

        ReputationProfileView other = queries.profile(USER_ID, OTHER);
        assertThat(other.availablePoints()).isNull();
        assertThat(other.frozenPoints()).isNull();
    }

    @Test
    void profileRejectsMissingAccount() {
        assertThatThrownBy(() -> queries.profile(999L, ADMIN))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.code()).isEqualTo("REP-4001"));
    }

    @Test
    void ledgerIsPrivateAndMapsChineseReasonLabels() {
        ledger.append(new LedgerEntry(11L, USER_ID, "event-1", ReputationReason.ANSWER_ACCEPTED,
                15, "ANSWER", 50L, NOW));

        assertThatThrownBy(() -> queries.ledger(USER_ID, OTHER, new PageRequest(1, 20)))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.code()).isEqualTo("REP-3002"));
        assertThat(queries.ledger(USER_ID, ADMIN, new PageRequest(1, 20)).items())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.id()).isEqualTo(11L);
                    assertThat(entry.reason()).isEqualTo("ANSWER_ACCEPTED");
                    assertThat(entry.reasonLabel()).isEqualTo("回答被采纳");
                    assertThat(entry.delta()).isEqualTo(15);
                });
    }

    @Test
    void summaryDefaultsToZeroAndBatchOmitsMissingAccounts() {
        assertThat(api.summary(999L).reputation()).isZero();
        assertThat(api.reputations(List.of(USER_ID, 999L)))
                .containsOnlyKeys(USER_ID)
                .containsEntry(USER_ID, 100);
    }

    @Test
    void leaderboardRanksAndHydratesDisplayNames() {
        store.entries = List.of(new LeaderboardStore.Entry(USER_ID, 100), new LeaderboardStore.Entry(OTHER_ID, 40));

        List<LeaderboardEntryView> top = leaderboard.top(LeaderboardPeriod.WEEK, 2);

        assertThat(top).extracting(LeaderboardEntryView::rank).containsExactly(1, 2);
        assertThat(top).extracting(LeaderboardEntryView::displayName).containsExactly("小明", "小红");
        assertThat(top).extracting(LeaderboardEntryView::verified).containsExactly(true, false);
    }

    @Test
    void leaderboardEventIsProcessedOnlyOnce() {
        ReputationChangedEvent event = new ReputationChangedEvent("reputation-1", NOW, USER_ID, 10, 110,
                "ANSWER_VOTE");

        leaderboard.on(event);
        leaderboard.on(event);

        assertThat(store.increments).singleElement().satisfies(increment -> {
            assertThat(increment.userId()).isEqualTo(USER_ID);
            assertThat(increment.delta()).isEqualTo(10);
            assertThat(increment.at()).isEqualTo(NOW);
        });
    }

    @Test
    void allTimeLeaderboardReadsAccountsInsteadOfRedis() {
        List<LeaderboardEntryView> top = leaderboard.top(LeaderboardPeriod.ALL, 2);

        assertThat(top).extracting(LeaderboardEntryView::userId).containsExactly(USER_ID, OTHER_ID);
        assertThat(store.topCalls).isZero();
    }

    @Test
    void reconciliationFindsAccountAndLedgerMismatch() {
        InMemoryAccounts mismatchedAccounts = new InMemoryAccounts();
        mismatchedAccounts.put(ReputationAccount.reconstitute(USER_ID, 100, 0, 0, 0));
        InMemoryLedger mismatchedLedger = new InMemoryLedger();
        mismatchedLedger.append(LedgerEntry.of(USER_ID, "event-90", ReputationReason.ANSWER_VOTE,
                90, "ANSWER", 50L, NOW));
        ReconciliationService reconciliation = new ReconciliationService(mismatchedAccounts, mismatchedLedger);

        assertThat(reconciliation.reconcile())
                .containsExactly(new ReconciliationService.Mismatch(USER_ID, 100, 90));
    }

    private record Increment(long userId, int delta, Instant at) {
    }

    private static final class RecordingLeaderboardStore implements LeaderboardStore {
        private List<Entry> entries = List.of();
        private final List<Increment> increments = new ArrayList<>();
        private int topCalls;

        @Override
        public void increment(long userId, int delta, Instant at) {
            increments.add(new Increment(userId, delta, at));
        }

        @Override
        public List<Entry> top(LeaderboardPeriod period, Instant at, int limit) {
            topCalls++;
            return entries;
        }
    }

    private static final class StubIdentity implements IdentityApi {
        private final Map<Long, UserSummary> users;

        private StubIdentity(Map<Long, UserSummary> users) {
            this.users = users;
        }

        @Override
        public Optional<UserSummary> findUser(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
            Map<Long, UserSummary> result = new LinkedHashMap<>();
            userIds.forEach(id -> Optional.ofNullable(users.get(id)).ifPresent(user -> result.put(id, user)));
            return result;
        }

        @Override
        public Optional<UserSummary> findByDisplayName(String displayName) {
            return Optional.empty();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return Optional.empty();
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return false;
        }
    }
}
