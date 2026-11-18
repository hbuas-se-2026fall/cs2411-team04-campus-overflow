package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.RecordingEvents;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BountyServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");
    private static final long QUESTION_ID = 10L;
    private static final long SPONSOR_ID = 9L;
    private static final long WINNER_ID = 3L;
    private static final Actor SPONSOR = new Actor(SPONSOR_ID, "王老师", Role.TEACHER);

    private InMemoryAccounts accounts;
    private InMemoryLedger ledger;
    private InMemoryBounties bounties;
    private RecordingEvents events;
    private RecordingAudit audit;
    private BountyService service;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        accounts.put(ReputationAccount.open(SPONSOR_ID, 1000));
        accounts.put(ReputationAccount.open(WINNER_ID, 0));
        ledger = new InMemoryLedger();
        bounties = new InMemoryBounties();
        events = new RecordingEvents();
        audit = new RecordingAudit();
        InMemoryBadges badges = new InMemoryBadges();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        IdentityApi identity = new StubIdentity(new UserSummary(SPONSOR_ID, "王老师", Role.TEACHER, true, "计算机"));
        ReputationLedgerService ledgerService = new ReputationLedgerService(accounts, ledger,
                new BadgeService(badges, events, clock), events, clock);
        service = new BountyService(bounties, accounts, ledgerService,
                id -> id == QUESTION_ID
                        ? Optional.of(new QuestionRef(id, 1L, 100L, "红黑树", null, false))
                        : Optional.empty(),
                identity, events, audit, clock);
    }

    @Test
    void openFreezesPointsPublishesEventAndExposesLatestView() {
        long bountyId = service.open(SPONSOR, QUESTION_ID, 100, 7);

        ReputationAccount sponsor = accounts.findByUserId(SPONSOR_ID).orElseThrow();
        assertThat(sponsor.availablePoints()).isEqualTo(900);
        assertThat(sponsor.frozenPoints()).isEqualTo(100);
        assertThat(events.of(BountyOpenedEvent.class)).singleElement().satisfies(e -> {
            assertThat(e.bountyId()).isEqualTo(bountyId);
            assertThat(e.points()).isEqualTo(100);
            assertThat(e.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        });
        assertThat(audit.actions).containsExactly("BOUNTY_OPEN");
        assertThat(service.latest(QUESTION_ID)).hasValueSatisfying(view -> {
            assertThat(view.id()).isEqualTo(bountyId);
            assertThat(view.sponsorName()).isEqualTo("王老师");
            assertThat(view.status()).isEqualTo(BountyStatus.OPEN.name());
        });
    }

    @Test
    void secondOpenOnSameQuestionIsRejected() {
        service.open(SPONSOR, QUESTION_ID, 100, 7);

        assertThatThrownBy(() -> service.open(SPONSOR, QUESTION_ID, 50, 7))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("REP-2001"));

        ReputationAccount sponsor = accounts.findByUserId(SPONSOR_ID).orElseThrow();
        assertThat(sponsor.availablePoints()).isEqualTo(900);
        assertThat(sponsor.frozenPoints()).isEqualTo(100);
    }

    @Test
    void acceptanceSpendsFrozenPointsAndAwardsWinner() {
        service.open(SPONSOR, QUESTION_ID, 100, 7);

        service.settleOnAccept(QUESTION_ID, 50L, WINNER_ID, "accept-1");

        ReputationAccount sponsor = accounts.findByUserId(SPONSOR_ID).orElseThrow();
        assertThat(sponsor.availablePoints()).isEqualTo(900);
        assertThat(sponsor.frozenPoints()).isZero();
        assertThat(accounts.findByUserId(WINNER_ID).orElseThrow().reputation()).isEqualTo(100);
        assertThat(ledger.entries).singleElement().satisfies(entry -> {
            assertThat(entry.eventId()).isEqualTo("accept-1");
            assertThat(entry.reason()).isEqualTo(ReputationReason.BOUNTY_AWARDED);
            assertThat(entry.delta()).isEqualTo(100);
        });
        assertThat(events.of(BountyAwardedEvent.class)).singleElement()
                .satisfies(e -> assertThat(e.winnerId()).isEqualTo(WINNER_ID));
    }

    @Test
    void duplicateAcceptanceDoesNotRecordTwice() {
        service.open(SPONSOR, QUESTION_ID, 100, 7);

        service.settleOnAccept(QUESTION_ID, 50L, WINNER_ID, "accept-1");
        service.settleOnAccept(QUESTION_ID, 50L, WINNER_ID, "accept-1");

        assertThat(ledger.entries).hasSize(1);
        assertThat(accounts.findByUserId(WINNER_ID).orElseThrow().reputation()).isEqualTo(100);
        assertThat(accounts.findByUserId(SPONSOR_ID).orElseThrow().frozenPoints()).isZero();
        assertThat(events.of(BountyAwardedEvent.class)).hasSize(1);
    }

    @Test
    void expiredBountyRefundsSponsorAndPublishesClosedEvent() {
        ReputationAccount sponsor = accounts.findByUserId(SPONSOR_ID).orElseThrow();
        sponsor.freezePoints(100);
        accounts.save(sponsor);
        bounties.save(Bounty.open(QUESTION_ID, true, SPONSOR, true, 100, 1, NOW.minus(Duration.ofDays(2))));

        int expired = service.expireDue();

        assertThat(expired).isEqualTo(1);
        assertThat(sponsor.availablePoints()).isEqualTo(1000);
        assertThat(sponsor.frozenPoints()).isZero();
        assertThat(events.of(BountyClosedEvent.class)).singleElement()
                .satisfies(e -> assertThat(e.refundedPoints()).isEqualTo(100));
    }

    private static final class InMemoryBounties implements BountyRepository {
        private final List<Bounty> store = new ArrayList<>();
        private long nextId = 1;

        @Override
        public Optional<Bounty> findOpenByQuestion(long questionId) {
            return store.stream().filter(b -> b.questionId() == questionId && b.status() == BountyStatus.OPEN)
                    .findFirst();
        }

        @Override
        public Optional<Bounty> findLatestByQuestion(long questionId) {
            return store.stream().filter(b -> b.questionId() == questionId).reduce((first, second) -> second);
        }

        @Override
        public List<Bounty> findDue(Instant now, int limit) {
            return store.stream().filter(b -> b.status() == BountyStatus.OPEN && b.isDue(now)).limit(limit).toList();
        }

        @Override
        public Bounty save(Bounty bounty) {
            if (bounty.id() == null) {
                bounty.assignId(nextId++);
            }
            store.removeIf(existing -> existing.id().equals(bounty.id()));
            store.add(bounty);
            return bounty;
        }
    }

    private static final class RecordingAudit implements AuditTrail {
        private final List<String> actions = new ArrayList<>();

        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            actions.add(action);
        }
    }

    private static final class StubIdentity implements IdentityApi {
        private final Map<Long, UserSummary> users = new LinkedHashMap<>();

        private StubIdentity(UserSummary... users) {
            for (UserSummary user : users) {
                this.users.put(user.id(), user);
            }
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
            return users.values().stream().filter(user -> user.displayName().equals(displayName)).findFirst();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return Optional.empty();
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return true;
        }
    }
}
