# Spring Security 专题：过滤器链与全局认证授权

> 归属：`spring-boot/docs` · 类型：实现规格 · 更新：2026-11-17 · v2.0（去教程化瘦身）
> 上游：[PRD 总纲](jwt-auth-design.md) · 文档地图：[README.md](README.md)
> **本文职责**：认证授权层 —— SecurityFilterChain 配置、JwtAuthenticationFilter、登录认证路径、授权（URL/方法级）、SecurityContext 全局取用户、401/403 统一响应、实现约束。
> Token 怎么签发怎么验 → [jwt.md](jwt.md)；用户数据从哪来 → [jpa.md](jpa.md)。

---

## 1. 架构要点

- Spring Security = **一串 Servlet Filter + 一个 SecurityContext**。`DelegatingFilterProxy`（Boot 自动注册为 `springSecurityFilterChain`）把容器 Filter 委派给 Spring 容器的 `FilterChainProxy`，后者按 `requestMatchers` 选中我们 `@Bean` 定义的 `SecurityFilterChain`；
- 链内 Filter **有序**：`addFilterBefore(x, UsernamePasswordAuthenticationFilter.class)` = 插到该 slot 之前；
- 认证侧对象链（登录走这条）：

```
AuthenticationManager（实现为 ProviderManager）
   └─▶ DaoAuthenticationProvider（用户名+密码专用；容器里有 UserDetailsService + PasswordEncoder Bean 时自动装配）
        ├─ retrieveUser() → UserDetailsService.loadUserByUsername()
        ├─ pre-check      → enabled / locked / expired（false → DisabledException 等内置异常）
        ├─ 密码校验       → PasswordEncoder.matches(raw, encoded)（BCrypt）
        └─ 成功           → UsernamePasswordAuthenticationToken(principal=LoginUser, authorities=角色)
```

- `UserDetails`（本项目叫 `LoginUser`）：框架眼中的用户，含 enabled/locked/expired 布尔——**禁用账号是框架原生行为，不自写 if**；
- `Authentication`：认证结果。principal = LoginUser、authorities = 权限列表——正是 Filter 造对象的模板：从 JWT 造出 `Authentication` 放进 SecurityContext，此后授权判定、`@PreAuthorize`、全局取用户全是框架原生能力。

---

## 2. SecurityContext 与异步

- `SecurityContextHolder` 默认 ThreadLocal 存储：同请求内 Controller / Service / Repository 随取随用，零传参。`SecurityContextHolderFilter` 在请求结束时**强制清除**——线程池复用时不清会串号（下一请求读到上一用户身份）；
- **`@Async` 里取不到**：本模块 `EventAsyncConfig` 的线程池是另一批线程，ThreadLocal 不跨线程。解法——`DelegatingSecurityContextAsyncTaskExecutor` 包装（提交时捕获 Context、子线程执行前恢复、执行后清除）：

```java
@Bean
public ThreadPoolTaskExecutor eventExecutor() {
    ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
    delegate.setCorePoolSize(4);
    delegate.initialize();
    return new DelegatingSecurityContextAsyncTaskExecutor(delegate);  // ← 包装
}
```

---

