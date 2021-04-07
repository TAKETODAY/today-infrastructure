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

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptorsCapable;

/**
 * @author TODAY 2020/12/10 22:51
 */
public abstract class InterceptableHandlerAdapter
        extends AbstractHandlerAdapter implements HandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(InterceptableHandlerAdapter.class);

  @Override
  public final Object handle(final RequestContext context, final Object handler) throws Throwable {

    if(handler instanceof HandlerInterceptorsCapable) {
      final HandlerInterceptor[] interceptors = ((HandlerInterceptorsCapable) handler).getInterceptors();
      if (interceptors != null) {
        // before
        for (final HandlerInterceptor intercepter : interceptors) {
          if (!intercepter.beforeProcess(context, handler)) {
            if (log.isDebugEnabled()) {
              log.debug("Interceptor: [{}] return false", intercepter);
            }
            return HandlerAdapter.NONE_RETURN_VALUE;
          }
        }
        // handle
        final Object result = handleInternal(context, handler);
        // after
        for (final HandlerInterceptor intercepter : interceptors) {
          intercepter.afterProcess(context, handler, result);
        }
        return result;
      }
    }
    return handleInternal(context, handler);
  }

  protected abstract Object handleInternal(final RequestContext context,
                                           final Object handler) throws Throwable;


}
