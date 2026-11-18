package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReputationQueryService {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final BadgeRepository badges;
    private final IdentityApi identity;

    public ReputationQueryService(ReputationAccountRepository accounts, LedgerRepository ledger,
                                  BadgeRepository badges, IdentityApi identity) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.badges = badges;
        this.identity = identity;
    }

    public ReputationProfileView profile(long userId, Actor viewerOrNull) {
        ReputationAccount account = accounts.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("REP-4001", "用户声誉档案不存在：" + userId));
        String displayName = identity.findUser(userId).map(UserSummary::displayName).orElse("已注销用户");
        boolean canSeePoints = viewerOrNull != null && (viewerOrNull.is(userId) || viewerOrNull.isAdmin());
        Integer availablePoints = canSeePoints ? account.availablePoints() : null;
        Integer frozenPoints = canSeePoints ? account.frozenPoints() : null;
        List<ReputationProfileView.BadgeView> badgeViews = badges.findByUser(userId).stream()
                .map(award -> new ReputationProfileView.BadgeView(award.type().name(), award.type().displayName(),
                        award.type().description(), award.awardedAt()))
                .toList();
        return new ReputationProfileView(userId, displayName, account.reputation(), availablePoints, frozenPoints,
                badgeViews);
    }

    /** 声誉流水属于个人数据：仅本人与管理员可查看。 */
    public PageResult<LedgerEntryView> ledger(long userId, Actor viewer, PageRequest page) {
        if (viewer == null || (!viewer.is(userId) && !viewer.isAdmin())) {
            throw new ForbiddenException("REP-3002", "无权查看他人声誉流水");
        }
        return ledger.findByUser(userId, page).map(entry -> new LedgerEntryView(entry.id(), entry.reason().name(),
                entry.reason().label(), entry.delta(), entry.refType(), entry.refId(), entry.createdAt()));
    }
}
