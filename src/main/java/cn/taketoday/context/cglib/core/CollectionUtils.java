/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.context.cglib.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * @author Chris Nokleberg
 * @version $Id: CollectionUtils.java,v 1.7 2004/06/24 21:15:21 herbyderby Exp $
 * @author TODAY
 */
public abstract class CollectionUtils {

    public static Map<Object, List<Object>> bucket(Collection<?> c, Transformer t) {
        Map<Object, List<Object>> buckets = new HashMap<>();

        for (final Object value : c) {
            Object key = t.transform(value);
            List<Object> bucket = buckets.get(key);
            if (bucket == null) {
                buckets.put(key, bucket = new LinkedList<>());
            }
            bucket.add(value);
        }
        return buckets;
    }

    public static <T extends Object> void reverse(Map<T, T> source, Map<T, T> target) {

        for (Entry<T, T> entry : source.entrySet()) {
            target.put(entry.getValue(), entry.getKey());
        }
    }

    public static <T extends Object> Collection<T> filter(Collection<T> c, Predicate<T> p) {

        final Iterator<T> it = c.iterator();
        while (it.hasNext()) {
            if (!p.test(it.next())) {
                it.remove();
            }
        }
        return c;
    }

    public static List<Object> transform(final Collection<Object> c, final Transformer t) {
        final List<Object> result = new ArrayList<>(c.size());

        for (final Object obj : c) {
            result.add(t.transform(obj));
        }
        return result;
    }

    public static Map<Object, Integer> getIndexMap(List<Object> list) {
        final Map<Object, Integer> indexes = new HashMap<>();
        int index = 0;
        for (final Object obj : list) {
            indexes.put(obj, Integer.valueOf(index++));
        }
        return indexes;
    }
}
