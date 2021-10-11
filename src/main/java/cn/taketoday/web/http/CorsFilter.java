/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.GenericFilter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.servlet.ServletUtils;

/**
 * {@link javax.servlet.Filter} that handles CORS preflight requests and intercepts
 * CORS simple and actual requests thanks to a {@link CorsProcessor} implementation
 * ({@link DefaultCorsProcessor} by default) in order to add the relevant CORS
 * response headers (like {@code Access-Control-Allow-Origin}) using the provided
 * {@link CorsConfigurationSource}
 *
 * @author Sebastien Deleuze
 * @author TODAY 2020/12/8 22:27
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
      if (!this.processor.process(corsConfiguration, context)
              || WebUtils.isPreFlightRequest(context)) {
        return;
      }
      // handle next
      chain.doFilter(request, response);
    }
    finally {
      RequestContextHolder.resetContext();
    }
  }

}
