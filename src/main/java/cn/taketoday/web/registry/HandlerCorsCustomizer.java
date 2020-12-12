/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.context.Ordered;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.annotation.CrossOrigin;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsProcessor;
import cn.taketoday.web.cors.DefaultCorsProcessor;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.interceptor.CorsHandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * @author TODAY
 * @date 2020/12/10 23:37
 */
public class HandlerCorsCustomizer implements HandlerMethodCustomizer {

  private CorsProcessor processor;

  @Override
  public Object customize(final HandlerMethod handler) {

    // 预防已经设置
    final HandlerInterceptor[] interceptors = handler.getInterceptors();
    if (ObjectUtils.isNotEmpty(interceptors)) {
      for (final HandlerInterceptor interceptor : interceptors) {
        if (interceptor instanceof CorsHandlerInterceptor) {
          return handler;
        }
      }
    }

    final CrossOrigin methodCrossOrigin = handler.getMethodAnnotation(CrossOrigin.class);
    final CrossOrigin classCrossOrigin = handler.getDeclaringClassAnnotation(CrossOrigin.class);

    if (classCrossOrigin == null && methodCrossOrigin == null) {
      // 没有 @CrossOrigin 配置
      return handler;
    }

    final CorsConfiguration config = new CorsConfiguration();
    config.applyPermitDefaultValues();

    config.updateCorsConfig(classCrossOrigin);
    config.updateCorsConfig(methodCrossOrigin);

    final CorsHandlerInterceptor interceptor = new CorsHandlerInterceptor(config);
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

}
