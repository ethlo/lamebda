package com.ethlo.lamebda;

import java.time.ZonedDateTime;

public class HandlerFunctionInfo
{
    private final String name;
    private ZonedDateTime lastModified;
    
    public HandlerFunctionInfo(String name)
    {
        this.name = name;
    }

    public ZonedDateTime getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(ZonedDateTime lastModified)
    {
        this.lastModified = lastModified;
    }

    public String getName()
    {
        return name;
    }
}
