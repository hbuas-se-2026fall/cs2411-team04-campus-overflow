package com.campusoverflow.identity.application;

/**
 * 会话清理端口（实现：Spring Session Redis 的 principal name 索引）。
 *
 * <p>管理员禁用账号后必须让该用户的既有会话立即失效，否则「禁用」只是一行状态，
 * 用户凭手里的旧 Cookie 仍能继续操作，直到会话自然超时（默认 30 分钟）。
 *
 * <p>放在应用层而不是直接依赖 Spring Session 的 {@code FindByIndexNameSessionRepository}，
 * 一是为了让领域与用例不感知会话存储，二是让 {@code AdminUserService} 的单测可以用手写 Fake 替换。
 * 与 {@link LoginThrottle} 同一惯例：应用层定义接口，基础设施层给实现。
 */
public interface SessionRevoker {

    /**
     * 使指定登录名（学号/工号，即 {@code User.username()}）名下的全部会话失效。
     *
     * @return 实际失效的会话数量，用于审计与日志
     */
    int revokeAll(String username);
}
