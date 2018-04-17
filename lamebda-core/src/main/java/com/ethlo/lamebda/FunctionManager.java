package com.ethlo.lamebda;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import com.ethlo.lamebda.function.ServerFunction;

public class FunctionManager<I,O, T extends ServerFunction<I, O>> implements Iterable<T>
{
    private static final Logger logger = LoggerFactory.getLogger(FunctionManager.class);
    
    private Map<String, T> functions = new ConcurrentHashMap<>();
    private ClassResourceLoader<I,O, T> loader;
    
    public FunctionManager(ClassResourceLoader<I,O, T> loader)
    {
        this.loader = loader;
        loader.setChangeListener(n->addFunction(loader.loadClass(n.getName())));
    }
    
    private void addFunction(T func)
    {
        final boolean exists = functions.put(func.getClass().getName(), func) != null;
        logger.info(exists ? "{} was modified" : "{} was loaded", func.getClass().getSimpleName());
    }
    
    @PostConstruct
    protected void loadAll()
    {
        for (HandlerFunctionInfo f : loader.findAll(PageRequest.of(0, Integer.MAX_VALUE)))
        {
            addFunction(loader.loadClass(f.getName()));
        }
    }

    @Override
    public Iterator<T> iterator()
    {
        return functions.values().iterator();
    }
}
