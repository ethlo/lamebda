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

public abstract class BaseServerFunction implements ServerFunction, FunctionContextAware
{
    @Override
    public void init(final ProjectConfiguration projectConfiguration)
    {
        // Empty implementation
    }

    protected abstract void initInternal(ProjectConfiguration cfg);

    public void handlePostConstructMethods()
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
