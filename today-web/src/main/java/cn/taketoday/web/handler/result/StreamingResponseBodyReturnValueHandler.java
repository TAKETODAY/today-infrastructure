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

import java.io.OutputStream;
import java.util.concurrent.Callable;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.handler.StreamingResponseBody;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.servlet.filter.ShallowEtagHeaderFilter;

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
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    if (returnValue instanceof ResponseEntity<?> responseEntity) {
      context.setStatus(responseEntity.getStatusCode());
      context.mergeToResponse(responseEntity.getHeaders());
      returnValue = responseEntity.getBody();
      if (returnValue == null) {
        return;
      }
    }

    if (returnValue instanceof StreamingResponseBody streamingBody) {
      if (ServletDetector.runningInServlet(context)) {
        ShallowEtagHeaderFilter.disableContentCaching(context);
      }
      var callable = new StreamingResponseBodyTask(context.getOutputStream(), streamingBody);
      context.getAsyncManager()
              .startCallableProcessing(callable, handler);
    }
    else {
      throw new IllegalArgumentException("StreamingResponseBody expected");
    }
  }

  private record StreamingResponseBodyTask(
          OutputStream outputStream, StreamingResponseBody streamingBody) implements Callable<Void> {

    @Override
    public Void call() throws Exception {
      streamingBody.writeTo(outputStream);
      outputStream.flush();
      return null;
    }
  }

}
