/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;

/**
 * Default implementation of {@link HttpHeaders}.
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
   *
   * @see #addAll(Map)
   */
  public DefaultHttpHeaders(Map<String, List<String>> headers) {
    this.headers = MultiValueMap.forSmartListAdaption(new LinkedCaseInsensitiveMap<>(headers.size(), Locale.ROOT));
    addAll(headers);
  }

  /**
   * Construct a new {@code HttpHeaders} instance backed by the supplied map.
   * <p>This constructor is available as an optimization for adapting to existing
   * headers map structures, primarily for internal use within the framework.
   *
   * @param headers the headers map (expected to operate with case-insensitive keys)
   * @since 5.0
   */
  public DefaultHttpHeaders(MultiValueMap<String, String> headers) {
    Assert.notNull(headers, "MultiValueMap is required");
    this.headers = headers;
  }

  /**
   * Construct a new {@code HttpHeaders} instance backed by an existing map.
   * <p>This constructor is available as an optimization for adapting to existing
   * headers map structures, primarily for internal use within the framework.
   *
   * @param headers the headers map (expected to operate with case-insensitive keys)
   */
  public DefaultHttpHeaders(HttpHeaders headers) {
    Assert.notNull(headers, "MultiValueMap is required");
    if (headers instanceof DefaultHttpHeaders httpHeaders) {
      this.headers = DefaultHttpHeaders.unwrap(httpHeaders);
    }
    else {
      this.headers = MultiValueMap.forSmartListAdaption(new LinkedCaseInsensitiveMap<>(8, Locale.ROOT));
      setAll(headers);
    }
  }

  @Override
  public @Nullable String getFirst(String name) {
    return headers.getFirst(name);
  }

  @Override
  public void add(String name, @Nullable String value) {
    if (value != null) {
      headers.add(name, value);
    }
  }

  @Override
  public @Nullable List<String> setHeader(String name, List<String> values) {
    return headers.put(name, values);
  }

  @Override
  protected void setHeader(String name, String value) {
    headers.setOrRemove(name, value);
  }

  @Override
  public @Nullable List<String> setOrRemove(String name, String @Nullable [] value) {
    return headers.setOrRemove(name, ObjectUtils.isEmpty(value) ? null : value);
  }

  @Override
  public @Nullable List<String> setOrRemove(String name, @Nullable List<String> value) {
    return headers.setOrRemove(name, CollectionUtils.isEmpty(value) ? null : value);
  }

  @Override
  public @Nullable List<String> remove(String name) {
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
  public boolean containsHeader(String name) {
    return headers.containsKey(name);
  }

  @Override
  public @Nullable List<String> get(String name) {
    return headers.get(name);
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
   * Return a {@link Set} views of header entries, reconstructed from
   * iterating over the {@link #keySet()}. This can include duplicate entries
   * if multiple casing variants of a given header name are tracked, see
   * {@link HttpHeaders class level javadoc}.
   */
  @Override
  public Set<Map.Entry<String, List<String>>> entrySet() {
    return headers.entrySet();
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    return headers.toSingleValueMap();
  }

  @Override
  public MultiValueMap<String, String> asMultiValueMap() {
    return headers;
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

  @Override
  public @Nullable List<String> setIfAbsent(String name, List<String> values) {
    return this.headers.putIfAbsent(name, values);
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
