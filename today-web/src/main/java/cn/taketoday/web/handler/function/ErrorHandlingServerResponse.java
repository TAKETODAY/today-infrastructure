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

package cn.taketoday.web.handler.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Base class for {@link ServerResponse} implementations with error handling.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class ErrorHandlingServerResponse implements ServerResponse {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final ArrayList<ErrorHandler<?>> errorHandlers = new ArrayList<>();

  protected final <T extends ServerResponse> void addErrorHandler(Predicate<Throwable> predicate,
          BiFunction<Throwable, ServerRequest, T> errorHandler) {

    Assert.notNull(predicate, "Predicate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");
    this.errorHandlers.add(new ErrorHandler<>(predicate, errorHandler));
  }

  @Nullable
  protected final Object handleError(Throwable t, RequestContext request, Context context) throws Exception {
    ServerResponse serverResponse = errorResponse(t, request);
    if (serverResponse != null) {
      return serverResponse.writeTo(request, context);
    }
    else if (t instanceof IOException) {
      throw (IOException) t;
    }
    else {
      throw new Exception(t);
    }
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
