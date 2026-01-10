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

import infra.lang.Assert;
import infra.web.HandlerAdapter;
import infra.web.HandlerAdapterNotFoundException;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;

/**
 * composite HandlerAdapter
 * <p>
 * default supports HttpRequestHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/10 15:18
 */
public class HandlerAdapters implements HandlerAdapter {
  private final HandlerAdapter[] handlerAdapters;

  public HandlerAdapters(HandlerAdapter[] handlerAdapters) {
    Assert.notNull(handlerAdapters, "HandlerAdapters is required");
    this.handlerAdapters = handlerAdapters;
  }

  @Override
  public boolean supports(Object handler) {
    return selectAdapter(handler) != null;
  }

  @Nullable
  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    if (handler instanceof HttpRequestHandler httpRequestHandler) {
      return httpRequestHandler.handleRequest(context);
    }

    for (HandlerAdapter handlerAdapter : handlerAdapters) {
      if (handlerAdapter.supports(handler)) {
        return handlerAdapter.handle(context, handler);
      }
    }
    throw new HandlerAdapterNotFoundException(handler);
  }

  public @Nullable HandlerAdapter selectAdapter(Object handler) {
    for (HandlerAdapter handlerAdapter : handlerAdapters) {
      if (handlerAdapter.supports(handler)) {
        return handlerAdapter;
      }
    }
    return null;
  }

}
