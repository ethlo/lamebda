package com.ethlo.lamebda;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

public enum ChangeType
{
    CREATED, MODIFIED, DELETED;
    
    public static ChangeType from(Kind<?> k)
    {
        if (k == StandardWatchEventKinds.ENTRY_CREATE)
        {
            return ChangeType.CREATED;
        }
        else if (k == StandardWatchEventKinds.ENTRY_MODIFY)
        {
            return ChangeType.MODIFIED;
        }
        else if (k == StandardWatchEventKinds.ENTRY_DELETE)
        {
            return ChangeType.DELETED;
        }
        
        throw new IllegalArgumentException("Unknown kind " + k); 
    }
}
