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

import com.ethlo.lamebda.*
import mypackage.*
import spec.Pet
import other.Other

@PropertyFile("config.properties")
class Correct extends SimpleServerFunction {

    Correct() {
        new MyLib().helloWorld("John Smith")
        def pet = new Pet()
    }

    @Override
    void get(HttpRequest request, HttpResponse response) {
        Other.Nested p = null
        response.json(HttpStatus.OK, [method: request.method, message: 'Hello world'])
    }
}