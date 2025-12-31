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
