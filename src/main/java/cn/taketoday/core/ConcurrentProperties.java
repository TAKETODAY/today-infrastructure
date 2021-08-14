/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.core;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author TODAY <br>
 * 2019-03-15 23:24
 */
public class ConcurrentProperties
        extends Properties implements ConcurrentMap<Object, Object> {
  private static final long serialVersionUID = 1L;

  private final ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>();

  public ConcurrentProperties() {}

  /**
   * Creates an empty property list with the specified defaults.
   *
   * @param defaults
   *         the defaults.
   */
  public ConcurrentProperties(Properties defaults) {
    super(defaults);
  }

  @Override
  public String getProperty(String key) {
    final Object oval = map.get(key);
    String sval = (oval instanceof String) ? (String) oval : null;
    return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    final String val = getProperty(key);
    return (val == null) ? defaultValue : val;
  }

  @Override
  public Object setProperty(String key, String value) {
    return put(key, value);
  }

  @Override
  public Enumeration<Object> keys() {
    return Collections.enumeration(keySet());
  }

  @Override
  public Enumeration<Object> elements() {
    return Collections.enumeration(values());
  }

  @Override
  public boolean contains(Object value) {
    return containsValue(value);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return map.get(key);
  }

  @Override
  public Object put(Object key, Object value) {
    if (key == null || value == null) {
      return null;
    }
    return map.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends Object, ? extends Object> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<Object> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<Object> values() {
    return map.values();
  }

  @Override
  public Set<Entry<Object, Object>> entrySet() {
    return map.entrySet();
  }

  @Override
  public Object putIfAbsent(Object key, Object value) {
    return map.putIfAbsent(key, value);
  }

  @Override
  public Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
    return map.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public Object compute(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
    return map.compute(key, remappingFunction);
  }

  @Override
  public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
    return map.computeIfPresent(key, remappingFunction);
  }

  @Override
  protected void rehash() {
    //
  }

  @Override
  public Object getOrDefault(Object key, Object defaultValue) {
    return map.getOrDefault(key, defaultValue);
  }

  @Override
  public void forEach(BiConsumer<? super Object, ? super Object> action) {
    map.forEach(action);
  }

  @Override
  public void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
    map.replaceAll(function);
  }

  @Override
  public Object merge(Object key, Object value,
                      BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction)//
  {
    return map.merge(key, value, remappingFunction);
  }

  @Override
  public Enumeration<?> propertyNames() {
    return keys();
  }

  @Override
  public Set<String> stringPropertyNames() {
    final Set<String> keys = new HashSet<>();
    for (Entry<Object, Object> entry : entrySet()) {
      Object k = entry.getKey();
      if (k instanceof String && entry.getValue() instanceof String) {
        keys.add((String) k);
      }
    }
    return keys;
  }

  @Override
  public ConcurrentProperties clone() {
    final ConcurrentProperties ret = new ConcurrentProperties();
    ret.putAll(map);
    return ret;
  }

  @Override
  public boolean remove(Object key, Object value) {
    return map.remove(key, value);
  }

  @Override
  public Object replace(Object key, Object value) {
    return map.replace(key, value);
  }

  @Override
  public boolean replace(Object key, Object oldValue, Object newValue) {
    return map.replace(key, oldValue, newValue);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.toString();
  }

  @Override
  public boolean equals(Object o) {
    return map.equals(o);
  }

  @Override
  public void list(PrintStream out) {
    final PrintWriter printWriter = new PrintWriter(out);
    list(printWriter);
    printWriter.flush();
  }

  @Override
  public void list(PrintWriter out) {
    out.println("-- listing properties --");
    for (Entry<Object, Object> entry : map.entrySet()) {
      out.println(entry.getKey() + " = " + entry.getValue());
    }

  }
}
