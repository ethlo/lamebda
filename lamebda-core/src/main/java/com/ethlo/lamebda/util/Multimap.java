package com.ethlo.lamebda.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class Multimap<K,V> extends LinkedHashMap<K,Set<V>>
{
    private static final long serialVersionUID = 6513494621809756841L;

    public void add(K key, V value)
    {
        this.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(value);
    }

    public void addAll(K key, Collection<V> items)
    {
        items.forEach(e->this.add(key, e));
    }
}
