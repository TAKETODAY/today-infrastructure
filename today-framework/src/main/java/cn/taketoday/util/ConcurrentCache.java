/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.util;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author TODAY <br>
 * 2019-02-23 15:48
 */
public final class ConcurrentCache<K, V> {

  private final int size;
  private final ConcurrentHashMap<K, V> eden;
  private final WeakHashMap<K, V> longterm;

  public ConcurrentCache(int size) {
    this.size = size;
    this.longterm = new WeakHashMap<>(size);
    this.eden = new ConcurrentHashMap<>(size);
  }

  public static <K, V> ConcurrentCache<K, V> of() {
    return fromSize(512);
  }

  public static <K, V> ConcurrentCache<K, V> fromSize(int size) {
    return new ConcurrentCache<>(size);
  }

  public V get(K k) {
    V v = this.eden.get(k);
    if (v == null) {
      synchronized(longterm) {
        v = this.longterm.get(k);
      }
      if (v != null) {
        this.eden.put(k, v);
      }
    }
    return v;
  }

  public V get(K k, Function<? super K, ? extends V> function) {
    V v;
    if ((v = get(k)) == null) {
      V newValue;
      if ((newValue = function.apply(k)) != null) {
        put(k, newValue);
        return newValue;
      }
    }
    return v;
  }

  public Object remove(K k) {
    this.eden.remove(k);
    synchronized(longterm) {
      return this.longterm.remove(k);
    }
  }

  public void clear() {
    this.eden.clear();
    synchronized(longterm) {
      this.longterm.clear();
    }
  }

  public void put(K k, V v) {
    final Map<K, V> eden = this.eden;
    if (eden.size() >= size) {
      synchronized(longterm) {
        this.longterm.putAll(eden);
      }
      eden.clear();
    }
    eden.put(k, v);
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    final Map<K, V> eden = this.eden;
    if (eden.size() >= size) {
      synchronized(longterm) {
        this.longterm.putAll(eden);
      }
      eden.clear();
    }
    eden.putAll(m);
  }

  public boolean isEmpty() {
    return eden.isEmpty() && longterm.isEmpty();
  }

}
