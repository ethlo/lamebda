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
import mypackage.*
import other.Other

class Correct {

    Correct() {

        // From /shared
        new MyLib().helloWorld("John Smith")

        // Nested class in /shared
        Other.Nested p = new Other.Nested()

        // From generated model
        //def pet = new Pet()
    }

    def get() {
        return [method: "GET", message: 'Hello world']
    }
}