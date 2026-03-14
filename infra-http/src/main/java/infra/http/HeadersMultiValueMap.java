package infra.http;

import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import infra.util.MultiValueMap;

/**
 * Adapter that exposes {@link HttpHeaders} as a {@link MultiValueMap}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/14 20:35
 */
final class HeadersMultiValueMap extends AbstractMap<String, List<String>> implements MultiValueMap<String, String> {

  private final HttpHeaders headers;

  public HeadersMultiValueMap(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public @Nullable String getFirst(String key) {
    return headers.getFirst(key);
  }

  @Override
  public void add(String key, @Nullable String value) {
    headers.add(key, value);
  }

  @Override
  public void addAll(@Nullable Map<String, List<String>> values) {
    headers.addAll(values);
  }

  @Override
  public @Nullable List<String> setOrRemove(String key, @Nullable String value) {
    return headers.setOrRemove(key, value);
  }

  @Override
  public @Nullable List<String> setOrRemove(String key, String @Nullable [] value) {
    return headers.setOrRemove(key, value);
  }

  @Override
  public @Nullable List<String> setOrRemove(String key, @Nullable Collection<String> value) {
    return headers.setOrRemove(key, value);
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
    return key instanceof String name && headers.contains(name);
  }

  @Override
  public boolean containsValue(Object value) {
    for (Entry<String, List<String>> entry : headers.entries()) {
      for (String string : entry.getValue()) {
        if (Objects.equals(string, value)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public @Nullable List<String> get(Object key) {
    return key instanceof String name ? headers.get(name) : null;
  }

  @Override
  public @Nullable List<String> put(String key, List<String> value) {
    return headers.setHeader(key, value);
  }

  @Override
  public @Nullable List<String> remove(Object key) {
    return key instanceof String name ? headers.remove(name) : null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void putAll(Map m) {
    headers.setAll(m);
  }

  @Override
  public @Nullable List<String> putIfAbsent(String key, List<String> value) {
    return headers.setIfAbsent(key, value);
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
    return headers.entries().stream().map(Entry::getValue).toList();
  }

  @Override
  public Set<Entry<String, List<String>>> entrySet() {
    return headers.entries();
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super List<String>> action) {
    headers.forEach(action);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    return headers.toSingleValueMap();
  }

  @Override
  public String toString() {
    return headers.toString();
  }

}
