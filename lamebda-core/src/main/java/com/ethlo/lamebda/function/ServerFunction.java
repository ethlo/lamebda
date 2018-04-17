package com.ethlo.lamebda.function;

public interface ServerFunction<I,O>
{
    void handle(I request, O response);

    boolean match(I request);
}
