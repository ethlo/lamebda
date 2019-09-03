package com.ethlo.lamebda.io;

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

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

public enum ChangeType
{
    CREATED, MODIFIED, DELETED;

    public static ChangeType from(Kind<?> k)
    {
        if (k == StandardWatchEventKinds.ENTRY_CREATE)
        {
            return ChangeType.CREATED;
        }
        else if (k == StandardWatchEventKinds.ENTRY_MODIFY)
        {
            return ChangeType.MODIFIED;
        }
        else if (k == StandardWatchEventKinds.ENTRY_DELETE)
        {
            return ChangeType.DELETED;
        }

        throw new IllegalArgumentException("Unknown kind " + k);
    }
}
