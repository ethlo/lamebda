package com.ethlo.lamebda.reporting;

/*-
 * #%L
 * lamebda-core
 * %%
 * Copyright (C) 2018 - 2019 Morten Haraldsen (ethlo)
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

import java.util.Objects;

public class HttpStatusWithReason
{
    private final int statusCode;
    private final String reasonPhrase;

    public HttpStatusWithReason(final int statusCode, final String reasonPhrase)
    {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getReasonPhrase()
    {
        return reasonPhrase;
    }

    @Override public String toString()
    {
        return statusCode + (reasonPhrase != null ? (" " + reasonPhrase) : "");
    }

    @Override public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final HttpStatusWithReason that = (HttpStatusWithReason) o;
        return statusCode == that.statusCode &&
                Objects.equals(reasonPhrase, that.reasonPhrase);
    }

    @Override public int hashCode()
    {

        return Objects.hash(statusCode, reasonPhrase);
    }
}
