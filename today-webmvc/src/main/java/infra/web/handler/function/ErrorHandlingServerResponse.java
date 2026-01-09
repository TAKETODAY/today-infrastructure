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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Base class for {@link ServerResponse} implementations with error handling.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class ErrorHandlingServerResponse implements ServerResponse {

  private final ArrayList<ErrorHandler<?>> errorHandlers = new ArrayList<>();

  protected final <T extends ServerResponse> void addErrorHandler(Predicate<Throwable> predicate,
          BiFunction<Throwable, ServerRequest, T> errorHandler) {

    Assert.notNull(predicate, "Predicate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");
    this.errorHandlers.add(new ErrorHandler<>(predicate, errorHandler));
  }

  @Nullable
  protected final Object handleError(Throwable t, RequestContext request, Context context) throws Throwable {
    ServerResponse serverResponse = errorResponse(t, request);
    if (serverResponse != null) {
      return serverResponse.writeTo(request, context);
    }
    throw t;
  }

  @Nullable
  protected final ServerResponse errorResponse(Throwable t, RequestContext request) {
    for (ErrorHandler<?> errorHandler : errorHandlers) {
      if (errorHandler.test(t)) {
        ServerRequest serverRequest = ServerRequest.findRequired(request);
        return errorHandler.handle(t, serverRequest);
      }
    }
    return null;
  }

  private static class ErrorHandler<T extends ServerResponse> {

    private final Predicate<Throwable> predicate;

    private final BiFunction<Throwable, ServerRequest, T> responseProvider;

    public ErrorHandler(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> responseProvider) {
      Assert.notNull(predicate, "Predicate is required");
      Assert.notNull(responseProvider, "ResponseProvider is required");
      this.predicate = predicate;
      this.responseProvider = responseProvider;
    }

    public boolean test(Throwable t) {
      return this.predicate.test(t);
    }

    public T handle(Throwable t, ServerRequest serverRequest) {
      return this.responseProvider.apply(t, serverRequest);
    }
  }

}
