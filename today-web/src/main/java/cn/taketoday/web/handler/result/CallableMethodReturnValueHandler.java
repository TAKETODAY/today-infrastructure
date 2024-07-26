/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.result;

import java.util.concurrent.Callable;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * Handles return values of type {@link Callable}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/24 17:47
 */
public class CallableMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturn(Callable.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof Callable<?>;
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof Callable<?> callable) {
      context.getAsyncManager().startCallableProcessing(callable, handler);
    }
    else if (HandlerMethod.isHandler(handler)) {
      startCallableProcessing(context, handler, returnValue);
    }
  }

  static void startCallableProcessing(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    context.getAsyncManager().startCallableProcessing(() -> returnValue, handler);
  }

}
