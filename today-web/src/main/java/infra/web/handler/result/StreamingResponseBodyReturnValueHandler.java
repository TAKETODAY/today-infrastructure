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

import java.io.OutputStream;
import java.util.concurrent.Callable;

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.http.ResponseEntity;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.handler.StreamingResponseBody;
import infra.web.handler.method.HandlerMethod;

import static infra.web.handler.result.CallableMethodReturnValueHandler.startCallableProcessing;

/**
 * Supports return values of type
 * {@link StreamingResponseBody} and also {@code ResponseEntity<StreamingResponseBody>}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:07
 */
public class StreamingResponseBodyReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof StreamingResponseBody
            || (
            returnValue instanceof ResponseEntity<?> response
                    && response.getBody() instanceof StreamingResponseBody
    );
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    MethodParameter returnType = handler.getReturnType();
    if (StreamingResponseBody.class.isAssignableFrom(returnType.getParameterType())) {
      return true;
    }
    else if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
      Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric().resolve();
      return bodyType != null && StreamingResponseBody.class.isAssignableFrom(bodyType);
    }
    return false;
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    if (returnValue instanceof ResponseEntity<?> entity) {
      context.setStatus(entity.getStatusCode());
      context.addHeaders(entity.headers());
      returnValue = entity.getBody();
      if (returnValue == null) {
        return;
      }
    }

    if (returnValue instanceof StreamingResponseBody streamingBody) {
      var callable = new StreamingBodyTask(context.getOutputStream(), streamingBody);
      context.asyncManager().startCallableProcessing(callable, handler);
    }
    else if (HandlerMethod.isHandler(handler)) {
      startCallableProcessing(context, handler, returnValue);
    }
    else {
      throw new IllegalArgumentException("StreamingResponseBody expected");
    }
  }

  private record StreamingBodyTask(
          OutputStream outputStream, StreamingResponseBody streamingBody) implements Callable<Void> {

    @Override
    public Void call() throws Exception {
      streamingBody.writeTo(outputStream);
      outputStream.flush();
      return null;
    }
  }

}
