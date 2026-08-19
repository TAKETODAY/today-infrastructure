package infra.web.filter;

import infra.util.Assert;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.cors.PreFlightRequestHandler;

/**
 * Filter that handles pre-flight requests through a
 * {@link PreFlightRequestHandler} and bypasses the rest of the chain.
 *
 * <p>The {@code @EnableWebMvc} config declares a bean of type
 * {@code PreFlightRequestHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/26 08:45
 */
public class PreFlightRequestFilter implements Filter {

  private final PreFlightRequestHandler handler;

  public PreFlightRequestFilter(PreFlightRequestHandler handler) {
    Assert.notNull(handler, "PreFlightRequestHandler is required");
    this.handler = handler;
  }

  @Override
  public void doFilter(HttpContext http, FilterChain chain) throws Exception {
    if (http.isPreFlightRequest()) {
      handler.handlePreFlight(http);
    }
    else {
      chain.doFilter(http);
    }
  }
}
