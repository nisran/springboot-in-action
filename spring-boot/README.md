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
