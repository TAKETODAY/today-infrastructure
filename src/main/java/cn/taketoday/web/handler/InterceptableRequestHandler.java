/**
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
package cn.taketoday.web.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptorsCapable;
import cn.taketoday.web.interceptor.InterceptorChain;

/**
 * @author TODAY 2019-12-25 16:19
 */
public abstract class InterceptableRequestHandler
        extends OrderedSupport implements RequestHandler, HandlerInterceptorsCapable {

  /** interceptors array */
  private HandlerInterceptor[] interceptors;

  public InterceptableRequestHandler() { }

  public InterceptableRequestHandler(HandlerInterceptor... interceptors) {
    setInterceptors(interceptors);
  }

  /**
   * perform {@link HandlerInterceptor} on this handler
   *
   * @param context
   *         Current request context
   *
   * @return handler's result
   *
   * @throws Throwable
   *         any exception occurred in this request context
   */
  @Override
  public Object handleRequest(final RequestContext context) throws Throwable {
    HandlerInterceptor[] interceptors = getInterceptors();
    if (interceptors == null) {
      return handleInternal(context);
    }
    // @since 4.0
    return new DefaultInterceptorChain(interceptors).proceed(context, this);
  }

  /**
   * perform this handler' behavior internal
   */
  protected abstract Object handleInternal(final RequestContext context)
          throws Throwable;

  /**
   * replace interceptors
   *
   * @param interceptors
   *         interceptors to add
   *
   * @since 3.0.1
   */
  public void setInterceptors(HandlerInterceptor... interceptors) {
    if (ObjectUtils.isNotEmpty(interceptors)) {
      sort(interceptors);
    }
    this.interceptors = interceptors;
  }

  protected void sort(HandlerInterceptor[] interceptors) {
    OrderUtils.reversedSort(interceptors);
  }

  /**
   * add interceptors at end of the {@link #interceptors}
   *
   * @param interceptors
   *         interceptors to add
   */
  public void addInterceptors(HandlerInterceptor... interceptors) {
    final ArrayList<HandlerInterceptor> objects = new ArrayList<>();
    if (this.interceptors != null) {
      Collections.addAll(objects, this.interceptors);
    }
    Collections.addAll(objects, interceptors);
    setInterceptors(objects);
  }

  /**
   * add interceptors at end of the {@link #interceptors}
   *
   * @param interceptors
   *         interceptors to add
   *
   * @since 3.0.1
   */
  public void addInterceptors(List<HandlerInterceptor> interceptors) {
    final ArrayList<HandlerInterceptor> objects = new ArrayList<>();
    if (this.interceptors != null) {
      Collections.addAll(objects, this.interceptors);
    }
    objects.addAll(interceptors);
    setInterceptors(objects);
  }

  public void setInterceptors(List<HandlerInterceptor> interceptors) {
    setInterceptors(ObjectUtils.isEmpty(interceptors)
                    ? null
                    : interceptors.toArray(new HandlerInterceptor[interceptors.size()]));
  }

  @Override
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  private final class DefaultInterceptorChain extends InterceptorChain {

    private DefaultInterceptorChain(HandlerInterceptor[] interceptors) {
      super(interceptors);
    }

    @Override
    protected Object proceedTarget(RequestContext context, Object handler) throws Throwable {
      return handleInternal(context);
    }
  }

}
