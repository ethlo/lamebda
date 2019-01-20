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

public class DelegatingFunctionManager implements FunctionManager
{
    private final FunctionManagerDirector functionManagerDirector;

    public DelegatingFunctionManager(final FunctionManagerDirector functionManagerDirector)
    {
        this.functionManagerDirector = functionManagerDirector;
    }

    @Override
    public boolean handle(final HttpRequest request, final HttpResponse response) throws Exception
    {
        for (FunctionManager functionManager : functionManagerDirector.getFunctionManagers().values())
        {
            if (functionManager.handle(request, response))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close()
    {
        functionManagerDirector.getFunctionManagers().values()
                .forEach(fm -> close());
    }
}
