package com.ethlo.lamebda;

public class FunctionModificationNotice
{
    private final String name;
    private final ChangeType changeType;
    
    public FunctionModificationNotice(String name, ChangeType changeType)
    {
        this.name = name;
        this.changeType = changeType;
    }
    
    public String getName()
    {
        return name;
    }

    public ChangeType getChangeType()
    {
        return changeType;
    }
}
