/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.support;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
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
public final class Netty4HttpHeaders extends cn.taketoday.http.HttpHeaders {

  private final HttpHeaders headers;

  public Netty4HttpHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  @Nullable
  public String getFirst(String name) {
    return this.headers.get(name);
  }

  @Override
  public void add(String name, @Nullable String value) {
    if (value != null) {
      this.headers.add(name, value);
    }
  }

  @Override
  public void addAll(String key, @Nullable Collection<? extends String> values) {
    if (values != null) {
      this.headers.add(key, values);
    }
  }

  @Override
  public void setHeader(String name, String value) {
    this.headers.set(name, value);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(headers.size());
    for (final Entry<String, String> entry : headers) {
      singleValueMap.put(entry.getKey(), entry.getValue());
    }
    return singleValueMap;
  }

  @Override
  public int size() {
    return this.headers.names().size();
  }

  @Override
  public boolean isEmpty() {
    return this.headers.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return (key instanceof String headerName && this.headers.contains(headerName));
  }

  @Override
  public boolean containsValue(Object value) {
    if (value instanceof String) {
      for (final Entry<String, String> header : headers) {
        if (Objects.equals(header.getValue(), value)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  @Nullable
  public List<String> get(Object name) {
    if (containsKey(name)) {
      return this.headers.getAll((String) name);
    }
    return null;
  }

  @Nullable
  @Override
  public List<String> put(String key, @Nullable List<String> value) {
    List<String> previousValues = this.headers.getAll(key);
    this.headers.set(key, value);
    return previousValues;
  }

  @Nullable
  @Override
  public List<String> remove(Object name) {
    if (name instanceof String headerName) {
      List<String> previousValues = this.headers.getAll(headerName);
      this.headers.remove(headerName);
      return previousValues;
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> m) {
    for (var entry : m.entrySet()) {
      headers.set(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    this.headers.clear();
  }

  @Override
  public Set<String> keySet() {
    return new HeaderNames();
  }

  @Override
  public Collection<List<String>> values() {
    final var headers = this.headers;
    final ArrayList<List<String>> ret = new ArrayList<>(headers.size());
    for (final String name : headers.names()) {
      ret.add(headers.getAll(name));
    }
    return ret;
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
        return headers.size();
      }
    };
  }

  private class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

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

  private class HeaderEntry implements Map.Entry<String, List<String>> {

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
  }

  private class HeaderNames extends AbstractSet<String> {

    @Override
    public Iterator<String> iterator() {
      return new HeaderNamesIterator(headers.names().iterator());
    }

    @Override
    public int size() {
      return headers.names().size();
    }
  }

  private final class HeaderNamesIterator implements Iterator<String> {
    private final Iterator<String> iterator;

    @Nullable
    private String currentName;

    private HeaderNamesIterator(Iterator<String> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public String next() {
      this.currentName = this.iterator.next();
      return this.currentName;
    }

    @Override
    public void remove() {
      if (this.currentName == null) {
        throw new IllegalStateException("No current Header in iterator");
      }
      if (!headers.contains(this.currentName)) {
        throw new IllegalStateException("Header not present: " + this.currentName);
      }
      headers.remove(this.currentName);
    }
  }

}