## 3. 本项目过滤器链设计

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ① 打开方法级安全（@PreAuthorize）
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)                       // ②
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // ③
            .authorizeHttpRequests(auth -> auth                          // ④ URL 授权规则（顺序敏感）
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)      // ⑤ 未认证 → 401 JSON
                .accessDeniedHandler(accessDeniedHandler))               //    权限不足 → 403 JSON
            .addFilterBefore(jwtAuthenticationFilter,                    // ⑥ 我们的过滤器
                             UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean  // 登录接口用：接收用户名密码触发 §5 的认证流程
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

| # | 配置 | 要点 |
|---|---|---|
| ① | `@EnableMethodSecurity` | 打开 `@PreAuthorize/@PostAuthorize`；方法必须 public 且经 Spring 代理调用 |
| ② | CSRF 关闭 | 前提：凭证走 Header、无 Cookie 会话 → 无攻击面 |
| ③ | STATELESS | 不创建/使用 HttpSession；SecurityContext 不入 Session |
| ④ | 规则顺序 | `requestMatchers` 自上而下**首个匹配生效**，`anyRequest()` 永远最后 |
| ⑤ | 两个 Handler | 全站 401/403 收敛为统一 JSON（§8） |
| ⑥ | 插入位置 | 认证 Filter 区段之前，保证授权判定时 Authentication 已就位 |

> **`permitAll` ≠ Filter 跳过该请求**。Filter 永远运行（有 token 就翻译）；`permitAll` 只是"翻译不翻译都放行"——公开接口带脏 token 不报错正是依赖这一点。

---

## 4. JwtAuthenticationFilter

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;          // jwt.md §2
    private final TokenBlacklistService blacklist;    // jwt.md §3（机制归属 jwt.md）

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveBearerToken(req);       // "Authorization: Bearer xxx"，无则 null
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AccessClaims claims = tokenService.parseAccessToken(token);   // 验签/过期/类型
                if (blacklist.isRevoked(claims.jti())                        // ① 登出黑名单
                        || blacklist.revokedBefore(claims.userId(), claims.iat())) { // ② 链撤销
                    req.setAttribute(AuthErrorAttributes.ATTR, "TOKEN_REVOKED");
                    SecurityContextHolder.clearContext();
                } else {
                    var authorities = claims.roles().stream()
                            .map(SimpleGrantedAuthority::new).toList();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.toLoginUser(), null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                req.setAttribute(AuthErrorAttributes.ATTR, resolveErrorCode(e));  // TOKEN_EXPIRED 等
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);   // 永远继续走链
    }
}
```

设计规则：

1. **只翻译，不拦截**：Filter 从不写响应、不决定放行——拒绝动作由授权环节 + 两个 Handler 完成；
2. **失败静默降级**：解析失败只 `clearContext()` + 错误码塞 request attribute，链继续——受保护接口随后因无 Authentication 触发 EntryPoint → 401；
3. `OncePerRequestFilter` 保证转发/include 只执行一次；`getAuthentication() == null` 防与其他认证机制叠加覆盖。

**错误码传递**：Filter 把 `TOKEN_EXPIRED` / `TOKEN_INVALID` / `TOKEN_TYPE_MISMATCH` / `TOKEN_REVOKED` 放入 `request.setAttribute(AuthErrorAttributes.ATTR, code)`，EntryPoint 读出组装进响应体 `error` 字段——不在 Filter 层直接写响应（会绕过统一出口，格式分裂）。

---

## 5. 登录认证路径

登录**不走** JwtAuthenticationFilter——登录是"用密码换凭证"，Filter 是"验凭证"，方向相反。

```java
@PostMapping("/api/auth/login")
public TokenResponse login(@Valid @RequestBody LoginRequest req) {
    try {
        var authToken = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        Authentication auth = authenticationManager.authenticate(authToken);   // ↓ §1 认证链
        LoginUser user = (LoginUser) auth.getPrincipal();
        return authService.issueTokens(user);                                  // 签发双 token（jwt.md §2.3）
    } catch (BadCredentialsException e) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "用户名或密码错误");
    } catch (DisabledException e) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_DISABLED", "账号已禁用");
    }
}
```

不在 Controller 手写"查库 + 比对密码"：框架路径免费附带 enabled/锁定检查、密码编码器抽象、防用户枚举（`DaoAuthenticationProvider` 默认把 `UsernameNotFoundException` 转成 `BadCredentialsException`，报文无"用户不存在 vs 密码错误"差异——错误码里**故意没有 USER_NOT_FOUND**）。

---

## 6. 授权：URL 规则与方法级安全

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")     // URL 层粗粒度
    .anyRequest().authenticated())
```

```java
@PreAuthorize("hasRole('ADMIN')")                          // 单角色
@PreAuthorize("hasAnyRole('ADMIN','USER')")                // 任一
@PreAuthorize("authentication.principal.userId == #id")    // SpEL 引用方法参数（行级校验）
@PostAuthorize("returnObject.owner == authentication.name") // 返回值校验
```

- URL 层粗粒度挡面 + `@PreAuthorize` 细粒度控点，二者可叠加；
- **hasRole vs hasAuthority**：`hasRole('ADMIN')` 自动补 `ROLE_` 前缀匹配 `ROLE_ADMIN`；`hasAuthority()` **不补**（`hasAuthority("ADMIN")` 对 `ROLE_ADMIN` 永远 false）。约定：角色语义用 `hasRole`，权限点（如 `user:delete`）用 `hasAuthority` 且权限串不带前缀。

