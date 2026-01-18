package infra.web.service.invoker;

/**
 * A factory interface for creating HTTP request values builders.
 *
 * @param <T> the type of {@link HttpRequestValues} to be created by this factory
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/17 23:26
 */
public interface HttpRequestValuesCreator<T extends HttpRequestValues> {

  /**
   * Creates an HTTP request values builder.
   *
   * @return a new HTTP request values builder instance
   */
  T.Builder createBuilder();

}
