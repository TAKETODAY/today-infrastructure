/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.result;

import java.io.OutputStream;
import java.util.concurrent.Callable;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.server.ServerHttpResponse;
import cn.taketoday.http.server.ServletServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.StreamingResponseBody;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.filter.ShallowEtagHeaderFilter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Supports return values of type
 * {@link StreamingResponseBody} and also {@code ResponseEntity<StreamingResponseBody>}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:07
 */
public class StreamingResponseBodyReturnValueHandler extends HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof StreamingResponseBody
            || (returnValue instanceof ResponseEntity && ((ResponseEntity<?>) returnValue).getBody() instanceof StreamingResponseBody);
  }

  @Override
  protected boolean supportsHandlerMethod(HandlerMethod handler) {
    MethodParameter returnType = handler.getReturnType();
    if (StreamingResponseBody.class.isAssignableFrom(returnType.getParameterType())) {
      return true;
    }
    else if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
      Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric().resolve();
      return (bodyType != null && StreamingResponseBody.class.isAssignableFrom(bodyType));
    }
    return false;
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      mavContainer.setRequestHandled(true);
      return;
    }

    HttpServletResponse response = ServletUtils.getServletResponse(context);
    Assert.state(response != null, "No HttpServletResponse");
    ServerHttpResponse outputMessage = new ServletServerHttpResponse(response);

    if (returnValue instanceof ResponseEntity<?> responseEntity) {
      response.setStatus(responseEntity.getStatusCode().value());
      outputMessage.getHeaders().putAll(responseEntity.getHeaders());
      returnValue = responseEntity.getBody();
      if (returnValue == null) {
        mavContainer.setRequestHandled(true);
        outputMessage.flush();
        return;
      }
    }

    ServletRequest request = ServletUtils.getServletRequest(context);
    Assert.state(request != null, "No ServletRequest");
    ShallowEtagHeaderFilter.disableContentCaching(request);

    Assert.isInstanceOf(StreamingResponseBody.class, returnValue, "StreamingResponseBody expected");
    StreamingResponseBody streamingBody = (StreamingResponseBody) returnValue;

    Callable<Void> callable = new StreamingResponseBodyTask(outputMessage.getBody(), streamingBody);

    WebAsyncUtils.getAsyncManager(context)
            .startCallableProcessing(callable, mavContainer);
  }

  private record StreamingResponseBodyTask(
          OutputStream outputStream, StreamingResponseBody streamingBody) implements Callable<Void> {

    @Override
    public Void call() throws Exception {
      this.streamingBody.writeTo(this.outputStream);
      this.outputStream.flush();
      return null;
    }
  }

}
