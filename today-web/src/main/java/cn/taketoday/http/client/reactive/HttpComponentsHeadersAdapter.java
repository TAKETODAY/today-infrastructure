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

package cn.taketoday.http.client.reactive;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;

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

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * {@code MultiValueMap} implementation for wrapping Apache HttpComponents
 * HttpClient headers.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class HttpComponentsHeadersAdapter implements MultiValueMap<String, String> {

  private final HttpMessage message;

  HttpComponentsHeadersAdapter(HttpMessage message) {
    this.message = message;
  }

  @Nullable
  @Override
  public String getFirst(String key) {
    Header header = this.message.getFirstHeader(key);
    return header != null ? header.getValue() : null;
  }

  @Override
  public void add(String key, @Nullable String value) {
    this.message.addHeader(key, value);
  }

  @Override
  public void set(String key, @Nullable String value) {
    this.message.setHeader(key, value);
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
  public List<String> get(Object key) {
    ArrayList<String> values = null;
    if (containsKey(key)) {
      Header[] headers = this.message.getHeaders((String) key);
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
  public List<String> remove(Object key) {
    if (key instanceof String headerName) {
      List<String> oldValues = get(key);
      this.message.removeHeaders(headerName);
      return oldValues;
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> map) {
    map.forEach(this::put);
  }

  @Override
  public void clear() {
    this.message.setHeaders();
  }

  @Override
  public Set<String> keySet() {
    LinkedHashSet<String> keys = new LinkedHashSet<>(size());
    for (Header header : this.message.getHeaders()) {
      keys.add(header.getName());
    }
    return keys;
  }

  @Override
  public Collection<List<String>> values() {
    ArrayList<List<String>> values = new ArrayList<>(size());
    for (Header header : this.message.getHeaders()) {
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
        return HttpComponentsHeadersAdapter.this.size();
      }
    };
  }

  @Override
  public String toString() {
    return HttpHeaders.formatHeaders(this);
  }

  private class EntryIterator implements Iterator<Map.Entry<String, List<String>>> {

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
      List<String> values = HttpComponentsHeadersAdapter.this.get(this.key);
      return values != null ? values : Collections.emptyList();
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previousValues = getValue();
      HttpComponentsHeadersAdapter.this.put(this.key, value);
      return previousValues;
    }
  }

}
