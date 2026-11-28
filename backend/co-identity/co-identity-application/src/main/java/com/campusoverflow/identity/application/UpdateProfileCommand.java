package com.campusoverflow.identity.application;

/**
 * 修改个人资料的入参。
 *
 * <p>三个字段都允许 {@code null}——{@code null} 表示「本次不修改这一项」还是「清空这一项」，
 * 这里取前者：只有显式传入空串才代表清空（{@code User.updateProfile} 会把空白串归一成 {@code null}）。
 * 之所以不让前端只传变化项，是因为这张表单总是整表提交，三字段齐全比「猜哪几个字段存在」更好懂。
 *
 * <p>注意没有 {@code username} / {@code role} / {@code verified} / {@code email} 字段：
 * 学号是身份本身，角色归管理员，认证走审核流程，邮箱是登录凭据——都不属于「个人资料」。
 */
public record UpdateProfileCommand(String displayName, String college, String className) {
}
