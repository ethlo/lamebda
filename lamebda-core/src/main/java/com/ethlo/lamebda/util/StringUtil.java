package com.ethlo.lamebda.util;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtil
{
    private static final int INDEX_NOT_FOUND = -1;

    private StringUtil()
    {
    }

    public static String hyphenToCamelCase(String name)
    {
        final String[] words = name.toLowerCase().split("-");
        final StringBuilder sb = new StringBuilder();
        for (String word : words)
        {
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1));
        }
        return sb.toString();
    }

    public static String[] tokenizeToStringArray(String str, String delimiters)
    {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens)
    {
        if (str == null)
        {
            return null;
        }
        final StringTokenizer st = new StringTokenizer(str, delimiters);
        final List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (trimTokens)
            {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0)
            {
                tokens.add(token);
            }
        }
        return toStringArray(tokens);
    }

    public static String[] toStringArray(Collection<String> collection)
    {
        if (collection == null)
        {
            return null;
        }
        return collection.toArray(new String[collection.size()]);
    }

    public static boolean hasText(String pattern)
    {
        return pattern != null && !"".equals(pattern.trim());
    }

    public static int countOccurrencesOf(String str, String sub)
    {
        if (str == null || sub == null || str.length() == 0 || sub.length() == 0)
        {
            return 0;
        }
        int count = 0;
        int pos = 0;
        int idx;
        while ((idx = str.indexOf(sub, pos)) != -1)
        {
            ++count;
            pos = idx + sub.length();
        }
        return count;
    }

    public static String camelCaseToHyphen(String s)
    {
        return s.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }

    public static String join(final Collection<?> collections, final String delim)
    {
        return collectionToDelimitedString(collections, delim, "", "");
    }

    public static String collectionToDelimitedString(Collection<?> coll, String delim, String prefix, String suffix)
    {
        if (coll == null || coll.isEmpty())
        {
            return "";
        }
        else
        {
            final StringBuilder sb = new StringBuilder();
            final Iterator it = coll.iterator();
            while (it.hasNext())
            {
                sb.append(prefix).append(it.next()).append(suffix);
                if (it.hasNext())
                {
                    sb.append(delim);
                }
            }
            return sb.toString();
        }
    }

    public static String strip(String str, final String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        str = stripStart(str, stripChars);
        return stripEnd(str, stripChars);
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static String stripStart(final String str, final String stripChars) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        if (stripChars == null) {
            while (start != strLen && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (start != strLen && stripChars.indexOf(str.charAt(start)) != INDEX_NOT_FOUND) {
                start++;
            }
        }
        return str.substring(start);
    }

    public static String stripEnd(final String str, final String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
                end--;
            }
        }
        return str.substring(0, end);
    }
}
