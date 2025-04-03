package com.esmile;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.io.IOException;

public class NettyCore {

    public static void main(String[] args) throws IOException {
        HttpHandler handler = (request, response) -> {
            System.out.println(request.getURI());
            return response.writeWith(Mono.just(response.bufferFactory().wrap(request.getURI().toString().getBytes())));
        };

        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
        HttpServer.create().host("localhost").port(8020).handle(adapter).bindNow();
        System.in.read();
    }

}
