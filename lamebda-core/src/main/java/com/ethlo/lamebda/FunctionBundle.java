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

public class FunctionBundle
{
    private final AbstractServerFunctionInfo info;
    private final ServerFunction function;

    public FunctionBundle(final AbstractServerFunctionInfo info, final ServerFunction function)
    {
        this.info = info;
        this.function = function;
    }

    public AbstractServerFunctionInfo getInfo()
    {
        return info;
    }

    public ServerFunction getFunction()
    {
        return function;
    }
}
