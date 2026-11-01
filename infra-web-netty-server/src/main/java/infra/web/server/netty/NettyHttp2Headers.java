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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import infra.http.HttpHeaders;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * {@link HttpHeaders} adapter for native Netty HTTP/2 headers.
 * <p>Pseudo headers are removed by {@link Http2HttpContext} once the request
 * metadata has been extracted, so this view only exposes regular headers.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class NettyHttp2Headers extends HttpHeaders {

  private final Http2Headers headers;

  NettyHttp2Headers(Http2Headers headers) {
    this.headers = headers;
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
    return headers.contains(name);
  }

  @Override
  public boolean contains(String name, String value) {
    return headers.contains(name, value, true);
  }

  @Override
  public @Nullable String getFirst(String name) {
    CharSequence value = headers.get(name);
    return value != null ? value.toString() : null;
  }

  @Override
  public @Nullable List<String> get(String name) {
    List<CharSequence> values = headers.getAll(name);
    if (values.isEmpty()) {
      return null;
    }
    List<String> result = new ArrayList<>(values.size());
    for (CharSequence value : values) {
      result.add(value.toString());
    }
    return result;
  }

  @Override
  public void add(String name, @Nullable String value) {
    if (value != null) {
      headers.add(name, value);
    }
  }

  @Override
  protected @Nullable List<String> setHeader(String name, String value) {
    List<String> previous = get(name);
    headers.set(name, value);
    return previous;
  }

  @Override
  protected @Nullable List<String> setHeader(String name, @Nullable Collection<String> value) {
    List<String> previous = get(name);
    headers.set(name, value);
    return previous;
  }

  @Override
  public @Nullable List<String> remove(String name) {
    List<String> previous = get(name);
    headers.remove(name);
    return previous;
  }

  @Override
  public void clear() {
    headers.clear();
  }

  @Override
  public Set<String> names() {
    Set<String> names = new LinkedHashSet<>();
    for (CharSequence name : headers.names()) {
      names.add(name.toString());
    }
    return names;
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
        return headers.names().size();
      }
    };
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super List<String>> action) {
    for (CharSequence name : headers.names()) {
      String headerName = name.toString();
      action.accept(headerName, get(headerName));
    }
  }

  private final class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

    private final Iterator<CharSequence> names = headers.names().iterator();

    @Override
    public boolean hasNext() {
      return names.hasNext();
    }

    @Override
    public Map.Entry<String, List<String>> next() {
      return new HeaderEntry(names.next().toString());
    }
  }

  private final class HeaderEntry implements Map.Entry<String, List<String>> {

    private final String key;

    HeaderEntry(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public List<String> getValue() {
      List<String> value = get(key);
      return value != null ? value : List.of();
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previous = getValue();
      headers.set(key, value);
      return previous;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      return o instanceof Map.Entry<?, ?> e
              && Objects.equals(key, e.getKey())
              && Objects.equals(getValue(), e.getValue());
    }

  }

}
