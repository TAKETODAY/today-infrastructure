package cn.taketoday.framework.reactive;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.web.http.HttpHeaders;

/**
 * Netty HttpHeaders
 *
 * @author TODAY 2021/3/30 18:30
 */
final class NettyHttpHeaders extends HttpHeaders {

  final io.netty.handler.codec.http.HttpHeaders headers;

  NettyHttpHeaders(io.netty.handler.codec.http.HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public String getFirst(String headerName) {
    return headers.get(headerName);
  }

  @Override
  public List<String> get(Object key) {
    return headers.getAll((CharSequence) key);
  }

  @Override
  public List<String> put(String key, List<String> value) {
    headers.set(key, value);
    return null;
  }

  @Override
  public List<String> remove(Object key) {
    headers.remove((CharSequence) key);
    return null;
  }

  @Override
  public void add(String headerName, String headerValue) {
    headers.add(headerName, headerValue);
  }

  @Override
  public void set(String headerName, String headerValue) {
    headers.set(headerName, headerValue);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    final HashMap<String, String> singleValueMap = new HashMap<>(size());
    for (final Entry<String, String> entry : headers) {
      singleValueMap.put(entry.getKey(), entry.getValue());
    }
    return singleValueMap;
  }

  //

  io.netty.handler.codec.http.HttpHeaders getOriginal() {
    return headers;
  }

  @Override
  public int size() {
    return headers.size();
  }

  @Override
  public boolean isEmpty() {
    return headers.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return headers.contains((CharSequence) key);
  }

  @Override
  public boolean containsValue(Object value) {
    for (final Entry<String, String> header : headers) {
      if (Objects.equals(header.getValue(), value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> m) {
    for (final Entry<? extends String, ? extends List<String>> entry : m.entrySet()) {
      headers.set(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    headers.clear();
  }

  @Override
  public Set<String> keySet() {
    return headers.names();
  }

  @Override
  public Collection<List<String>> values() {
    final ArrayList<List<String>> ret = new ArrayList<>();
    final io.netty.handler.codec.http.HttpHeaders headers = this.headers;
    for (final String name : headers.names()) {
      ret.add(headers.getAll(name));
    }
    return ret;
  }

  @Override
  public Set<Entry<String, List<String>>> entrySet() {
    final HashSet<Entry<String, List<String>>> ret = new HashSet<>();
    final io.netty.handler.codec.http.HttpHeaders headers = this.headers;
    for (final String name : headers.names()) {
      ret.add(new AbstractMap.SimpleEntry<>(name, headers.getAll(name)));
    }
    return ret;
  }

  @Override
  public Map<String, String[]> toArrayMap(final IntFunction<String[]> mappingFunction) {
    final HashMap<String, String[]> singleValueMap = new HashMap<>(headers.size());
    copyToArrayMap(singleValueMap, mappingFunction);
    return singleValueMap;
  }

  @Override
  public void copyToArrayMap(final Map<String, String[]> newMap, final IntFunction<String[]> mappingFunction) {
    Assert.notNull(newMap, "newMap must not be null");
    Assert.notNull(mappingFunction, "mappingFunction must not be null");
    final io.netty.handler.codec.http.HttpHeaders headers = this.headers;
    final Set<String> names = headers.names();
    for (final String name : names) {
      final List<String> values = headers.getAll(name);
      if (!CollectionUtils.isEmpty(values)) {
        final String[] toArray = values.toArray(mappingFunction.apply(values.size()));
        newMap.put(name, toArray);
      }
    }

  }
}
