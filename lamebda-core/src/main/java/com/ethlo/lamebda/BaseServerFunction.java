package com.ethlo.lamebda;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;

import com.ethlo.lamebda.context.FunctionConfiguration;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.util.Assert;

public abstract class BaseServerFunction implements ServerFunction, FunctionContextAware
{
    protected FunctionContext context;

    @Override
    public final void setContext(final FunctionContext context)
    {
        if (this.context == null)
        {
            Assert.notNull(context, "context cannot be null");
            this.context = context;
            this.init(context);
            this.init(context.getProjectConfiguration());
            this.init(context.getConfiguration());
            initInternal(context);
        }
    }

    @Override
    public void init(final FunctionConfiguration functionConfiguration)
    {
        // Empty implementation
    }

    @Override
    public void init(ProjectConfiguration projectConfiguration)
    {
        // Empty implementation
    }

    @Override
    public void init(FunctionContext functionContext)
    {
        // Empty implementation
    }

    protected abstract void initInternal(FunctionContext functionContext);

    protected void handlePostConstructMethods()
    {
        for (Method method : getClass().getDeclaredMethods())
        {
            if (method.getAnnotation(PostConstruct.class) != null && method.getParameterCount() == 0)
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(this);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    throw new IllegalArgumentException("Cannot call @PostConstruct annotated method " + method, e);
                }
            }
        }
    }
}
