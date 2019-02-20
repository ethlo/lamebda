package com.ethlo.lamebda.loaders;

import java.util.function.Consumer;

import com.ethlo.lamebda.ApiSpecificationModificationNotice;
import com.ethlo.lamebda.FunctionModificationNotice;
import com.ethlo.lamebda.io.FileSystemEvent;

public interface FileSystemNotificationAware
{
    void setProjectChangeListener(Consumer<FileSystemEvent> l);

    void setApiSpecificationChangeListener(Consumer<ApiSpecificationModificationNotice> apiSpecificationChangeListener);

    void setLibChangeListener(Consumer<FileSystemEvent> listener);

    void setFunctionChangeListener(Consumer<FunctionModificationNotice> l);
}