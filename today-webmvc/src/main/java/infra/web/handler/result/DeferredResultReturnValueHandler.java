/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.async.DeferredResult;
import infra.web.handler.method.HandlerMethod;

/**
 * ReturnValueHandler for {@link DeferredResult} and {@link Future}
 * and {@link CompletionStage}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/30 22:25
 */
public class DeferredResultReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(DeferredResult.class)
            || handler.isReturnTypeAssignableTo(Future.class)
            || handler.isReturnTypeAssignableTo(CompletionStage.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof DeferredResult
            || returnValue instanceof Future
            || returnValue instanceof CompletionStage;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    DeferredResult<Object> result;
    if (returnValue instanceof DeferredResult) {
      result = (DeferredResult<Object>) returnValue;
    }
    else if (returnValue instanceof Future<?>) {
      result = adaptListenableFuture((Future<Object>) returnValue);
    }
    else if (returnValue instanceof CompletionStage) {
      result = adaptCompletionStage((CompletionStage<?>) returnValue);
    }
    else if (HandlerMethod.isHandler(handler)) {
      result = new DeferredResult<>();
      result.setResult(returnValue);
    }
    else {
      // Should not happen...
      throw new IllegalStateException("Unexpected return value type: " + returnValue);
    }

    context.asyncManager().startDeferredResultProcessing(result, handler);
  }

  private DeferredResult<Object> adaptListenableFuture(Future<Object> future) {
    DeferredResult<Object> result = new DeferredResult<>();
    future.onCompleted(result);
    result.onTimeout(future::cancel);
    return result;
  }

  private DeferredResult<Object> adaptCompletionStage(CompletionStage<?> future) {
    DeferredResult<Object> result = new DeferredResult<>();
    future.handle((value, ex) -> {
      if (ex != null) {
        if (ex instanceof CompletionException && ex.getCause() != null) {
          ex = ex.getCause();
        }
        result.setErrorResult(ex);
      }
      else {
        result.setResult(value);
      }
      return null;
    });
    return result;
  }

}
