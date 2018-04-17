package com.ethlo.lamebda;

import java.io.IOException;
import java.util.function.Consumer;

import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.ethlo.lamebda.function.ServerFunction;

import groovy.lang.GroovyClassLoader;

public abstract class ClassResourceLoader<I,O,T extends ServerFunction<I, O>>
{
    private final ApplicationContext applicationContext;
    private Consumer<FunctionModificationNotice> changeListener;
    private Class<T> type;
    
    public ClassResourceLoader(Class<T> type, ApplicationContext applicationContext)
    {
        Assert.notNull(type, "type cannot be null");
        this.type = type;
        
        Assert.notNull(applicationContext, "applicationContext cannot be null");
        this.applicationContext = applicationContext;
    }
    
    /**
     * Load the contents of the named class
     * @param name The class name
     * @return The contents of the specified name
     * @throws IOException If the class could not be found or loaded
     */
    public abstract String load(String name) throws IOException;
    
    /**
     * Return a list of all known functions
     * @param pageable
     * @return
     */
    public abstract Page<HandlerFunctionInfo> findAll(Pageable pageable);
    
    /**
     * Set a listener that gets notified whenever the function's source changes
     * @param l The listener
     */
    public void setChangeListener(Consumer<FunctionModificationNotice> l)
    {
        this.changeListener = l;
    }
    
    public final T loadClass(String name)
    {
        try (final GroovyClassLoader classLoader = new GroovyClassLoader())
        {
            final Class<?> clazz = classLoader.parseClass(load(name + ".groovy"));
            Assert.isTrue(name.equals(clazz.getName()), "Incorrect class name " + clazz.getName() + " in " + name);
            final T f = type.cast(clazz.newInstance());
            applicationContext.getAutowireCapableBeanFactory().autowireBean(f);
            return f;
        }
        catch (InstantiationException | IllegalAccessException | IOException exc)
        {
            throw new IllegalStateException("Cannot load class " + name, exc);
        }
    }

    protected void functionChanged(String name, ChangeType changeType)
    {
        changeListener.accept(new FunctionModificationNotice(name, changeType));
    }
}