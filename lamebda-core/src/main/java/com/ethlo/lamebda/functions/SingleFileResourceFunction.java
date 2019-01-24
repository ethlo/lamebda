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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ethlo.lamebda.HttpMimeType;
import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.context.FunctionContext;
import com.ethlo.lamebda.util.Assert;
import com.ethlo.lamebda.util.FileNameUtil;

public class SingleFileResourceFunction extends FallbackSingleFileResourceFunction
{
    public SingleFileResourceFunction(String urlPath, Path filePath)
    {
        super(urlPath, filePath, null);
        Assert.notNull(filePath, "filePath cannot be null");
    }
}
