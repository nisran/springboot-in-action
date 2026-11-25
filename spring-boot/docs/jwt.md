# JWT 专题：令牌设计、签发与生命周期

> 归属：`spring-boot/docs` · 类型：实现规格 · 更新：2026-11-17 · v2.0（去教程化瘦身）
> 上游：[PRD 总纲](jwt-auth-design.md) · 文档地图：[README.md](README.md)
> **本文职责**：Token 层 —— claims、签名密钥、签发/解析、TTL、旋转+复用检测、黑名单三张表、实现约束。
> Filter 如何消费 token → [spring-security.md](spring-security.md)；roles 数据从哪来 → [jpa.md](jpa.md)。

---

## 1. Token 设计

### 1.1 双令牌模型

| | Access Token | Refresh Token |
|---|---|---|
| 用途 | 访问业务接口 | 仅换新令牌，**不能**访问业务接口 |
| 有效期 | 30 分钟 | 7 天 |
| 传输 | `Authorization: Bearer <accessToken>` | `Refresh-Token: <refreshToken>`（自定义头，仅 refresh / logout 使用） |
| 携带 roles | ✅（签发时刻快照） | ❌（刷新时从数据库重载最新角色） |

安全模型：Access 高频传输、泄露窗口大 → 短有效期对冲；Refresh 低频使用 → 单次使用 + 复用检测 + 链撤销兜底（§3）。

Refresh 走自定义头而非 `Authorization`：物理隔离两类凭证，误把 RT 带进业务接口会被 `token_type` 校验直接拦截。登录在**响应体**下发两个 token（不走响应头，避免被响应头日志收集）。

### 1.2 Claims 定义

| Claim | Access | Refresh | 用途 |
|---|:---:|:---:|---|
| `iss` | ✓ | ✓ | 签发方标识，解析时校验，防跨系统 token 串用 |
| `sub` | ✓ | ✓ | userId，刷新时据此重查数据库 |
| `username` | ✓ | ✓ | 仅日志排查友好，**不参与鉴权** |
| `roles` | ✓ | ✗ | Access 自带权限 → Filter 免查库；Refresh 不带 → 刷新拿最新角色 |
| `token_type` | `access` | `refresh` | **验签后必须强校验**（两类 token 同一密钥签出，不校验即互通） |
| `jti` | ✓ | ✓ | UUID。黑名单主键 + 复用检测主键 + 日志追踪 |
| `iat` / `exp` | ✓ | ✓ | 签发/过期时间（秒级 epoch）。链撤销用 `iat` 比较 cutoff |

生效语义：用户改角色/禁用 → 最迟下次刷新生效；登出/盗用 → 黑名单即时生效（§3）。

### 1.3 签出的 token 长什么样

```jsonc
// Header
{ "alg": "HS256", "typ": "JWT" }

// Payload（Access）
{
  "iss": "springboot-in-action",
  "sub": "1002",
  "username": "admin",
  "roles": ["ROLE_ADMIN"],
  "token_type": "access",
  "jti": "3f2a9c1e-8b4d-4e6a-9c2f-7d1b0a5e8f93",
  "iat": 1763270400,
  "exp": 1763272200          // iat + 1800s
}
// Refresh 差异：无 roles、token_type=refresh、exp = iat + 7d
```

---

## 2. 签发与解析（TokenService）

**分层铁律**：`TokenService` 是全模块**唯一直接使用 jjwt API 的类**，其他类面向自建模型（`AccessClaims` / `LoginUser`）。换 JWT 库只改这一个类。

### 2.1 依赖

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### 2.2 密钥管理

```java
// JwtProperties 绑定阶段 fail-fast 校验：解码后不足 32 字节（256bit）拒绝启动
SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
```

- HS256 要求密钥 ≥ 32 字节（短密钥 jjwt 抛 `WeakKeyException`），绑定阶段主动校验并给出清晰报错；
- 密钥经 `jwt.secret: ${JWT_SECRET:...}` 环境变量注入，真实密钥永不进 Git。

### 2.3 签发骨架

```java
public String generateAccessToken(LoginUser user) {
    Instant now = Instant.now();
    return Jwts.builder()
            .setIssuer(jwtProps.getIssuer())
            .setSubject(user.getUserId())
            .claim("username", user.getUsername())
            .claim("roles", user.getRoles())
            .claim("token_type", "access")
            .setId(UUID.randomUUID().toString())             // jti
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(jwtProps.getAccessTokenTtl())))
            .signWith(key, SignatureAlgorithm.HS256)         // 显式算法
            .compact();
}
// generateRefreshToken：去掉 roles，token_type=refresh，TTL 换 refresh-token-ttl
```

### 2.4 解析骨架与异常映射

```java
public AccessClaims parseAccessToken(String token) {
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)                          // 验签密钥
            .requireIssuer(jwtProps.getIssuer())         // iss 校验
            .setAllowedClockSkewSeconds(60)              // 时钟容差
            .setAllowedAlgorithms(...)                   // 显式限定 HS256
            .build()
            .parseClaimsJws(token)                       // ← 会验签
            .getBody();
    if (!"access".equals(claims.get("token_type", String.class))) {
        throw new IncorrectClaimException(...);          // token_type 强校验
    }
    return AccessClaims.from(claims);
}
```

