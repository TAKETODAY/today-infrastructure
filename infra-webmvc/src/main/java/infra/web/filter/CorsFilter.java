/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.filter;

import infra.lang.Assert;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.cors.CorsConfiguration;
import infra.web.cors.CorsConfigurationSource;
import infra.web.cors.CorsProcessor;
import infra.web.cors.DefaultCorsProcessor;
import infra.web.cors.UrlBasedCorsConfigurationSource;

/**
 * {@link infra.web.Filter} to handle CORS pre-flight requests and intercept
 * CORS simple and actual requests with a {@link CorsProcessor}, and to update
 * the response, for example, with CORS response headers, based on the policy matched
 * through the provided {@link CorsConfigurationSource}.
 *
 * <p>This is an alternative to configuring CORS in the Infra MVC Java config
 * and the Infra MVC XML namespace. It is useful for applications depending
 * only on infra-web (not on infra-webmvc) or for security constraints that
 * require CORS checks to be performed at {@link infra.web.Filter} level.
 *
 * <p>This filter could be used in conjunction with {@link DelegatingFilterProxy}
 * in order to help with its initialization.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @see UrlBasedCorsConfigurationSource
 * @since 5.0
 */
public class CorsFilter implements Filter {

  private final CorsConfigurationSource configSource;

  private CorsProcessor processor = new DefaultCorsProcessor();

  /**
   * Constructor accepting a {@link CorsConfigurationSource} used by the filter
   * to find the {@link CorsConfiguration} to use for each incoming request.
   *
   * @see UrlBasedCorsConfigurationSource
   */
  public CorsFilter(CorsConfigurationSource configSource) {
    Assert.notNull(configSource, "CorsConfigurationSource is required");
    this.configSource = configSource;
  }

  /**
   * Configure a custom {@link CorsProcessor} to use to apply the matched
   * {@link CorsConfiguration} for a request.
   * <p>By default {@link DefaultCorsProcessor} is used.
   */
  public void setCorsProcessor(CorsProcessor processor) {
    Assert.notNull(processor, "CorsProcessor is required");
    this.processor = processor;
  }

  @Override
  public void doFilter(HttpContext request, FilterChain chain) throws Exception {
    CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(request);
    boolean isValid = this.processor.process(corsConfiguration, request);
    if (!isValid || request.isPreFlightRequest()) {
      return;
    }
    chain.doFilter(request);
  }

}
