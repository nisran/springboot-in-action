package com.esmile;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class FluxTest {

    @Test
    void fluxAndMonoTest(){
        Flux<String> stringFlux = Flux.just("Spring", "Spring Boot", "Spring Boot Flux")
//                .concatWith(Flux.error(() -> new IllegalArgumentException("Exception Occurred")))
                .log();

        stringFlux.subscribe(System.out::println, System.err::println, () -> System.out.println("Completed"));

        StepVerifier.create(stringFlux)
                .expectNext("Spring")
                .expectNext("Spring Boot")
                .expectNext("Method…")
//                .expectError(IllegalArgumentException.class)
                .expectComplete();
    }

    @Test
    void monoTest(){
        Mono<String> stringMono = Mono.just("Spring");

        stringMono.subscribe(System.out::println);

        StepVerifier.create(stringMono)
                .expectNext("Spring")
//                .expectError(IllegalArgumentException.class)
                .expectComplete();
    }

    @Test
    void customConsumer(){
        Flux.range(1, 10)
                .doOnRequest(r -> System.out.println("request of " + r))
                .subscribe(new BaseSubscriber<>() {

                    @Override
                    public void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    public void hookOnNext(Integer integer) {
                        System.out.println("Cancelling after having received " + integer);
                        if (integer == 5) {
                            cancel();
                        }
                        requestUnbounded();
                    }
                });

    }

    @Test
    void testBuffer(){
        Flux.range(1, 10).subscribe(System.out::println);

        Flux.range(1, 10)
                .limitRate(2)
                .log()
                .buffer(3)
                .log()
                .subscribe(System.out::println);
    }

    @Test
    void testThread() {
        // all in main thread
        Flux.range(1, 10)
                .map(e -> e + 2).subscribeOn(Schedulers.newParallel("TTTTT"))
                .log()
                .map(e -> e + 10).subscribeOn(Schedulers.newParallel("parallel"))
                .subscribe(e -> System.out.println(Thread.currentThread() + ": " + e));
    }

}
