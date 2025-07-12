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

package infra.web.handler.method;

import infra.http.HttpEntity;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.bind.resolver.HttpEntityMethodProcessor;
import infra.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * Handler for return values of type {@link infra.http.ResponseEntity}
 * that delegates to one of the following:
 *
 * <ul>
 * <li>{@link HttpEntityMethodProcessor} for responses with a concrete body value
 * <li>{@link ResponseBodyEmitterReturnValueHandler} for responses with a body
 * that is a {@link ResponseBodyEmitter} or an async/reactive type.
 * </ul>
 *
 * <p>Use of this wrapper allows for late check in {@link #handleReturnValue} of
 * the type of the actual body value in case the method signature does not
 * provide enough information to decide via {@link #supportsHandler(Object)}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class ResponseEntityReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final HttpEntityMethodProcessor httpEntityMethodProcessor;

  private final ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler;

  public ResponseEntityReturnValueHandler(HttpEntityMethodProcessor httpEntityMethodProcessor,
          ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler) {

    this.httpEntityMethodProcessor = httpEntityMethodProcessor;
    this.responseBodyEmitterHandler = responseBodyEmitterHandler;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return httpEntityMethodProcessor.supportsHandler(handler);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return httpEntityMethodProcessor.supportsReturnValue(returnValue);
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof HttpEntity<?> httpEntity) {
      Object body = httpEntity.getBody();
      if (body != null) {
        if (this.responseBodyEmitterHandler.supportsBodyType(body.getClass())) {
          this.responseBodyEmitterHandler.handleReturnValue(context, handler, returnValue);
          return;
        }
      }
    }

    this.httpEntityMethodProcessor.handleReturnValue(context, handler, returnValue);
  }

}
