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
package cn.taketoday.web.interceptor;

import java.io.IOException;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsConfigurationCapable;
import cn.taketoday.web.cors.CorsConfigurationSource;
import cn.taketoday.web.cors.CorsProcessor;
import cn.taketoday.web.cors.CorsUtils;
import cn.taketoday.web.cors.DefaultCorsProcessor;

/**
 * @author TODAY 2020/12/10 22:49
 */
public class CorsHandlerInterceptor
        extends OrderedSupport implements HandlerInterceptor {

  private CorsProcessor processor;
  private CorsConfigurationSource configSource;

  public CorsHandlerInterceptor() { }

  public CorsHandlerInterceptor(CorsConfiguration configSource) {
    setConfigSource(configSource);
  }

  public CorsHandlerInterceptor(CorsConfigurationSource configSource) {
    setConfigSource(configSource);
  }

  @Override
  public boolean beforeProcess(final RequestContext context, final Object handler) throws IOException {
    final CorsConfiguration corsConfiguration = getCorsConfiguration(context, handler);
    if (corsConfiguration == null) {
      return true;
    }
    return getProcessor().process(corsConfiguration, context)
            && !CorsUtils.isPreFlightRequest(context);
  }

  protected CorsConfiguration getCorsConfiguration(final RequestContext context, Object handler) {
    if (configSource != null) {
      return configSource.getCorsConfiguration(context);
    }
    if (handler instanceof CorsConfigurationCapable) {
      return ((CorsConfigurationCapable) handler).getCorsConfiguration();
    }
    else if (handler instanceof CorsConfigurationSource) {
      return ((CorsConfigurationSource) handler).getCorsConfiguration(context);
    }
    return null;
  }

  public final CorsProcessor getProcessor() {
    CorsProcessor processor = this.processor;
    if (processor == null) {
      processor = createProcessor();
      this.processor = processor;
    }
    return processor;
  }

  protected DefaultCorsProcessor createProcessor() {
    return new DefaultCorsProcessor();
  }

  /**
   * Configure a custom {@link CorsProcessor} to use to apply the matched
   * {@link CorsConfiguration} for a request.
   * <p>By default {@link DefaultCorsProcessor} is used.
   */
  public void setCorsProcessor(CorsProcessor processor) {
    this.processor = processor;
  }

  public CorsConfigurationSource getConfigSource() {
    return configSource;
  }

  /**
   * Configure a custom {@link CorsConfigurationSource}
   */
  public void setConfigSource(final CorsConfigurationSource configSource) {
    this.configSource = configSource;
  }

  /**
   * Configure a custom {@link CorsConfiguration}
   */
  public void setConfigSource(final CorsConfiguration configSource) {
    this.configSource = context -> configSource;
  }

}
