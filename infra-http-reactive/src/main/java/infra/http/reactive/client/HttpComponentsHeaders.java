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

package infra.http.reactive.client;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.util.CollectionUtils;

/**
 * {@code HttpHeaders} implementation for wrapping Apache HttpComponents
 * HttpClient headers.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HttpComponentsHeaders extends HttpHeaders {

  private final HttpMessage message;

  HttpComponentsHeaders(HttpMessage message) {
    this.message = message;
  }

  @Nullable
  @Override
  public String getFirst(String name) {
    Header header = this.message.getFirstHeader(name);
    return header != null ? header.getValue() : null;
  }

  @Override
  public void add(String name, @Nullable String value) {
    this.message.addHeader(name, value);
  }

  @Override
  protected void setHeader(String name, String value) {
    this.message.setHeader(name, value);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    Map<String, String> map = CollectionUtils.newLinkedHashMap(size());
    this.message.headerIterator().forEachRemaining(h -> map.putIfAbsent(h.getName(), h.getValue()));
    return map;
  }

  @Override
  public int size() {
    return this.message.getHeaders().length;
  }

  @Override
  public boolean isEmpty() {
    return this.message.getHeaders().length == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof String headerName && this.message.containsHeader(headerName);
  }

  @Override
  public boolean containsValue(Object value) {
    return value instanceof String
            && Arrays.stream(this.message.getHeaders()).anyMatch(h -> h.getValue().equals(value));
  }

  @Nullable
  @Override
  public List<String> get(Object name) {
    ArrayList<String> values = null;
    if (containsKey(name)) {
      Header[] headers = message.getHeaders((String) name);
      values = new ArrayList<>(headers.length);
      for (Header header : headers) {
        values.add(header.getValue());
      }
    }
    return values;
  }

  @Nullable
  @Override
  public List<String> put(String key, List<String> values) {
    List<String> oldValues = remove(key);
    for (String value : values) {
      add(key, value);
    }
    return oldValues;
  }

  @Nullable
  @Override
  public List<String> remove(Object name) {
    if (name instanceof String headerName) {
      List<String> oldValues = get(name);
      message.removeHeaders(headerName);
      return oldValues;
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> map) {
    for (var entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    this.message.setHeaders();
  }

  @Override
  public Set<String> keySet() {
    LinkedHashSet<String> keys = new LinkedHashSet<>(size());
    for (Header header : message.getHeaders()) {
      keys.add(header.getName());
    }
    return keys;
  }

  @Override
  public Collection<List<String>> values() {
    ArrayList<List<String>> values = new ArrayList<>(size());
    for (Header header : message.getHeaders()) {
      values.add(get(header.getName()));
    }
    return values;
  }

  @Override
  public Set<Map.Entry<String, List<String>>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<Map.Entry<String, List<String>>> iterator() {
        return new EntryIterator();
      }

      @Override
      public int size() {
        return HttpComponentsHeaders.this.size();
      }
    };
  }

  private final class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

    private final Iterator<Header> iterator = message.headerIterator();

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public Map.Entry<String, List<String>> next() {
      return new HeaderEntry(this.iterator.next().getName());
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
      List<String> values = HttpComponentsHeaders.this.get(key);
      return values != null ? values : Collections.emptyList();
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previousValues = getValue();
      HttpComponentsHeaders.this.put(this.key, value);
      return previousValues;
    }
  }

}
