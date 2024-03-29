/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.support;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Jetty HTTP headers.
 *
 * <p>There is a duplicate of this class in the server package!
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class JettyHeadersAdapter implements MultiValueMap<String, String> {

  private final HttpFields headers;

  /**
   * Creates a new {@code JettyHeadersAdapter} based on the given
   * {@code HttpFields} instance.
   *
   * @param headers the {@code HttpFields} to base this adapter on
   */
  public JettyHeadersAdapter(HttpFields headers) {
    Assert.notNull(headers, "Headers is required");
    this.headers = headers;
  }

  @Override
  public String getFirst(String key) {
    return this.headers.get(key);
  }

  @Override
  public void add(String key, @Nullable String value) {
    if (value != null) {
      HttpFields.Mutable mutableHttpFields = mutableFields();
      mutableHttpFields.add(key, value);
    }
  }

  @Override
  public void addAll(String key, Collection<? extends String> values) {
    values.forEach(value -> add(key, value));
  }

  @Override
  public void set(String key, @Nullable String value) {
    HttpFields.Mutable mutableHttpFields = mutableFields();
    if (value != null) {
      mutableHttpFields.put(key, value);
    }
    else {
      mutableHttpFields.remove(key);
    }
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(this.headers.size());
    Iterator<HttpField> iterator = this.headers.iterator();
    iterator.forEachRemaining(field -> {
      if (!singleValueMap.containsKey(field.getName())) {
        singleValueMap.put(field.getName(), field.getValue());
      }
    });
    return singleValueMap;
  }

  @Override
  public int size() {
    return this.headers.getFieldNamesCollection().size();
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
    return (value instanceof String searchString &&
            this.headers.stream().anyMatch(field -> field.contains(searchString)));
  }

  @Nullable
  @Override
  public List<String> get(Object key) {
    if (containsKey(key)) {
      return this.headers.getValuesList((String) key);
    }
    return null;
  }

  @Nullable
  @Override
  public List<String> put(String key, List<String> value) {
    HttpFields.Mutable mutableHttpFields = mutableFields();
    List<String> oldValues = get(key);
    mutableHttpFields.put(key, value);
    return oldValues;
  }

  @Nullable
  @Override
  public List<String> remove(Object key) {
    HttpFields.Mutable mutableHttpFields = mutableFields();
    if (key instanceof String name) {
      List<String> oldValues = get(key);
      mutableHttpFields.remove(name);
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
    HttpFields.Mutable mutableHttpFields = mutableFields();
    mutableHttpFields.clear();
  }

  @Override
  public Set<String> keySet() {
    return new HeaderNames();
  }

  @Override
  public Collection<List<String>> values() {
    return this.headers.getFieldNamesCollection().stream()
            .map(this.headers::getValuesList).toList();
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

  private HttpFields.Mutable mutableFields() {
    if (this.headers instanceof HttpFields.Mutable mutableHttpFields) {
      return mutableHttpFields;
    }
    else {
      throw new IllegalStateException("Immutable headers");
    }
  }

  @Override
  public String toString() {
    return HttpHeaders.formatHeaders(this);
  }

  private class EntryIterator implements Iterator<Entry<String, List<String>>> {

    private final Iterator<String> names = headers.getFieldNamesCollection().iterator();

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
      return headers.getValuesList(this.key);
    }

    @Override
    public List<String> setValue(List<String> value) {
      HttpFields.Mutable mutableHttpFields = mutableFields();
      List<String> previousValues = headers.getValuesList(this.key);
      mutableHttpFields.put(this.key, value);
      return previousValues;
    }
  }

  private class HeaderNames extends AbstractSet<String> {

    @Override
    public Iterator<String> iterator() {
      return new HeaderNamesIterator(headers.getFieldNamesCollection().iterator());
    }

    @Override
    public int size() {
      return headers.getFieldNamesCollection().size();
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
      HttpFields.Mutable mutableHttpFields = mutableFields();
      if (this.currentName == null) {
        throw new IllegalStateException("No current Header in iterator");
      }
      if (!headers.contains(this.currentName)) {
        throw new IllegalStateException("Header not present: " + this.currentName);
      }
      mutableHttpFields.remove(this.currentName);
    }
  }

}
