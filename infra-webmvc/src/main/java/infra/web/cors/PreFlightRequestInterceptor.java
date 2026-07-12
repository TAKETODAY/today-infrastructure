package infra.web.cors;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.HandlerInterceptor;
import infra.web.HttpContext;
import infra.web.InterceptorChain;

/**
 * HandlerInterceptor that handles pre-flight requests through a
 * {@link PreFlightRequestHandler} and bypasses the rest of the chain.
 *
 * <p>The {@code @EnableWebMvc} config declares a bean of type
 * {@code PreFlightRequestHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/26 08:45
 */
public class PreFlightRequestInterceptor implements HandlerInterceptor {

  private final PreFlightRequestHandler handler;

  public PreFlightRequestInterceptor(PreFlightRequestHandler handler) {
    Assert.notNull(handler, "PreFlightRequestHandler is required");
    this.handler = handler;
  }

  @Override
  public @Nullable Object intercept(HttpContext request, InterceptorChain chain) throws Throwable {
    if (request.isPreFlightRequest()) {
      handler.handlePreFlight(request);
      return NONE_RETURN_VALUE;
    }
    return chain.proceed(request);
  }

}
