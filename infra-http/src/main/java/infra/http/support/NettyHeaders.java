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

package infra.http.support;

import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import io.netty.handler.codec.Headers;

/**
 * {@code HttpHeaders} implementation for wrapping Netty {@link Headers}.
 * <p>Unlike {@link Netty4HttpHeaders}, which wraps the Netty HTTP/1
 * {@code HttpHeaders} type, this adapter accepts any Netty {@code Headers}
 * implementation, including HTTP/2 native headers.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class NettyHeaders extends infra.http.HttpHeaders {

  private final Headers<CharSequence, @Nullable CharSequence, ?> headers;

  private final boolean lowercaseNames;

  public NettyHeaders(Headers<CharSequence, @Nullable CharSequence, ?> headers) {
    this(headers, false);
  }

  /**
   * Create an adapter for the given Netty headers.
   *
   * @param headers the Netty headers to wrap
   * @param lowercaseNames whether header names should be lower-cased before
   * accessing the underlying headers, as required by HTTP/2
   */
  public NettyHeaders(Headers<CharSequence, @Nullable CharSequence, ?> headers, boolean lowercaseNames) {
    this.headers = headers;
    this.lowercaseNames = lowercaseNames;
  }

  private CharSequence normalizeHeaderName(String name) {
    return lowercaseNames ? name.toLowerCase(Locale.ROOT) : name;
  }

  @Override
  public int size() {
    return headers.names().size();
  }

  @Override
  public boolean isEmpty() {
    return headers.isEmpty();
  }

  @Override
  public boolean contains(String name) {
    return headers.contains(normalizeHeaderName(name));
  }

  @Override
  public boolean contains(String name, String value) {
    return headers.contains(normalizeHeaderName(name), value);
  }

  @Override
  public @Nullable String getFirst(String name) {
    CharSequence value = headers.get(normalizeHeaderName(name));
    return value != null ? value.toString() : null;
  }

  @Override
  public @Nullable List<String> get(String name) {
    List<@Nullable CharSequence> values = headers.getAll(normalizeHeaderName(name));
    return values.isEmpty() ? null : toStringList(values);
  }

  @Override
  public void add(String name, @Nullable String value) {
    if (value != null) {
      headers.add(normalizeHeaderName(name), value);
    }
  }

  @Override
  protected @Nullable List<String> setHeader(String name, String value) {
    CharSequence headerName = normalizeHeaderName(name);
    List<String> previous = get(name);
    headers.set(headerName, value);
    return previous;
  }

  @Override
  protected @Nullable List<String> setHeader(String name, @Nullable Collection<String> value) {
    CharSequence headerName = normalizeHeaderName(name);
    List<String> previous = get(name);
    headers.set(headerName, value);
    return previous;
  }

  @Override
  public @Nullable List<String> remove(String name) {
    List<String> previous = get(name);
    headers.remove(normalizeHeaderName(name));
    return previous;
  }

  @Override
  public void clear() {
    headers.clear();
  }

  @Override
  public Set<String> names() {
    Set<CharSequence> names = headers.names();
    Set<String> result = new LinkedHashSet<>(names.size());
    for (CharSequence name : names) {
      result.add(name.toString());
    }
    return result;
  }

  @Override
  public Set<Map.Entry<String, List<String>>> entries() {
    return new AbstractSet<>() {

      @Override
      public Iterator<Map.Entry<String, List<String>>> iterator() {
        return new EntryIterator();
      }

      @Override
      public int size() {
        return headers.size();
      }
    };
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super List<String>> action) {
    var headers = this.headers;
    for (CharSequence name : headers.names()) {
      action.accept(name.toString(), toStringList(headers.getAll(name)));
    }
  }

  private static List<String> toStringList(List<? extends CharSequence> values) {
    List<String> result = new ArrayList<>(values.size());
    for (CharSequence value : values) {
      result.add(value.toString());
    }
    return result;
  }

  private final class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

    private final Iterator<String> names = names().iterator();

    @Override
    public boolean hasNext() {
      return this.names.hasNext();
    }

    @Override
    public Map.Entry<String, List<String>> next() {
      return new HeaderEntry(this.names.next());
    }
  }

  private final class HeaderEntry implements Map.Entry<String, List<String>> {

    private final String key;

    HeaderEntry(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return this.key;
    }

    @Override
    public List<String> getValue() {
      return toStringList(headers.getAll(this.key));
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previousValues = toStringList(headers.getAll(this.key));
      headers.set(this.key, value);
      return previousValues;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this)
        return true;

      return o instanceof Map.Entry<?, ?> e
              && Objects.equals(key, e.getKey())
              && Objects.equals(getValue(), e.getValue());
    }

  }

}
