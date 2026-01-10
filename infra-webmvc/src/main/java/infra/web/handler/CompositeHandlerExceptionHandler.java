/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.core.OrderedSupport;
import infra.lang.Assert;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;

/**
 * @author TODAY 2020/12/23 21:53
 */
public class CompositeHandlerExceptionHandler extends OrderedSupport implements HandlerExceptionHandler {

  private final List<HandlerExceptionHandler> handlers;

  public CompositeHandlerExceptionHandler(final List<HandlerExceptionHandler> handlers) {
    Assert.notNull(handlers, "handlers is required");
    this.handlers = handlers;
  }

  /**
   * Return the list of exception resolvers to delegate to.
   */
  @Nullable
  public List<HandlerExceptionHandler> getExceptionHandlers() {
    return this.handlers;
  }

  @Nullable
  @Override
  public Object handleException(final RequestContext context, final Throwable exception, @Nullable final Object handler) throws Exception {
    var handlers = getExceptionHandlers();
    if (handlers != null) {
      for (var exceptionHandler : handlers) {
        Object view = exceptionHandler.handleException(context, exception, handler);
        if (view != null) {
          return view;
        }
      }
    }
    return null;
  }

}
