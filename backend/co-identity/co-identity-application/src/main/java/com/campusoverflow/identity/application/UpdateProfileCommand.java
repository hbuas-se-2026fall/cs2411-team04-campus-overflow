package com.campusoverflow.identity.application;

/**
 * 修改个人资料的入参，<b>部分更新语义</b>。
 *
 * <p>{@code null} = 「这一项本次不修改」，空串 = 「清空这一项」。两者必须分开，否则调用方
 * 无法只改一个字段：若 {@code null} 也当成清空，少传一个字段就等于静默删掉用户的资料。
 * {@link UserCommandService#updateProfile} 负责把 {@code null} 换成库里现值。
 *
 * <p>字段上没有 Bean Validation 的长度注解：昵称 2–20 字、学院/班级 ≤50 字的规则由
 * {@code User.updateProfile} → {@code Guard.requireText} 统一执行，错误码统一是 {@code ID-1002}。
 * 若这里再加 {@code @Size}，同一条规则会多出一种 {@code SYS-400-VALIDATION} 的失败方式，
 * 前端得对同一个问题处理两套码。
 *
 * <p>注意没有 {@code username} / {@code role} / {@code verified} / {@code email} 字段：
 * 学号是身份本身，角色归管理员，认证走审核流程，邮箱是登录凭据——都不属于「个人资料」。
 */
public record UpdateProfileCommand(String displayName, String college, String className) {
}
