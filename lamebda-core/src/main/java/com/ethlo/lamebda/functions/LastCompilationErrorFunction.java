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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.Janitor;
import org.codehaus.groovy.control.ProcessingUnit;
import org.codehaus.groovy.control.messages.Message;

import com.ethlo.lamebda.HttpRequest;
import com.ethlo.lamebda.HttpResponse;
import com.ethlo.lamebda.HttpStatus;
import com.ethlo.lamebda.SimpleServerFunction;
import com.ethlo.lamebda.error.ErrorResponse;
import com.ethlo.lamebda.util.StringUtil;

public class LastCompilationErrorFunction extends SimpleServerFunction
{
    private final CompilationFailedException exc;

    public LastCompilationErrorFunction(String name, CompilationFailedException exc)
    {
        super("/error/" + StringUtil.camelCaseToHyphen(name));
        this.exc = exc;
    }

    @Override
    protected void get(HttpRequest request, HttpResponse response)
    {
        if (exc.getNode() != null)
        {
            final int line = exc.getNode().getLineNumber();
            final int column = exc.getNode().getColumnNumber();
            response.error(new ErrorResponse(HttpStatus.OK, "Error at line " + line + ", column " + column + ": " + exc.getMessage()));
        }
        else
        {
            response.error(new ErrorResponse(HttpStatus.OK, exc.getMessage()));
        }
    }
}



