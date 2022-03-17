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
package cn.taketoday.web.handler;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptorsProvider;
import cn.taketoday.web.interceptor.InterceptorChain;

/**
 * @author TODAY 2020/12/10 22:51
 */
public abstract class InterceptableHandlerAdapter
        extends AbstractHandlerAdapter implements HandlerAdapter {

  @Override
  public final Object handle(final RequestContext context, final Object handler) throws Throwable {
    if (handler instanceof HandlerInterceptorsProvider) {
      final HandlerInterceptor[] interceptors = ((HandlerInterceptorsProvider) handler).getInterceptors();
      if (interceptors != null) {
        return new InterceptorChain(interceptors) {
          @Override
          protected Object proceedTarget(RequestContext context, Object handler) throws Throwable {
            return handleInternal(context, handler);
          }
        }.proceed(context, handler);
      }
    }
    return handleInternal(context, handler);
  }

  protected abstract Object handleInternal(final RequestContext context,
                                           final Object handler) throws Throwable;

}
