package com.campusoverflow.identity.infrastructure.security;

import com.campusoverflow.identity.application.SessionRevoker;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring Session principal name 索引的会话清理实现。
 *
 * <p>前提：{@code application.yml} 里 {@code spring.session.redis.repository-type: indexed}
 * 把会话仓库切换成 {@code RedisIndexedSessionRepository}（它实现了
 * {@link FindByIndexNameSessionRepository}）。默认的 {@code RedisSessionRepository} 只实现
 * {@code SessionRepository}，没有按登录名反查的能力。
 *
 * <p>索引由 Spring Security 集成在登录时写入，取值为 {@code authentication.getName()}，
 * 对本项目即 {@code SecurityUser.getUsername()} —— <b>学号/工号，不是昵称</b>
 * （见 {@code SecurityUser.java} 的 {@code getUsername()} 与 {@code toActor()} 的区别）。
 * 传昵称不会报错，只会永远返回空集合，禁用看起来「成功」但会话没清掉。
 *
 * <p>选它而非 {@code SessionRegistry}：{@code SessionRegistryImpl} 是纯内存注册表，
 * 应用一重启就空，重启前登录的会话根本不在表里，禁用时一个也清不掉；而这里直接查 Redis 索引，
 * 重启后依然有效。
 *
 * <p><b>已知限制：并发竞态。</b>登录与禁用同时发生时存在一个毫秒级的窗口——若某次登录的
 * 索引写入晚于本方法的 {@code findByPrincipalName} 扫描，那个会话不会被清掉，
 * 用户仍可用它访问直到会话自然过期（{@code spring.session.timeout: 30m}）。
 * 不做每请求校验的原因：那需要在认证链路上每次访问都查一次账号状态，
 * 与「会话即凭证」的设计相冲突，且会把数据库压力从「登录时一次」放大到「每次请求一次」。
 * 若要收紧，正确做法是在会话里记录 {@code status} 快照并在定期任务里核对，
 * 而不是在 {@link #revokeAll} 上继续加逻辑。
 */
@Component
public class RedisSessionRevoker implements SessionRevoker {

    private static final Logger log = LoggerFactory.getLogger(RedisSessionRevoker.class);

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public RedisSessionRevoker(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public int revokeAll(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        Map<String, ? extends Session> found = sessions.findByPrincipalName(username);
        if (found == null || found.isEmpty()) {
            return 0;
        }
        // 先固化 key 集合再删除：直接在 findByPrincipalName 的返回值上边遍历边删，
        // 某些 SessionRepository 实现返回的是底层 Map 的视图，会抛 ConcurrentModificationException。
        for (String sessionId : found.keySet().toArray(new String[0])) {
            sessions.deleteById(sessionId);
        }
        log.info("已清理用户 {} 的 {} 个会话", username, found.size());
        return found.size();
    }
}
