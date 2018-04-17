package com.ethlo.lamebda;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.ethlo.lamebda.loaders.FileSystemClassResourceLoader;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class Server
{
    public static void main(String[] args)
    {
        SpringApplication.run(Server.class, args);
    }
    
    @Bean
    public ClassResourceLoader<ServerRequest, ServerResponseSink, FluxServerFunction> classResourceLoader(ApplicationContext applicationContext, @Value("${lamebda.source.dir}") String basePath) throws IOException
    {
        return new FileSystemClassResourceLoader<>(FluxServerFunction.class, applicationContext, basePath);
    }
    
    @Bean
    public FunctionManager<ServerRequest, ServerResponseSink, FluxServerFunction> functionManager(ClassResourceLoader<ServerRequest, ServerResponseSink, FluxServerFunction> loader)
    {
        return new FunctionManager<>(loader);
    }
    
    @Bean 
    public RouterFunction<ServerResponse> delegate(FunctionManager<ServerRequest, ServerResponseSink, FluxServerFunction> functionManager)
    {
        final HandlerFunction<ServerResponse> handler = request->
        {
            for (FluxServerFunction f : functionManager)
            {
                if (f.match(request))
                {
                    final ServerResponseSink responseSink = new ServerResponseSink();
                    f.handle(request, responseSink);
                    return responseSink.get();
                }
            }
            throw new EmptyResultDataAccessException(1);
        };
        
        return request->Mono.just(handler);
    }
}