# Spring & Spring Boot

结合简单代码了解spring核心功能

## Spring Event Publish & Listener
原理
- 调用`ApplicationEventPublisher` publish event, 将某个`ApplicationEvent`的实现类给发布出去
- `ApplicationEventMulticaster`将通过发布的event类型去寻找所有的listener,一次调用
- 如果`ApplicationEventMulticaster` 配置了线程池, 那么就会使用多线程调用listener

创建Listener
- 实现`ApplicationListener`接口
- 方法加上`@EventListener`
  - condition方法
- 手动add到`ApplicationEventMulticaster`

问题与思考
- spring event listener 在spring启动过程中用的比较多，可以查看`ApplicationEvent`实现类
- 内部实现还是比较简单没办法定制一下需求：
  - 比如：根据不同的event实现同步和异步而不是所有的都统一配置
  - 同一个event有些listener同步并包含事务, 有些日志等非核心Listener用异步等
  - 可以跟其他设计模式比较研究...

## Spring Boot Test

- 单元测试
- 继承测试

## Spring Boot Validator

常见的validator

- hibernate column validate

- spring notnull 注解：这应该是解释性注解，用于生成文档和idea

- 方法注解：springboot IO

- controller注解

    

引入：

```xml
```



## Spring Boot Actuator

document: [Production-ready Features (spring.io)](https://docs.spring.io/spring-boot/docs/3.1.1/reference/html/actuator.html)

springboot 提供了很多production-ready 功能，帮你实现auditing，health，matrics等监督功能。这些功能一般通过HTTP终端或者JMX来提供，前者用的较多。

可以通过引入`spring-boot-starter-actuator`，其提供了所有的功能，无需developer实现。

### Enableing Endpoint

首先访问`localhost:8080/actuator`可以看到actuator开发的接口，如：`health，beans，info，mapping `等等。

- 默认情况都是开启的，出了shutdown
- 也可以关掉默认的配置，按需打开

### Exposing Endpoint

虽然endpoint是打开的但是没有暴露出去，出了health接口，所有需要按需暴露。
