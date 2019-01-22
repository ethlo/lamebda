package com.ethlo.lamebda.functions;

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

import java.io.IOException;
import java.net.URI;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.ProjectConfiguration;
import com.ethlo.lamebda.SimpleServerFunction;

public class RedirectFunction extends SimpleServerFunction implements BuiltInServerFunction
{
    private String targetUrlPath;

    public RedirectFunction(String urlPath, String targetUrlPath)
    {
        super(urlPath);
        this.targetUrlPath = targetUrlPath;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws IOException
    {
        response.setStatus(HttpStatus.MOVED_TEMPORARILY);
        response.addHeader("Location", targetUrlPath);
    }
}
