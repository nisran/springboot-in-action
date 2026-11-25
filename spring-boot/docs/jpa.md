# JPA 专题：用户数据层

> 归属：`spring-boot/docs` · 类型：实现规格 · 更新：2026-11-17 · v2.0（去教程化瘦身）
> 上游：[PRD 总纲](jwt-auth-design.md) · 文档地图：[README.md](README.md)
> **本文职责**：数据层 —— users/user_roles 模型、Repository、用户加载（UserDetailsService）、种子数据、ddl-auto 与 PG 配置、H2 测试替身、实现约束。
> claims 里的 roles 怎么进 token → [jwt.md](jwt.md)；加载后的用户怎么被认证 → [spring-security.md](spring-security.md)。

---

## 1. 数据模型

### 1.1 表结构（`ddl-auto: update` 自动生成）

```sql
users                       -- 注意：不是 user（§7 约束 1）
├── id          BIGSERIAL PK
├── username    VARCHAR(255) UNIQUE NOT NULL
├── password    VARCHAR(255) NOT NULL   -- BCrypt 摘要，永远不存明文
└── enabled     BOOLEAN                 -- Spring Security 原生禁用开关

user_roles                  -- @ElementCollection 生成的从表
├── user_id     BIGINT FK → users.id
└── role        VARCHAR(255)           -- 枚举名存字符串："ROLE_USER" / "ROLE_ADMIN"
```

### 1.2 UserEntity

```java
@Entity
@Table(name = "users")                                   // ①
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ②
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;                             // ③ BCrypt 摘要

    private boolean enabled = true;                      // ④

    @ElementCollection(fetch = FetchType.EAGER)          // ⑤
    @CollectionTable(name = "user_roles",
                     joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)                         // ⑥
    private Set<AppRole> roles;
}

public enum AppRole { ROLE_USER, ROLE_ADMIN }
```

| # | 注解 | 约束理由 |
|---|---|---|
| ① | `@Table(name="users")` | `user` 是 PostgreSQL 保留字，必须显式改名 |
| ② | `IDENTITY` | PG 用 `BIGSERIAL` 自增；无分布式 ID 需求 |
| ③ | password 列 | 只存 BCrypt 摘要；编码发生在 Service 层 `passwordEncoder.encode` |
| ④ | `enabled` | `UserDetails.isEnabled()` 原生消费：false → `DisabledException` → 401 `USER_DISABLED`，不自写 if |
| ⑤ | `@ElementCollection(EAGER)` | 角色是"值集合"非独立实体，比 `@ManyToMany` 轻；EAGER 因为认证路径无事务，LAZY 集合会炸（§7 约束 3） |
| ⑥ | `@Enumerated(STRING)` | ORDINAL 时在枚举中间插值 → 全库角色错位 |

角色升级路径：未来角色需动态管理（增删、权限点挂角色）时升级 `@ManyToMany` + Role 实体，Repository 层以上代码不动。

---

## 2. Repository

```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);   // 种子幂等判断用
}
```

- `findByUsername` 是派生查询：Spring Data 解析方法名生成 JPQL；方法名拼错字段在**启动时**报错（fail-fast）；
- 返回 `Optional`：显式表达"可能不存在"，判断义务交调用方（§3 正好利用）。

---

## 3. 用户加载：DatabaseUserDetailsService

```java
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(u -> LoginUser.of(
                        String.valueOf(u.getId()),   // userId = 主键字符串化，进 JWT 的 sub
                        u.getUsername(),
                        u.getPassword(),             // BCrypt 摘要，交给 Provider.matches
                        u.isEnabled(),
                        u.getRoles()))               // Set<AppRole> → 带 ROLE_ 前缀的 authorities
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }
}
```

