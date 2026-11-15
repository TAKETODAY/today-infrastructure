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

import infra.util.Assert;
import infra.web.HttpContext;
import infra.web.ReturnValueHandler;
import infra.web.handler.function.ServerRequest;
import infra.web.handler.function.ServerResponse;

/**
 * {@link ReturnValueHandler} for functional web {@link ServerResponse} values.
 *
 * <p>Writes the response through {@link ServerResponse#writeTo} and then
 * continues with the value returned by it, mirroring the way other return
 * values are further processed by the {@code ReturnValueHandler} chain. This
 * makes {@code ServerResponse} a first-class return value, including for
 * asynchronous concurrent results and exception handlers.
 *
 * <p>Streaming responses, such as
 * {@link infra.web.handler.function.ServerResponse#sse}, write their status and
 * headers first and return an emitter. The emitter is then delegated to
 * {@link ResponseBodyEmitterReturnValueHandler} for initialization, timeout and
 * completion handling.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class ServerResponseReturnValueHandler implements SmartReturnValueHandler {

  private final ReturnValueHandler delegate;

  /**
   * Create a new handler.
   *
   * @param delegate the handler to continue with the value produced by
   * {@link ServerResponse#writeTo}; usually the full return-value handler list
   */
  public ServerResponseReturnValueHandler(ReturnValueHandler delegate) {
    Assert.notNull(delegate, "ReturnValueHandler delegate is required");
    this.delegate = delegate;
  }

  @Override
  public boolean supportsHandler(Object handler, @Nullable Object returnValue) {
    return supportsReturnValue(returnValue);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof ServerResponse;
  }

  @Override
  public void handleReturnValue(HttpContext request, @Nullable Object handler,
          @Nullable Object returnValue) throws Exception {

    ServerResponse response = (ServerResponse) returnValue;
    ServerRequest serverRequest = ServerRequest.findRequired(request);

    Object result = response.writeTo(request, serverRequest);
    if (result == NONE_RETURN_VALUE) {
      return;
    }

    delegate.handleReturnValue(request, handler, result);
  }

}
