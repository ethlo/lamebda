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
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ethlo.lamebda.util.IoUtil;

@RestController
@RequestMapping(value = "/specification")
public class DocumentationController
{
    private final ClassLoader classLoader;

    public DocumentationController(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @GetMapping(value = "/api/api.yaml", produces = "text/yaml")
    public ResponseEntity<String> getSpecFile() throws IOException
    {
        final ClassPathResource res = new ClassPathResource("/specification/oas.yaml", classLoader);
        if (res.exists())
        {
            return new ResponseEntity<>(IoUtil.toString(res.getInputStream(), StandardCharsets.UTF_8), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}