/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.ArrayList;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.HandlerInterceptor;

/**
 * @author TODAY 2021/8/30 21:38
 * @since 4.0
 */
public class InterceptorRegistration {

  protected HandlerInterceptor interceptor;
  private int order = 0;

  private final ArrayList<String> includePatterns = new ArrayList<>();
  private final ArrayList<String> excludePatterns = new ArrayList<>();

  /**
   * Create an {@link InterceptorRegistration} instance.
   */
  public InterceptorRegistration(HandlerInterceptor interceptor) {
    Assert.notNull(interceptor, "Interceptor is required");
    this.interceptor = interceptor;
  }

  public InterceptorRegistration setInterceptor(HandlerInterceptor interceptor) {
    this.interceptor = interceptor;
    return this;
  }

  public InterceptorRegistration addPathPatterns(String... pattern) {
    CollectionUtils.addAll(includePatterns, pattern);
    return this;
  }

  public InterceptorRegistration excludePathPatterns(String... pattern) {
    CollectionUtils.addAll(excludePatterns, pattern);
    return this;
  }

  protected final boolean matchInRuntime(String requestPath, PathMatcher pathMatcher) {
    // exclude
    for (String excludePattern : excludePatterns) {
      if (pathMatcher.match(excludePattern, requestPath)) {
        return false;
      }
    }

    // include
    for (String includePattern : includePatterns) {
      if (pathMatcher.match(includePattern, requestPath)) {
        return true;
      }
    }

    return false;
  }

  //

  public HandlerInterceptor getInterceptor() {
    return interceptor;
  }

  /**
   * Specify an order position to be used. Default is 0.
   */
  public InterceptorRegistration order(int order) {
    this.order = order;
    return this;
  }

  /**
   * Return the order position to be used.
   */
  protected int getOrder() {
    return this.order;
  }

}
