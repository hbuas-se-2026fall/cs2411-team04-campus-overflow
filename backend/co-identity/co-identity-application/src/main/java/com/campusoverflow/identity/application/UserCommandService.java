package com.campusoverflow.identity.application;

import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：本人维护自己的资料。
 *
 * <p>与 {@link AdminUserService} 的分界：管理员改的是「别人的角色与状态」（授权动作），
 * 这里改的是「自己的展示信息」（自助动作）。两者都不能改学号/工号，那是身份本身。
 */
@Service
public class UserCommandService {

    private final UserRepository users;
    private final AuditTrail audit;

    public UserCommandService(UserRepository users, AuditTrail audit) {
        this.users = users;
        this.audit = audit;
    }

    /**
     * 修改当前登录用户的昵称、学院、班级。
     *
     * <p>昵称全局唯一（{@code User} 的类注释写明它用于 {@code @提及}），所以这里比领域层多做一步：
     * 先看新昵称是否已被别人占用。{@code existsByDisplayName} 分不清「被别人占了」和「就是我自己」，
     * 因此改成先 {@code findByDisplayName} 再比 id——否则用户什么都不改、只改学院也会被判成重名。
     *
     * <p>只写审计、不发领域事件：昵称变更不影响任何聚合的一致性，检索与通知都在读取时实时联表取
     * {@code displayName}，没有需要同步的物化副本。若以后出现「通知正文里快照了昵称」之类的设计，
     * 才需要补一个 {@code UserProfileUpdatedEvent}。
     */
    @Transactional
    public void updateProfile(Actor actor, UpdateProfileCommand cmd) {
        Objects.requireNonNull(actor, "actor");
        User user = users.findById(actor.userId())
                .orElseThrow(() -> new NotFoundException("ID-4001", "用户不存在"));

        String newDisplayName = cmd.displayName() == null ? null : cmd.displayName().strip();
        if (newDisplayName != null && !newDisplayName.equals(user.displayName())) {
            Optional<User> holder = users.findByDisplayName(newDisplayName);
            if (holder.isPresent() && !holder.get().id().equals(user.id())) {
                throw new ConflictException("ID-2002", "该昵称已被使用");
            }
        }

        user.updateProfile(newDisplayName, cmd.college(), cmd.className());
        users.save(user);
        audit.record("PROFILE_UPDATED", actor.userId(), "USER", String.valueOf(actor.userId()),
                user.displayName());
    }
}
