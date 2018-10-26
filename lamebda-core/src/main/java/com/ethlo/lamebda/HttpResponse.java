package com.ethlo.lamebda;

import com.ethlo.lamebda.error.ErrorResponse;

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

public interface HttpResponse
{
    void setStatus(int status);

    void setContentType(String string);

    void setCharacterEncoding(String name);
    
    void write(String body);
    
    void write(byte[] body);

    void error(ErrorResponse error);

    void error(int status);

    void error(int status, String message);

    void json(int status, Object body);

    Object raw();
}
