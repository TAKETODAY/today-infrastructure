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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import io.netty5.handler.codec.http.headers.HttpHeaders;

/**
 * {@code MultiValueMap} implementation for wrapping Netty HTTP headers.
 *
 * <p>This class is based on {@link Netty4HttpHeaders}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class Netty5HttpHeaders extends cn.taketoday.http.HttpHeaders {

  private final HttpHeaders headers;

  /**
   * Create a new {@code Netty5HeadersAdapter} based on the given
   * {@code HttpHeaders}.
   */
  public Netty5HttpHeaders(HttpHeaders headers) {
    Assert.notNull(headers, "Headers is required");
    this.headers = headers;
  }

  @Override
  @Nullable
  public String getFirst(String name) {
    CharSequence value = this.headers.get(name);
    return (value != null ? value.toString() : null);
  }

  @Override
  public void add(String name, @Nullable String value) {
    if (value != null) {
      this.headers.add(name, value);
    }
  }

  @Override
  public void addAll(String key, @Nullable Collection<? extends String> values) {
    this.headers.add(key, values);
  }

  @Override
  public void setHeader(String name, String value) {
    this.headers.set(name, value);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
    this.headers.forEach(entry -> singleValueMap.putIfAbsent(
            entry.getKey().toString(), entry.getValue().toString()));
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
    if (value instanceof CharSequence) {
      for (final Entry<CharSequence, CharSequence> header : headers) {
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
    Iterator<CharSequence> iterator = this.headers.valuesIterator((CharSequence) name);
    if (iterator.hasNext()) {
      List<String> result = new ArrayList<>();
      iterator.forEachRemaining(value -> result.add(value.toString()));
      return result;
    }
    return null;
  }

  @Nullable
  @Override
  public List<String> put(String key, @Nullable List<String> value) {
    List<String> previousValues = get(key);
    this.headers.set(key, value);
    return previousValues;
  }

  @Nullable
  @Override
  public List<String> remove(Object name) {
    if (name instanceof String headerName) {
      List<String> previousValues = get(headerName);
      this.headers.remove(headerName);
      return previousValues;
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> map) {
    map.forEach(this.headers::set);
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
    List<List<String>> result = new ArrayList<>(this.headers.size());
    forEach((key, value) -> result.add(value));
    return result;
  }

  @Override
  public Set<Entry<String, List<String>>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<Entry<String, List<String>>> iterator() {
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
    return cn.taketoday.http.HttpHeaders.formatHeaders(this);
  }

  private class EntryIterator implements Iterator<Entry<String, List<String>>> {

    private final Iterator<CharSequence> names = headers.names().iterator();

    @Override
    public boolean hasNext() {
      return this.names.hasNext();
    }

    @Override
    public Entry<String, List<String>> next() {
      return new HeaderEntry(this.names.next());
    }
  }

  private class HeaderEntry implements Entry<String, List<String>> {

    private final CharSequence key;

    HeaderEntry(CharSequence key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return this.key.toString();
    }

    @Override
    public List<String> getValue() {
      List<String> values = get(this.key);
      return (values != null ? values : Collections.emptyList());
    }

    @Override
    public List<String> setValue(List<String> value) {
      List<String> previousValues = getValue();
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

    private final Iterator<CharSequence> iterator;

    @Nullable
    private CharSequence currentName;

    private HeaderNamesIterator(Iterator<CharSequence> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public String next() {
      this.currentName = this.iterator.next();
      return this.currentName.toString();
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
