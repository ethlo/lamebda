package com.ethlo.lamebda.loaders;

import com.ethlo.lamebda.ServerFunction;

@FunctionalInterface
public interface FunctionPostProcesor
{
    public ServerFunction process(ServerFunction function);
}
