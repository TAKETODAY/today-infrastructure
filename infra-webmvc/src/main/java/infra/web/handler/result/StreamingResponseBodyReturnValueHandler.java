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

import java.io.OutputStream;
import java.util.concurrent.Callable;

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.http.ResponseEntity;
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
      context.addHeaders(entity.getHeaders());
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
