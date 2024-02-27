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

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * ReturnValueHandler for {@link DeferredResult} and {@link ListenableFuture}
 * and {@link CompletionStage}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/30 22:25
 */
public class DeferredResultReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(DeferredResult.class)
            || handler.isReturnTypeAssignableTo(ListenableFuture.class)
            || handler.isReturnTypeAssignableTo(CompletionStage.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof DeferredResult
            || returnValue instanceof ListenableFuture
            || returnValue instanceof CompletionStage;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    DeferredResult<?> result;
    if (returnValue instanceof DeferredResult) {
      result = (DeferredResult<?>) returnValue;
    }
    else if (returnValue instanceof ListenableFuture<?>) {
      result = adaptListenableFuture((ListenableFuture<Object>) returnValue);
    }
    else if (returnValue instanceof CompletionStage) {
      result = adaptCompletionStage((CompletionStage<?>) returnValue);
    }
    else {
      // Should not happen...
      throw new IllegalStateException("Unexpected return value type: " + returnValue);
    }

    context.getAsyncManager()
            .startDeferredResultProcessing(result, handler);
  }

  private DeferredResult<Object> adaptListenableFuture(ListenableFuture<Object> future) {
    DeferredResult<Object> result = new DeferredResult<>();
    future.addListener(result);
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
