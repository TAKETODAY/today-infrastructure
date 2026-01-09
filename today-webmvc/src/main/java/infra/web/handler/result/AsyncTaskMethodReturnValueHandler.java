/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
