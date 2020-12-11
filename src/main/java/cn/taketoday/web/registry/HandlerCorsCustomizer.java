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

import cn.taketoday.web.handler.InterceptableRequestHandler;
import cn.taketoday.web.interceptor.CorsHandlerInterceptor;

/**
 * @author TODAY
 * @date 2020/12/10 23:37
 */
public class HandlerCorsCustomizer implements HandlerCustomizer {

  private CorsHandlerInterceptor corsInterceptor;

  public HandlerCorsCustomizer() {}

  public HandlerCorsCustomizer(CorsHandlerInterceptor corsInterceptor) {
    this.corsInterceptor = corsInterceptor;
  }

  public void setCorsInterceptor(final CorsHandlerInterceptor corsInterceptor) {
    this.corsInterceptor = corsInterceptor;
  }

  @Override
  public Object customize(final Object handler) {
    if (handler instanceof InterceptableRequestHandler) {
      final InterceptableRequestHandler requestHandler = (InterceptableRequestHandler) handler;
      requestHandler.setInterceptors(corsInterceptor);
    }
    return handler;
  }
}
