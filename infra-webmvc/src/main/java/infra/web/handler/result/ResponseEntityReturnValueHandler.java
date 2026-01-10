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

import infra.http.HttpEntity;
import infra.web.RequestContext;
import infra.web.ReturnValueHandler;
import infra.web.bind.resolver.HttpEntityMethodProcessor;

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
public class ResponseEntityReturnValueHandler implements ReturnValueHandler {

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
