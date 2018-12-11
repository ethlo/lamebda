package com.ethlo.lamebda;

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
@FunctionalInterface
public interface ServerFunction
{
    /**
     * Handle the request and write the contents to the response
     * @param request The incoming HTTP request
     * @param response The outgoing response
     * @return The handling result, {@link FunctionResult#PROCESSED} if this method handled the request, otherwise {@link FunctionResult#SKIPPED}
     */
    FunctionResult handle(HttpRequest request, HttpResponse response) throws Exception;
}