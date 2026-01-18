package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A decorator for {@link RequestExecution} that wraps the result of the delegated execution in an {@link Optional}.
 * This ensures that even null results from the underlying execution are represented as a non-null Optional.empty().
 *
 * @param <T> the type of the request values, must extend {@link HttpRequestValues}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/17 23:02
 */
public class WrapOptionalExecutionDecorator<T extends HttpRequestValues> implements RequestExecution<T> {

  private final RequestExecution<T> delegate;

  public WrapOptionalExecutionDecorator(RequestExecution<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public @Nullable Object execute(T requestValues) {
    return Optional.ofNullable(delegate.execute(requestValues));
  }

}
