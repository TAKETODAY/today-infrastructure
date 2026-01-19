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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.cors;

import infra.lang.Assert;
import infra.web.HandlerInterceptor;
import infra.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/21 17:24
 */
public class CorsInterceptor implements HandlerInterceptor {

  private final CorsConfigurationSource configSource;

  private CorsProcessor processor = new DefaultCorsProcessor();

  /**
   * Constructor accepting a {@link CorsConfigurationSource} used by the filter
   * to find the {@link CorsConfiguration} to use for each incoming request.
   */
  public CorsInterceptor(CorsConfigurationSource configSource) {
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
  public boolean preProcessing(RequestContext request, Object handler) throws Throwable {
    CorsConfiguration corsConfiguration = configSource.getCorsConfiguration(request);
    return processor.process(corsConfiguration, request)
            && !request.isPreFlightRequest();
  }

}
