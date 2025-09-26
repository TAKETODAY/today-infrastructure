/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.http;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import infra.lang.Assert;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.MultiValueMap;

/**
 * Default HttpHeaders
 *
 * <p>Note that {@code HttpHeaders} instances created by the default constructor
 * treat header names in a case-insensitive manner. Instances created with the
 * {@link #DefaultHttpHeaders(MultiValueMap)} constructor like those instantiated
 * internally by the framework to adapt to existing HTTP headers data structures
 * do guarantee per-header get/set/add operations to be case-insensitive as
 * mandated by the HTTP specification. However, it is not necessarily the case
 * for operations that deal with the collection as a whole (like {@code size()},
 * {@code values()}, {@code keySet()} and {@code entrySet()}).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2020-01-30 18:31
 */
public class DefaultHttpHeaders extends HttpHeaders {

  @Serial
  private static final long serialVersionUID = 1L;

  final MultiValueMap<String, String> headers;

  /**
   * Construct a case-insensitive header map
   */
  public DefaultHttpHeaders() {
    this.headers = MultiValueMap.forSmartListAdaption(
            new LinkedCaseInsensitiveMap<>(8, Locale.ROOT));
  }

  /**
   * Construct with a user input header map
   */
  public DefaultHttpHeaders(Map<String, List<String>> headers) {
    this.headers = MultiValueMap.forSmartListAdaption(headers);
  }

  /**
   * Construct a new {@code HttpHeaders} instance backed by an existing map.
   * <p>This constructor is available as an optimization for adapting to existing
   * headers map structures, primarily for internal use within the framework.
   *
   * @param headers the headers map (expected to operate with case-insensitive keys)
   */
  public DefaultHttpHeaders(MultiValueMap<String, String> headers) {
    Assert.notNull(headers, "MultiValueMap is required");
    if (headers == EMPTY) {
      this.headers = MultiValueMap.forSmartListAdaption(new LinkedCaseInsensitiveMap<>(8, Locale.ROOT));
    }
    else if (headers instanceof DefaultHttpHeaders httpHeaders) {
      while (httpHeaders.headers instanceof DefaultHttpHeaders wrapped) {
        httpHeaders = wrapped;
      }
      this.headers = httpHeaders.headers;
    }
    else {
      this.headers = headers;
    }
  }

  @Nullable
  @Override
  public String getFirst(String name) {
    return headers.getFirst(name);
  }

  @Override
  public void add(String name, @Nullable String value) {
    headers.add(name, value);
  }

  @Override
  protected void setHeader(String name, String value) {
    headers.setOrRemove(name, value);
  }

  @Nullable
  @Override
  public List<String> setOrRemove(String name, @Nullable Collection<String> value) {
    return headers.setOrRemove(name, value);
  }

  @Nullable
  @Override
  public List<String> setOrRemove(String name, @Nullable String[] value) {
    return headers.setOrRemove(name, value);
  }

  @Override
  public List<String> remove(Object name) {
    return headers.remove(name);
  }

  /**
   * Return the number of headers in the collection. This can be inflated,
   * see {@link HttpHeaders class level javadoc}.
   */
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

  @Nullable
  @Override
  public List<String> get(Object name) {
    return headers.get(name);
  }

  @Nullable
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

  /**
   * Return a {@link Set} view of header names. This can include multiple
   * casing variants of a given header name, see
   * {@link HttpHeaders class level javadoc}.
   */
  @Override
  public Set<String> keySet() {
    return headers.keySet();
  }

  /**
   * Return a {@link Collection} view of all the header values, reconstructed
   * from iterating over the {@link #keySet()}. This can include duplicates if
   * multiple casing variants of a given header name are tracked, see
   * {@link HttpHeaders class level javadoc}.
   */
  @Override
  public Collection<List<String>> values() {
    return headers.values();
  }

  /**
   * Return a {@link Set} views of header entries, reconstructed from
   * iterating over the {@link #keySet()}. This can include duplicate entries
   * if multiple casing variants of a given header name are tracked, see
   * {@link HttpHeaders class level javadoc}.
   */
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

  /**
   * Perform an action over each header, as when iterated via
   * {@link #entrySet()}. This can include duplicate entries
   * if multiple casing variants of a given header name are tracked, see
   * {@link HttpHeaders class level javadoc}.
   *
   * @param action the action to be performed for each entry
   */
  @Override
  public void forEach(BiConsumer<? super String, ? super List<String>> action) {
    this.headers.forEach(action);
  }

  @Nullable
  @Override
  public List<String> putIfAbsent(String key, List<String> value) {
    return this.headers.putIfAbsent(key, value);
  }

  @Override
  public HttpHeaders asReadOnly() {
    return new ReadOnlyHttpHeaders(headers);
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