- 登录链路：AuthenticationManager → DaoAuthenticationProvider → **本类** → PG（完整链路见 [spring-security.md §5](spring-security.md#5-登录认证路径)）；
- `UsernameNotFoundException` 会被 Provider 隐藏成 `BadCredentialsException`（防用户枚举）——**不**为它定义专门错误码；
- `enabled=false` → `DisabledException` → 401 `USER_DISABLED`；
- **刷新路径同样经过这里**：refresh 按 `sub`(userId) 重查最新角色/禁用状态——"改角色最迟下次刷新生效"的落点。

---

## 4. 种子数据：DataInitializer

```java
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin", "ROLE_ADMIN");
        seed("user",  "ROLE_USER");
    }

    private void seed(String username, String role) {
        if (userRepository.existsByUsername(username)) return;      // 幂等：重启不重复插
        userRepository.save(new UserEntity(username,
                passwordEncoder.encode("123456"),                   // 明文只在内存，落库即摘要
                Set.of(AppRole.valueOf(role))));
    }
}
```

演示账号：`admin/123456`（ROLE_ADMIN）、`user/123456`（ROLE_USER）。

用 `CommandLineRunner` 而非 `data.sql`：密码需经 `passwordEncoder.encode`（SQL 里只能写死 hash）；且 `data.sql` 与 `ddl-auto` 执行顺序随 Boot 版本有差异（2.5+ 需 `defer-datasource-initialization=true`）——代码式种子更稳。

---

## 5. 配置详解

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${PG_HOST:localhost}:5432/${PG_DB:springboot_in_action}
    username: ${PG_USER:postgres}
    password: ${PG_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: update          # 教学演示；生产 Flyway（§5.1）
    open-in-view: false         # §5.2
```

### 5.1 `ddl-auto` 五个值

| 值 | 行为 | 适用 |
|---|---|---|
| `update` | 缺表建表、缺列加列；**永不删列/改列类型** | 本模块演示 |
| `create` | 每次启动 drop 再建 | 一次性实验 |
| `create-drop` | 启动建、关闭删 | **测试**（配合 H2） |
| `validate` | 只校验不修改 | 生产（配 Flyway） |
| `none` | 完全不管 | 生产（配 Flyway） |

`update` 是"只进不退"的，表结构会漂移；生产必须 Flyway（SQL 版本化、可评审、可回滚）+ `validate`。

### 5.2 `open-in-view: false`（OSIV）

OSIV 默认开启时 Session 从请求进来到视图渲染完才关，数据库连接被占满整个请求周期，高并发下连接池耗尽。显式关闭是生产共识；关闭后事务外访问懒属性抛 `LazyInitializationException`——本模型 roles 已 EAGER（§1.2 ⑤），恰好免疫。

### 5.3 本地 PostgreSQL

```bash
# docker 一行起库（与本配置默认值对齐）
docker run -d --name pg -p 5432:5432 \
  -e POSTGRES_DB=springboot_in_action -e POSTGRES_PASSWORD=postgres postgres:16
```

连接信息全部支持环境变量覆盖；真实密码不进 Git。连接池为 Boot 默认 HikariCP。

---

## 6. 测试：H2 替身

`src/test/resources/application.yml`（测试类路径覆盖主配置）：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop     # 每个测试上下文全新建表
```

- `MODE=PostgreSQL` 开启 H2 的 PG 兼容语法；
- 选型：H2 内存库（快、零依赖、CI 友好）。本模块测试重点是**安全链路**而非 SQL 方言，H2 足够；未来出现 PG 专有 SQL（JSONB、窗口函数）再切 Testcontainers；
- 依赖：`com.h2database:h2`（test scope，版本随 Boot BOM）。

---

## 7. 实现约束（MUST）

1. 表名禁用裸 `user`（PG 保留字，裸用报语法错）——实体显式 `@Table(name="users")`；标识符统一小写（PG 不加引号的标识符一律转小写）；
2. 枚举一律 `@Enumerated(STRING)`，ORDINAL 是定时炸弹；
3. 会被认证路径读到的关联必须 EAGER（认证路径无事务，LAZY 抛 `LazyInitializationException`）；
4. Entity 只存 BCrypt 摘要，明文密码只存在于请求进来的那一刻；
5. 禁用判定交给框架（`enabled` 布尔 → `DisabledException`），不自写 if；
6. `ddl-auto: update` 不是迁移工具——生产 Flyway + `validate`；
7. `open-in-view: false`；
8. 种子数据必须幂等（`existsByUsername`），否则重启撞 unique 约束启动失败；
9. 列表页批量查用户时注意 roles 集合 N+1（`@ElementCollection` 默认第二条 SELECT）——需 `join fetch` 或 `@BatchSize`。

---

## 8. 改动同步提醒

改了本文覆盖的内容，按 [README 路由表](README.md#3-同步路由表改了什么--必须同步哪些文档) 同步：

- 角色模型/取值变化 → **jwt.md**（claims 的 roles 定义）、**spring-security.md**（hasRole 对应关系）
- `DatabaseUserDetailsService` 映射变化 → **spring-security.md** §5（登录认证路径）
- 换真实迁移工具（Flyway）→ **PRD** 决策记录补记
