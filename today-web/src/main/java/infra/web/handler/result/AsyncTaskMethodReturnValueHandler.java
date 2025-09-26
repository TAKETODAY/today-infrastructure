/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanFactory;
import infra.web.RequestContext;
import infra.web.async.WebAsyncTask;
import infra.web.handler.method.HandlerMethod;

import static infra.web.handler.result.CallableMethodReturnValueHandler.startCallableProcessing;

/**
 * for WebAsyncTask
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:03
 */
public class AsyncTaskMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Nullable
  private final BeanFactory beanFactory;

  public AsyncTaskMethodReturnValueHandler(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof WebAsyncTask;
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(WebAsyncTask.class);
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof WebAsyncTask<?> task) {
      if (this.beanFactory != null) {
        task.setBeanFactory(this.beanFactory);
      }
      context.asyncManager().startCallableProcessing(task, handler);
    }
    else if (HandlerMethod.isHandler(handler)) {
      startCallableProcessing(context, handler, returnValue);
    }
  }

}
