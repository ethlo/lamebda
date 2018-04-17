package com.ethlo.lamebda;

import org.springframework.web.reactive.function.server.ServerRequest;

import com.ethlo.lamebda.function.ServerFunction;

public interface FluxServerFunction extends ServerFunction<ServerRequest, ServerResponseSink>
{

}
