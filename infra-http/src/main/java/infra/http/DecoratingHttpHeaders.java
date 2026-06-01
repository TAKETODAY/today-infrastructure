package infra.http;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.lang.Assert;

/**
 * A decorator for {@link HttpHeaders} that delegates all operations to an underlying instance.
 * <p>This class allows for adding custom behavior to HTTP headers while maintaining the
 * original interface contract. Subclasses can override specific methods to intercept
 * or modify header operations.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/6/1 15:04
 */
public class DecoratingHttpHeaders extends HttpHeaders {

  protected final HttpHeaders delegate;

  public DecoratingHttpHeaders(HttpHeaders delegate) {
    Assert.notNull(delegate, "delegate is required");
    this.delegate = delegate;
  }

  @Override
  public @Nullable String getFirst(String name) {
    return delegate.getFirst(name);
  }

  @Override
  public void add(String name, @Nullable String value) {
    delegate.add(name, value);
  }

  @Override
  protected @Nullable List<String> setHeader(String name, String value) {
    return delegate.setHeader(name, value);
  }

  @Override
  protected @Nullable List<String> setHeader(String name, Collection<String> values) {
    return delegate.setHeader(name, values);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public @Nullable List<String> get(String name) {
    return delegate.get(name);
  }

  @Override
  public @Nullable List<String> remove(String name) {
    return delegate.remove(name);
  }

  @Override
  public Set<String> names() {
    return delegate.names();
  }

  @Override
  public Set<Map.Entry<String, List<String>>> entries() {
    return delegate.entries();
  }

  @Override
  public String toString() {
    return "%s [delegate=%s]".formatted(getClass().getSimpleName(), delegate);
  }

}
