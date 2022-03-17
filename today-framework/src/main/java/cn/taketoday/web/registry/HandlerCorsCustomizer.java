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
package cn.taketoday.web.registry;

import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.annotation.CrossOrigin;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsProcessor;
import cn.taketoday.web.cors.DefaultCorsProcessor;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.interceptor.CorsHandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * @author TODAY 2020/12/10 23:37
 * @since 3.0
 */
public class HandlerCorsCustomizer implements AnnotationHandlerMethodCustomizer, EmbeddedValueResolverAware {
  private CorsProcessor processor;

  private StringValueResolver resolver;

  public HandlerCorsCustomizer() {
    this(new DefaultCorsProcessor());
  }

  public HandlerCorsCustomizer(CorsProcessor processor) {
    setCorsProcessor(processor);
  }

  @Override
  public Object customize(ActionMappingAnnotationHandler handler) {

    // 预防已经设置
    HandlerInterceptor[] interceptors = handler.getInterceptors();
    if (ObjectUtils.isNotEmpty(interceptors)) {
      for (HandlerInterceptor interceptor : interceptors) {
        if (interceptor instanceof CorsHandlerInterceptor) {
          return handler;
        }
      }
    }

    CrossOrigin methodCrossOrigin = handler.getMethod().getMethodAnnotation(CrossOrigin.class);
    CrossOrigin classCrossOrigin = handler.getMethod().getDeclaringClassAnnotation(CrossOrigin.class);

    if (classCrossOrigin == null && methodCrossOrigin == null) {
      // 没有 @CrossOrigin 配置
      return handler;
    }

    CorsConfiguration config = new CorsConfiguration();

    updateCorsConfig(config, classCrossOrigin);
    updateCorsConfig(config, methodCrossOrigin);

    // 覆盖默认
    config.applyPermitDefaultValues();

    CorsHandlerInterceptor interceptor = new CorsHandlerInterceptor(config);
    interceptor.setCorsProcessor(processor);
    interceptor.setOrder(Ordered.HIGHEST_PRECEDENCE);

    handler.addInterceptors(interceptor);

    return handler;
  }

  public CorsProcessor getProcessor() {
    return processor;
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

  public void updateCorsConfig(CorsConfiguration config, CrossOrigin annotation) {
    if (annotation == null) {
      return;
    }
    for (String origin : annotation.value()) {
      config.addAllowedOrigin(resolveExpressionValue(origin));
    }
    for (HttpMethod method : annotation.methods()) {
      config.addAllowedMethod(method.name());
    }
    for (String header : annotation.allowedHeaders()) {
      config.addAllowedHeader(resolveExpressionValue(header));
    }
    for (String header : annotation.exposedHeaders()) {
      config.addExposedHeader(resolveExpressionValue(header));
    }

    String allowCredentials = resolveExpressionValue(annotation.allowCredentials());
    if ("true".equalsIgnoreCase(allowCredentials)) {
      config.setAllowCredentials(true);
    }
    else if ("false".equalsIgnoreCase(allowCredentials)) {
      config.setAllowCredentials(false);
    }
    else if (!allowCredentials.isEmpty()) {
      throw new IllegalStateException(
              "@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
                      "or an empty string (\"\"): current value is [" + allowCredentials + "]");
    }

    if (annotation.maxAge() >= 0 && config.getMaxAge() == null) {
      config.setMaxAge(annotation.maxAge());
    }
  }

  protected String resolveExpressionValue(String value) {
    if (resolver != null) {
      return resolver.resolveStringValue(value);
    }
    return value;
  }

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.resolver = resolver;
  }
}