---

## 7. 全局取当前用户

```java
// ① Controller 参数注入（推荐：零样板、类型安全、天然可测）
@GetMapping("/api/auth/me")
public UserInfo me(@AuthenticationPrincipal LoginUser user) {
    return new UserInfo(user.getUserId(), user.getUsername(), user.getRoles());
}

// ② 任意代码位置（Service/异步）：AuthContext 工具类
public final class AuthContext {
    public static LoginUser currentUser() {                    // 未认证 → null
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof LoginUser u) ? u : null;
    }
    public static LoginUser requireCurrentUser() {             // 未认证 → 直接 401
        LoginUser u = currentUser();
        if (u == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "未认证");
        return u;
    }
    public static boolean hasRole(String role) {               // 内部补 ROLE_ 前缀
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
```

选择：Controller 用 ①；Service 用 ② 的 `requireCurrentUser()`（显式表达"这里必须有用户"）；声明式需求直接 `@PreAuthorize`。异步线程先配 §2 的 Delegating 包装，再谈取值。

---

## 8. 401 / 403 统一响应

| 出口 | 触发条件 | HTTP | `error` |
|---|---|:---:|---|
| `RestAuthenticationEntryPoint` | 到达授权环节时**没有** Authentication（未登录/翻译失败/黑名单拒绝） | 401 | attribute 里的细分码（缺省 `UNAUTHORIZED`） |
| `RestAccessDeniedHandler` | **已认证**但权限不足 | 403 | `ACCESS_DENIED` |

```java
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e)
            throws IOException {
        String code = (String) req.getAttribute(AuthErrorAttributes.ATTR);
        ApiError.write(res, HttpStatus.UNAUTHORIZED,
                code != null ? code : "UNAUTHORIZED", req.getRequestURI());
    }
}
// RestAccessDeniedHandler 同构：403 ACCESS_DENIED
```

响应体契约见 [PRD §5](jwt-auth-design.md#5-异常场景契约)。

> **匿名访问 admin 接口 → 401 不是 403**：无 token 时框架判"未认证"而非"权限不足"，走 EntryPoint。测 403 必须先带有效 token 认证成功、仅角色不够（T13 用例的设计原因）。

---

## 9. 实现约束（MUST）

1. Filter 只翻译不拦截，**绝不直接写 401 响应**——错误码经 request attribute 交 EntryPoint 统一出口；
2. `permitAll` 是授权规则不是过滤器开关，Filter 对所有请求执行；
3. `requestMatchers` 首个匹配生效，宽规则在前会吞掉窄规则；`anyRequest()` 永远最后；
4. `hasRole` 补 `ROLE_` 前缀、`hasAuthority` 不补；claims 存 `ROLE_ADMIN`；
5. `@PreAuthorize` 走 Spring 代理：同类内部自调用（`this.method()`）注解失效，方法必须 public；
6. `AccessDeniedHandler` 只对**已认证**用户生效，匿名 → 401（EntryPoint）；
7. 异步取用户必须 `DelegatingSecurityContextAsyncTaskExecutor` 包装；禁用 `MODE_INHERITABLETHREADLOCAL`（线程池复用出脏数据）；
8. CSRF 关闭的前提是凭证不走 Cookie；若未来改 Cookie 方案必须重开 CSRF；
9. `@EnableWebSecurity` 必须显式声明（否则自定义链后 Boot 自动配置整体回退）；
10. 错误码不设 `USER_NOT_FOUND`（防用户枚举）；
11. `addFilterBefore` 基准选 `UsernamePasswordAuthenticationFilter.class`（插到授权 Filter 之后 = 白做）。

---

## 10. 改动同步提醒

改了本文覆盖的内容，按 [README 路由表](README.md#3-同步路由表改了什么--必须同步哪些文档) 同步：

- 白名单 URL / 错误码产生逻辑 / 401·403 行为 → **PRD**（异常场景表、API 契约）
- 黑名单三张表与策略变化 → **jwt.md** §3（机制归属在那里，本文只写接入点）
- LoginUser 映射来源变化 → **jpa.md** §3
- 新增接口 → **PRD API 契约** + 本文对应鉴权注解说明
