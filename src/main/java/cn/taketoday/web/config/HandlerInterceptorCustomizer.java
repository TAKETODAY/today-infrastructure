/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.config;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.web.handler.InterceptableRequestHandler;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.registry.HandlerCustomizer;

/**
 * @author TODAY 2021/8/29 22:32
 * @since 4.0
 */
public class HandlerInterceptorCustomizer implements HandlerCustomizer {
  private MultiValueMap<String, HandlerInterceptor> interceptors;

  private PathMatcher pathMatcher = new AntPathMatcher();

  public HandlerInterceptorCustomizer(MultiValueMap<String, HandlerInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public Object customize(String handlerKey, Object handler) {
    MultiValueMap<String, HandlerInterceptor> interceptors = getInterceptors();
    if (interceptors != null && handler instanceof InterceptableRequestHandler) {
      PathMatcher matcher = getPathMatcher();
      for (Map.Entry<String, List<HandlerInterceptor>> entry : interceptors.entrySet()) {
        String key = entry.getKey();
        if (matcher.isPattern(key)) {
          if (matcher.isPattern(handlerKey)) {
            // TODO
          }
          else {
            if (matcher.match(key, handlerKey)) {
              List<HandlerInterceptor> interceptorList = entry.getValue();
              ((InterceptableRequestHandler) handler).addInterceptors(interceptorList);
            }
          }
        }
        else {
          if (matcher.isPattern(handlerKey)) {
            if (matcher.match(handlerKey, key)) {
              List<HandlerInterceptor> interceptorList = entry.getValue();
              ((InterceptableRequestHandler) handler).addInterceptors(interceptorList);
            }
          }
          else {
            // TODO

          }
        }

      }
    }
    return handler;
  }

  public void setPathMatcher(PathMatcher pathMatcher) {
    this.pathMatcher = pathMatcher;
  }

  public PathMatcher getPathMatcher() {
    return pathMatcher;
  }

  public void setInterceptors(MultiValueMap<String, HandlerInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public MultiValueMap<String, HandlerInterceptor> getInterceptors() {
    return interceptors;
  }
}
