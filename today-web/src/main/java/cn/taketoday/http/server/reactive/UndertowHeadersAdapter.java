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

package cn.taketoday.http.server.reactive;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

/**
 * {@code MultiValueMap} implementation for wrapping Undertow HTTP headers.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
class UndertowHeadersAdapter implements MultiValueMap<String, String> {

  private final HeaderMap headers;

  UndertowHeadersAdapter(HeaderMap headers) {
    this.headers = headers;
  }

  @Override
  public String getFirst(String key) {
    return this.headers.getFirst(key);
  }

  @Override
  public void add(String key, @Nullable String value) {
    this.headers.add(HttpString.tryFromString(key), value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addAll(String key, @Nullable Collection<? extends String> values) {
    if (values != null) {
      headers.addAll(HttpString.tryFromString(key), (Collection<String>) values);
    }
  }

  @Override
  public void addAll(@Nullable Map<String, List<String>> values) {
    if (values != null) {
      for (Entry<String, List<String>> entry : values.entrySet()) {
        headers.addAll(HttpString.tryFromString(entry.getKey()), entry.getValue());
      }
    }
  }

  @Override
  public void set(String key, @Nullable String value) {
    this.headers.put(HttpString.tryFromString(key), value);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
    this.headers.forEach(values -> singleValueMap.put(values.getHeaderName().toString(), values.getFirst()));
    return singleValueMap;
  }

  @Override
  public int size() {
    return this.headers.size();
  }

  @Override
  public boolean isEmpty() {
    return (this.headers.size() == 0);
  }

  @Override
  public boolean containsKey(Object key) {
    return (key instanceof String headerName && this.headers.contains(headerName));
  }

  @Override
  public boolean containsValue(Object value) {
    return (value instanceof String &&
            this.headers.getHeaderNames()
                    .stream()
                    .map(this.headers::get)
                    .anyMatch(values -> values.contains(value)));
  }

  @Override
  @Nullable
  public List<String> get(Object key) {
    return (key instanceof String headerName ? this.headers.get(headerName) : null);
  }

  @Override
  @Nullable
  public List<String> put(String key, List<String> value) {
    HeaderValues previousValues = this.headers.get(key);
    this.headers.putAll(HttpString.tryFromString(key), value);
    return previousValues;
  }

  @Override
  @Nullable
  public List<String> remove(Object key) {
    if (key instanceof String headerName) {
      Collection<String> removed = this.headers.remove(headerName);
      if (removed != null) {
        return new ArrayList<>(removed);
      }
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> map) {
    map.forEach((key, values) -> this.headers.putAll(HttpString.tryFromString(key), values));
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
    return this.headers.getHeaderNames().stream()
            .map(this.headers::get)
            .collect(Collectors.toList());
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

  @Override
  public String toString() {
    return HttpHeaders.formatHeaders(this);
  }

  private class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

    private final Iterator<HttpString> names = headers.getHeaderNames().iterator();

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

    private final HttpString key;

    HeaderEntry(HttpString key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return this.key.toString();
    }

    @Override
    public List<String> getValue() {
      return headers.get(this.key);
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previousValues = headers.get(this.key);
      headers.putAll(this.key, value);
      return previousValues;
    }
  }

  private class HeaderNames extends AbstractSet<String> {

    @Override
    public Iterator<String> iterator() {
      return new HeaderNamesIterator(headers.getHeaderNames().iterator());
    }

    @Override
    public int size() {
      return headers.getHeaderNames().size();
    }
  }

  private final class HeaderNamesIterator implements Iterator<String> {

    private final Iterator<HttpString> iterator;

    @Nullable
    private String currentName;

    private HeaderNamesIterator(Iterator<HttpString> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public String next() {
      this.currentName = this.iterator.next().toString();
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
