package com.ethlo.lamebda;

import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public class ServerResponseSink
{
    private Mono<ServerResponse> response;

    public Mono<ServerResponse> get()
    {
        return response;
    }
    
    public void set(Mono<ServerResponse> response)
    {
        this.response = response;
    }
}
