package com.ethlo.lamebda.error;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 Morten Haraldsen (ethlo)
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

public abstract class ClientException extends RuntimeException
{
    private static final long serialVersionUID = 8703127206192317947L;

    public ClientException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public ClientException(String msg)
    {
        super(msg);
    }
}
