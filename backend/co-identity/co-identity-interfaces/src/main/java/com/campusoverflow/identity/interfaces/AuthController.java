package com.campusoverflow.identity.interfaces;

import com.campusoverflow.identity.application.AuthenticationAuditService;
import com.campusoverflow.identity.application.LoginThrottle;
import com.campusoverflow.identity.application.MeView;
import com.campusoverflow.identity.application.RegisterUserCommand;
import com.campusoverflow.identity.application.UpdateProfileCommand;
import com.campusoverflow.identity.application.UserCommandService;
import com.campusoverflow.identity.application.UserQueryService;
import com.campusoverflow.identity.application.UserRegistrationService;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.ActorAware;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。采用服务端会话（Spring Session + Redis）+ HttpOnly Cookie，而非长效 JWT（见 ADR-003）。
 * 登出由安全过滤链处理：POST /api/v1/auth/logout。
 */
@Tag(name = "认证 Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRegistrationService registration;
    private final UserQueryService userQuery;
    private final UserCommandService userCommand;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginThrottle throttle;
    private final AuthenticationAuditService authAudit;

    public AuthController(UserRegistrationService registration, UserQueryService userQuery,
                          UserCommandService userCommand,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository, LoginThrottle throttle,
                          AuthenticationAuditService authAudit) {
        this.registration = registration;
        this.userQuery = userQuery;
        this.userCommand = userCommand;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.throttle = throttle;
        this.authAudit = authAudit;
    }

    @Operation(summary = "获取 CSRF Token（同时写入 XSRF-TOKEN Cookie）")
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @Operation(summary = "学生注册")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Long>> register(@Valid @RequestBody RegisterRequest req) {
        long id = registration.register(new RegisterUserCommand(req.username(), req.displayName(), req.email(),
                req.password(), req.college(), req.className()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @Operation(summary = "登录（学号/工号 + 密码）")
    @PostMapping("/login")
    public MeView login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                        HttpServletResponse response) {
        if (throttle.isBlocked(req.username())) {
            authAudit.loginFailed(req.username(), "THROTTLED");
            throw new LockedException("登录失败次数过多，请 1 分钟后再试");
        }
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.username(), req.password()));
        } catch (AuthenticationException e) {
            throttle.recordFailure(req.username());
            authAudit.loginFailed(req.username(), e.getClass().getSimpleName());
            throw new BadCredentialsException("账号或密码错误");
        }
        throttle.reset(req.username());
        // 防御会话固定攻击：认证成功后轮换会话 ID
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        Actor actor = ((ActorAware) auth.getPrincipal()).toActor();
        authAudit.loginSucceeded(actor.userId(), req.username());
        return userQuery.me(actor.userId());
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public MeView me(@Parameter(hidden = true) Actor actor) {
        return userQuery.me(actor.userId());
    }

    /**
     * 修改自己的资料。路径用 {@code /auth/me} 而非 {@code /users/{id}}：URL 里不带任何用户 id，
     * 方法签名也就没有「改谁」这个参数，从接口层面杜绝越权改他人资料。
     *
     * <p>返回 204 而不是新的 {@code MeView}：前端提交成功后本来就要重新拉 {@code /auth/me} 刷新
     * {@code auth.tsx} 里的全局状态（否则顶栏还显示旧昵称），返回体用不上。少一处数据契约也少一处
     * 与 {@code MeView} 同步的负担。
     */
    @Operation(summary = "修改自己的资料（昵称 / 学院 / 班级）")
    @PutMapping("/me")
    public ResponseEntity<Void> updateProfile(@Parameter(hidden = true) Actor actor,
                                             @Valid @RequestBody ProfileUpdateRequest req) {
        userCommand.updateProfile(actor, new UpdateProfileCommand(req.displayName(), req.college(), req.className()));
        return ResponseEntity.noContent().build();
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 20) String username,
            @NotBlank @Size(min = 2, max = 20) String displayName,
            @NotBlank @Size(max = 100) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @Size(max = 50) String college,
            @Size(max = 50) String className) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /**
     * 只有这三个字段可改：学号是身份、角色归管理员、认证走审核流程，都不属于「个人资料」。
     *
     * <p>部分更新语义：{@code null} = 本次不改这一项，空串 = 清空这一项。
     *
     * <p>{@code displayName} <b>故意不加</b> {@code @Size}：昵称的 2–20 字、不含 {@code @}/空格规则
     * 由 {@code User.updateProfile} → {@code Guard.requireText} 执行，错误码是 {@code ID-1002}。
     * 若这里再加 {@code @Size(min = 2)}，同一个「昵称太短」会有两种失败方式——空串被 Bean Validation
     * 拦成 400 {@code SYS-400-VALIDATION}、{@code null} 之外的过短值走领域层得到 422 {@code ID-1002}，
     * 前端得为同一个问题处理两套码。去掉后领域层是唯一出口。
     *
     * <p>{@code college} / {@code className} 保留 {@code @Size(max = 50)}：领域层的
     * {@code User.optional(value, 50)} 对超长值是<b>静默截断</b>而不是报错，所以「超长」这条规则
     * 在领域层没有错误码可统一。留在这里报 400，比悄悄截掉用户输入的 10 个字要好。
     */
    public record ProfileUpdateRequest(String displayName,
                                       @Size(max = 50) String college,
                                       @Size(max = 50) String className) {
    }
}
