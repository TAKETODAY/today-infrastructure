/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.SmartList;

/**
 * Default HttpHeaders
 *
 * @author TODAY 2020-01-30 18:31
 * @since 3.0
 */
public class DefaultHttpHeaders extends HttpHeaders {
  @Serial
  private static final long serialVersionUID = 1L;

  final MultiValueMap<String, String> headers;

  public static final Function<String, List<String>> defaultHeaderMapping = k -> new SmartList<>();

  /**
   * Construct a case-insensitive header map
   */
  public DefaultHttpHeaders() {
    this.headers = MultiValueMap.from(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH), defaultHeaderMapping);
  }

  /**
   * Construct with a user input header map
   */
  public DefaultHttpHeaders(Map<String, List<String>> headers) {
    this.headers = MultiValueMap.from(headers, defaultHeaderMapping);
  }

  /**
   * Construct a new {@code HttpHeaders} instance backed by an existing map.
   * <p>This constructor is available as an optimization for adapting to existing
   * headers map structures, primarily for internal use within the framework.
   *
   * @param headers the headers map (expected to operate with case-insensitive keys)
   */
  public DefaultHttpHeaders(MultiValueMap<String, String> headers) {
    Assert.notNull(headers, "MultiValueMap must not be null");
    this.headers = headers;
  }

  @Override
  public String getFirst(String headerName) {
    return headers.getFirst(headerName);
  }

  @Override
  public void add(String headerName, String headerValue) {
    headers.add(headerName, headerValue);
  }

  @Override
  public void set(String headerName, String headerValue) {
    headers.set(headerName, headerValue);
  }

  @Override
  public List<String> remove(Object key) {
    return headers.remove(key);
  }

  @Override
  public int size() {
    return headers.size();
  }

  @Override
  public boolean isEmpty() {
    return headers.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return headers.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return headers.containsValue(value);
  }

  @Override
  public List<String> get(Object key) {
    return headers.get(key);
  }

  @Override
  public List<String> put(String key, List<String> value) {
    return headers.put(key, value);
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> m) {
    headers.putAll(m);
  }

  @Override
  public void clear() {
    headers.clear();
  }

  @Override
  public Set<String> keySet() {
    return headers.keySet();
  }

  @Override
  public Collection<List<String>> values() {
    return headers.values();
  }

  @Override
  public Set<Entry<String, List<String>>> entrySet() {
    return headers.entrySet();
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    return headers.toSingleValueMap();
  }

  @Override
  public Map<String, String[]> toArrayMap(IntFunction<String[]> mappingFunction) {
    return headers.toArrayMap(mappingFunction);
  }

  @Override
  public void copyToArrayMap(Map<String, String[]> newMap, IntFunction<String[]> function) {
    headers.copyToArrayMap(newMap, function);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DefaultHttpHeaders)) {
      return false;
    }
    return unwrap(this).equals(unwrap((DefaultHttpHeaders) other));
  }

  private static MultiValueMap<String, String> unwrap(DefaultHttpHeaders headers) {
    while (headers.headers instanceof DefaultHttpHeaders) {
      headers = (DefaultHttpHeaders) headers.headers;
    }
    return headers.headers;
  }

  @Override
  public int hashCode() {
    return this.headers.hashCode();
  }

}
