package com.esmile.controller.user;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

class UserRepositoryTest {

    @Test
    void testR2dbc() {
        ConnectionFactory connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///testdb");

        Publisher<? extends Connection> connectionPublisher = connectionFactory.create();
        Mono<Connection> connectionMono = Mono.from(connectionFactory.create());

        connectionMono.subscribe( connection ->
            connection.createStatement("INSERT INTO person (id, first_name, last_name) VALUES ($1, $2, $3)")
                    .bind("$1", 1)
                    .bind("$2", "Walter")
                    .bind("$3", "White")
                    .execute());

        Mono.from(connectionFactory.create())
                .flatMapMany(connection -> connection
                        .createStatement("SELECT firstname FROM PERSON WHERE age > $1")
                        .bind("$1", 42)
                        .execute())
                .flatMap(result -> result
                        .map((row, metadata) -> row.get("firstname", String.class)))
                .doOnNext(System.out::println)
                .subscribe();

    }
}