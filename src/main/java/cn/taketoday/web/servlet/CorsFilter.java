package cn.taketoday.web.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.GenericFilter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsConfigurationSource;
import cn.taketoday.web.cors.CorsProcessor;
import cn.taketoday.web.cors.DefaultCorsProcessor;
import cn.taketoday.web.utils.ServletUtils;
import cn.taketoday.web.utils.WebUtils;

/**
 * {@link javax.servlet.Filter} that handles CORS preflight requests and intercepts
 * CORS simple and actual requests thanks to a {@link CorsProcessor} implementation
 * ({@link DefaultCorsProcessor} by default) in order to add the relevant CORS
 * response headers (like {@code Access-Control-Allow-Origin}) using the provided
 * {@link CorsConfigurationSource}
 *
 * @author Sebastien Deleuze
 * @author TODAY
 * @date 2020/12/8 22:27
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @since 3.0
 */
public class CorsFilter extends GenericFilter implements Filter {

  private final CorsConfigurationSource configSource;

  private CorsProcessor processor = new DefaultCorsProcessor();

  /**
   * Constructor accepting a {@link CorsConfigurationSource} used by the filter
   * to find the {@link CorsConfiguration} to use for each incoming request.
   */
  public CorsFilter(CorsConfigurationSource configSource) {
    Assert.notNull(configSource, "CorsConfigurationSource must not be null");
    this.configSource = configSource;
  }

  /**
   * Configure a custom {@link CorsProcessor} to use to apply the matched
   * {@link CorsConfiguration} for a request.
   * <p>By default {@link DefaultCorsProcessor} is used.
   */
  public void setCorsProcessor(CorsProcessor processor) {
    Assert.notNull(processor, "CorsProcessor must not be null");
    this.processor = processor;
  }

  @Override
  public void doFilter(final ServletRequest request,
                       final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {

    final RequestContext context = ServletUtils.getRequestContext(request, response);
    try {
      CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(context);
      boolean isValid = this.processor.process(corsConfiguration, context);
      if (!isValid || WebUtils.isPreFlightRequest(context)) {
        return;
      }
      chain.doFilter(request, response);
    }
    finally {
      RequestContextHolder.resetContext();
    }
  }

}
