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

import java.util.concurrent.Callable;

import infra.web.RequestContext;
import infra.web.handler.method.HandlerMethod;

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
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof Callable<?> callable) {
      context.asyncManager().startCallableProcessing(callable, handler);
    }
    else if (HandlerMethod.isHandler(handler)) {
      startCallableProcessing(context, handler, returnValue);
    }
  }

  static void startCallableProcessing(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    context.asyncManager().startCallableProcessing(() -> returnValue, handler);
  }

}
