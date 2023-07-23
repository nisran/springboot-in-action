# Spring Security
spring security 的核心就是将一堆filter加到servlet前面，进行认证和鉴权。

## Spring Security Internal Flow

- User try to access a secure API in the first time.
- The request come to `AuthenticationFilter` and redirect to login page via `DefaultLoginPageGeneratingFilter`.
- User enter username and password in the login page and rey to login.
- `UserPasswordAuthenticationFilter`extract username and password to create an object `UsernamePasswordAuthenticationToken` (implemention of `Authentication` interface)
    - then invoke `authenticate()` method of `ProviderManager`.
- `ProviderManager` is an implementation of `AuthenticationManager`, who is a manager class of multiple Authentication manager. It will invoke multiple AuthProviders one by one, authentication success when at last one authProvider successfully authenticate and return Authentication object.
- In current scenario `DaoAuthenticationProvider` will be invoked, it will invoke method `loadUserByUsername()` from `UserDetailsManager` to get password/authorities from DB/Inmemory. then compare user credential from UI and DB to validate if user is authentic or not after `PasswordEncoder`.
- The authentication object will be store in the `SecurityContext` object.



## Defining & Managing User

Authentication VS UserDetails

- `Principal(Interface) -> Authentication(Interface) -> UsernamePasswordAuthenticationToken(class)`
- `UserDetails(Interface) -> User(Class)`
- 前者是框架生成的，一开始来自客户端，认证后在补充完善保存在securityContext共系统使用；后者一般来自数据库等用于认证鉴权。

Security Configuration
- `SecurityFilterChain`: 默认创建于`SpringBootWebSecurityConfiguration`配置类 `defaultSecurityConfiguration`方法, 默认所有的方法都是需要认证的。
- 自定义`SecurityFilterChain`，设置需要/不需要认证的API; formLogin相关的配置等等
- 可配置`InMemoryUserDetailsManager`，在内存中设置多个用户
- 配置`PasswordEncoder`

`UserDetailsManager`

- InMemoryUserDetailsManager
- `JdbcUserDetailsManager`: 如果想用这个类，那么你要间的数据库表结构要跟给定的一摸一样，无法满足自定义需求

## Password Management
Encoding VS Encryption Vs Hashing：

- Encoding：编码；将数据从一种格式转换到另一种格式，毫无加密操作；如：ASCII，Base64，Unicode
- Encryption：加密；数据加密可以通过特定的密钥和算法
- Hashing：哈希；将数据通过一定的算法转换成特定长度的数字，是单向操作，一般用于认证，添加索引快读匹配等

PasswordEncoder

- brute force attack: 蛮力破解：通过哈希表或者强大的算例尝试破解
- BCryptPasswordEncoder：破解需要：算力；[工具网站](https://bcrypt-generator.com/)
- SCryptPasswordEncoder：破解需要：算力，内存；
- Argon2PasswordEncoder：破解需要：算力，内存，多核；

总的来说，按当前计算机算力第一个已经很安全了，虽然第二第三个更安全，但是登录/注册的时候会很消耗性能，所以不推荐后两个。

出了上面之外，spring security提供了众多工具类，不需要再引进第三方包。可参考：`org.springframework.security.crypto.keygen` 和`org.springframework.security.crypto.encrypt`。

## Understanding Authentication Provider and implement it 

鉴权：用户名/密码；面部/指纹；验证码等 

所以需要各种`AuthenticationProvider`, 去使用不同场景下的权限认证，如:

```java
public interface AuthenticationProvider {
	Authentication authenticate(Authentication authentication) throws AuthenticationException;

	boolean supports(Class<?> authentication);
}
```



自定义AuthenticationProvider

## CORS & CSRF

CORS：Origin Resourse Sharing.

- Cors 不是安全问题，而是浏览器约定的不同源值之间通信的保护机制

- 没有设置header 直接访问后端会出现Cors issue。
    - different scheme：scheme（Http or Https）+domain+port
- Solution：
    - 加注解：`@CrossOrigin`：但是加每一个Contoller类或者方法很麻烦，所以我们统一配置
    - 全局配置

CSRF: attack： Cross Site Request Forgery，跨站点请求伪造。 

- 别的网站的脚本会利用cookies伪造用户（通过脚本），去调用原网站的信息做一些操作

- 一般是post，get api不需要担心
- solution:
    - 打开CSRF
    - 配置spring security生成一个随机的CSRF token，然后发给UI，UI再每次请求中将此token在header中带获取
    - 对与特定的路劲我们可以关掉CSRF校验，比如一些public API

## Authentication & Authorization

- AuthN 一般在 AuthZ前面
- HTTP Code：401 & 403

Authority & Role的区别：前者是具体的权限，后者是一组权限和action的集合

可以在`requestMatchers` 后面跟permeitAll 或者 authenticate一样，配置hasauthority，hasRole等方法来配置具体的路径上面。

hasrole 用法，springsecurity6 和小于6的区别：前者会在参数上添加`ROLE_`

## Filter

 定制化需求：校验，Log（IP等），对头部的修改

为了查看spring security中的filter，先进入debug模式

- `@EnableWebSecurity(debug=trur)` 
- logging.level.org.springframework.security.web.FilterChainProxy: debug

然后可以看到日志中的filters：

``` java
DisableEncodeUrlFilter disableEncodeUrlFilter;
WebAsyncManagerIntegrationFilter webAsyncManagerIntegrationFilter;
SecurityContextHolderFilter securityContextHolderFilter;
HeaderWriterFilter headerWriterFilter;
CorsFilter corsFilter;
CsrfFilter csrfFilter;
LogoutFilter logoutFilter;
UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;
DefaultLoginPageGeneratingFilter defaultLoginPageGeneratingFilter;
DefaultLogoutPageGeneratingFilter defaultLogoutPageGeneratingFilter;
BasicAuthenticationFilter basicAuthenticationFilter;
RequestCacheAwareFilter requestCacheAwareFilter;
SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter;
AnonymousAuthenticationFilter anonymousAuthenticationFilter;
SessionManagementFilter sessionManagementFilter;
ExceptionTranslationFilter exceptionTranslationFilter;
FilterSecurityInterceptor filterSecurityInterceptor;
```



### 创建自己的Filter 并注入进去

我们可以实现`Jakarta.Serclet`接口，实现自己FIlter并通过`addFilterBefore`, `addFilterAfter` 将自己的filter注入在特定的位置。

当然还有一些抽象filter已经帮我做了部分配置，如： `GenericfilterBean`, `OncePerRequest`等。

## JWT

缺点：

- JsessionID 没有任何信息，无法进行扩展

- 保存在coocies：一直存在browser，可能被利用。

JWT：Json Web Tocken

Advantages of tocken:

JWT advantage

- 保存user detail，特别是微服务

结构

- header：metadata
- payload：
- signarure optional:
- 不会将用户信息存在服务器，都在jwt

## Method Layer security

- open by `@EnableMethodSecurity`
- `@PreAuthorize` and `@PostAuthorize`
