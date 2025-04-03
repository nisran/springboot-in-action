Spring Webflux

# Why

Tomcat based server

- limit of concurrent user：Tomcat will create a thread for each request and walk the entire flow（7 steps）until respond complete。
- Thread pool size determines that how many request it can handle at a time. you have to extend hardware if you want upgrade concurrent functionality.
- Low latency: return the response until all requests complete.
- How to handle synchronizes and blocking
  - Complex 
  - Future

# Base knowledge
- Lambda
- Functional Interface
- Stream API
- Reactive Stream

## Reactive Stream
- publisher
- Subscriber
  - Consumer Interface
  - BaseSubscriber
  - cancel : Disposal Interface
  - Backpressure
    - Buffer
- Subscription
- Processor

Practice

- Flux: 0|1 data flow
- Mono: 0-N data flow

# Spring WebFlux

## Reactive Core

Reactor Netty: HttpHandler and HttpServer

``` java
HttpHandler handler = ...
ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
HttpServer.create().host(host).port(port).handle(adapter).bindNow();
```

Dispatcher Handler

- VS Dispatcher Handler

SSE

- WebFilter
- 

# R2DBC

- R2DBC native api
- Spring-boot-starter-data-r2dbc
- basic crud
- 





# Reference

- Project Reactor: https://projectreactor.io/
- Reactive Streams: https://www.reactive-streams.org/
- The Reactive Manifesto: https://www.reactivemanifesto.org/
- Spring WebFlux: https://docs.spring.io/spring-framework/reference/web/webflux.html
- R2DBC：https://r2dbc.io/
- Learning Notes: https://www.yuque.com/leifengyang/springboot3/rg4wqgdbg8mfv04q

