/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.support;

import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.netty.handler.codec.http.HttpHeaders;

/**
 * {@code HttpHeaders} implementation for wrapping Netty 4 HTTP headers.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class Netty4HttpHeaders extends infra.http.HttpHeaders {

  private final HttpHeaders headers;

  public Netty4HttpHeaders(HttpHeaders headers) {
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
  public @Nullable String getFirst(String name) {
    return headers.get(name);
  }

  @Override
  public @Nullable List<String> get(String name) {
    List<String> list = headers.getAll(name);
    return list.isEmpty() ? null : list;
  }

  @Override
  public void add(String name, @Nullable String value) {
    if (value != null) {
      headers.add(name, value);
    }
  }

  @Override
  public @Nullable List<String> setHeader(String name, String value) {
    List<String> previous = headers.getAll(name);
    headers.set(name, value);
    return previous;
  }

  @Override
  protected @Nullable List<String> setHeader(String name, @Nullable Collection<String> value) {
    List<String> previous = headers.getAll(name);
    headers.set(name, value);
    return previous;
  }

  @Override
  public @Nullable List<String> remove(String name) {
    List<String> previousValues = headers.getAll(name);
    headers.remove(name);
    return previousValues;
  }

  @Override
  public void clear() {
    headers.clear();
  }

  @Override
  public Set<String> names() {
    return headers.names();
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

  private final class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

    private final Iterator<String> names = headers.names().iterator();

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
      return headers.getAll(this.key);
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previousValues = headers.getAll(this.key);
      headers.set(this.key, value);
      return previousValues;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(Object o) {
      if (o == this)
        return true;

      return o instanceof Map.Entry<?, ?> e
              && Objects.equals(key, e.getKey())
              && Objects.equals(getValue(), e.getValue());
    }

  }

}