| 异常 | 含义 | 错误码（契约见 [PRD §5](jwt-auth-design.md#5-异常场景契约)） |
|---|---|---|
| `SignatureException` | 签名不符（篡改/密钥不对） | `TOKEN_INVALID` |
| `ExpiredJwtException` | 过期 | `TOKEN_EXPIRED` |
| `MalformedJwtException` | 格式坏（非三段/编码错） | `TOKEN_INVALID` |
| `IncorrectClaimException` | iss / token_type 不符 | `TOKEN_TYPE_MISMATCH` / `TOKEN_INVALID` |
| `IllegalArgumentException` | token 为 null/空串 | `TOKEN_INVALID` |

---

## 3. 生命周期：刷新、撤销与黑名单

### 3.1 有效期矩阵

| 参数 | 值 | 配置项 |
|---|---|---|
| Access TTL | 30m | `jwt.access-token-ttl` |
| Refresh TTL | 7d | `jwt.refresh-token-ttl` |
| 时钟容差 | 60s | `setAllowedClockSkewSeconds`（防多机时钟偏移引发随机 401 抖动） |

### 3.2 旋转与复用检测

- **旋转**：每次刷新同时签发新 AT + 新 RT（TTL 重置完整 7 天）——RT 单次使用，价值随使用即刻转移给下一代；
- **复用检测**：正常客户端只保存最新一代；**旧 RT 再现 = 盗用信号**（备份/泄露/中间人才会让旧 token 复活）；
- **链撤销**：检测到复用时登记用户级截止时刻 `userCutoff[userId] = now`，此后该用户所有 `iat < now` 的 token 一律拒绝——攻击者与正常用户手里的 token 全部作废，强制重新登录。宁错杀一次登录，不放过一条被盗链。

### 3.3 黑名单三张表

| 缓存表 | key → value | 淘汰策略 | 谁写入 | 谁查询 |
|---|---|---|---|---|
| ① `revokedJti` 登出黑名单 | jti → token 过期时刻 | 剩余寿命（自定义 Expiry） | logout | Filter 每请求 |
| ② `usedRefresh` 已用 RT 表 | RT jti → RT 过期时刻 | 同上 | refresh（首次使用） | refresh（复用检测） |
| ③ `userCutoff` 用户级链撤销 | userId → 撤销截止时刻 | expireAfterWrite 8 天 | refresh（检测到复用） | Filter 每请求 |

- **淘汰语义**：条目只需活到 token 自然过期那一刻（之后 `exp` 已拦截，条目失去意义）。Caffeine `Expiry<String, Long>`（value 存过期时刻，`expireAfterCreate` 返回 `expiresAt - now`）恰好表达该语义——缓存天然不膨胀，无需定时清理。统一 TTL 是错的：7d 浪费内存，30m 会让 RT 复用检测漏检；
- **`userCutoff` 固定 8 天**：保护对象中最长寿命是 7 天的 RT，8 天后整条链已死透；上限 `maximumSize(100_000)` 防内存失控；
- **接口化**：`TokenBlacklistService` 接口 + `CaffeineTokenBlacklist` 实现；多实例部署时新增 `RedisTokenBlacklist` 即可，Filter/AuthService 零改动。**Caffeine 版仅适用单实例部署**。

### 3.4 登出调用链

1. 客户端携 `Authorization`（AT）+ `Refresh-Token`（RT）调 `POST /api/auth/logout`；
2. `revoke(AT.jti, AT.exp)` + `revoke(RT.jti, RT.exp)` + `markRefreshUsed(RT.jti, RT.exp)`；
3. 返回 204，客户端删本地存储；
4. 效果：旧 AT → Filter 命中黑名单 → `401 TOKEN_REVOKED`；旧 RT → refresh 查已用表 → `401 REFRESH_INVALID`。**登出即刻生效**。

### 3.5 时序总览

```
签发 ──▶ 使用 ──▶ 刷新(旋转+登记已用) ──▶ 再使用新对 ──▶ …
  │                                                    │
  ├── 登出 ──▶ jti 入黑名单 ──▶ Filter 拒绝(TOKEN_REVOKED)
  ├── RT 复用 ──▶ 链撤销 ──▶ 全家拒绝(TOKEN_REVOKED / REFRESH_REUSE_DETECTED)
  └── 自然过期 ──▶ exp 拦截 ──▶ 黑名单条目到期自动蒸发
```

---

## 4. 实现约束（MUST）

1. claims 只放鉴权必需、可容忍过期的字段（userId / username / roles / token_type / jti / iat / exp）；JWT 不加密（Base64 即可读），绝不放手机号/邮箱/密码哈希等敏感信息；
2. 解析必须用 `parseClaimsJws()`（会验签）；`parseClaims()`（旧版 `parse()`）不验签，禁止用于鉴权路径；
3. 解析显式限定算法集合 HS256，不读 token 自带 `alg`（防 `alg:none` / RS→HS 混淆攻击）；
4. 验签后必须强校验 `token_type`——两类 token 同一密钥签出，不校验即互通；
5. exp 单位：claims 里是秒级 epoch，jjwt `setExpiration(Date)` 收毫秒——手工构造测试 token 时别搞混；
6. 时钟容差 `clockSkewSeconds=60`；
7. claims 不塞大对象（token 每请求随 Header 传输，体积即带宽）；
8. 手写解码 JWT 必须用 Base64Url decoder（`+/=` 已被替换为 `-_`）；
9. 黑名单条目按 token 剩余寿命淘汰（自定义 Expiry），不用统一 TTL；
10. 登出即失效（服务端最小必要状态）是既定决策，优先于无状态洁癖。

---

## 5. 改动同步提醒

改了本文覆盖的内容，按 [README 路由表](README.md#3-同步路由表改了什么--必须同步哪些文档) 同步：

- claims 结构 / TTL / 错误码 → **PRD**（API 契约、异常场景表）
- 黑名单在 Filter 中的调用方式变化 → **spring-security.md** §4
- roles 字段来源（用户表/角色模型）→ **jpa.md**
- 推翻 Q1–Q7 任一决策 → **PRD 决策记录**补记
