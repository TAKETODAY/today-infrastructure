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

package cn.taketoday.http.server.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Mono;

/**
 * {@code HttpHandler} delegating requests to one of several {@code HttpHandler}'s
 * based on simple, prefix-based mappings.
 *
 * <p>This is intended as a coarse-grained mechanism for delegating requests to
 * one of several applications -- each represented by an {@code HttpHandler}, with
 * the application "context path" (the prefix-based mapping) exposed via
 * {@link ServerHttpRequest#getPath()}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ContextPathCompositeHandler implements HttpHandler {
  private final LinkedHashMap<String, HttpHandler> handlerMap;

  public ContextPathCompositeHandler(Map<String, ? extends HttpHandler> handlerMap) {
    Assert.notEmpty(handlerMap, "Handler map must not be empty");
    this.handlerMap = initHandlers(handlerMap);
  }

  private static LinkedHashMap<String, HttpHandler> initHandlers(Map<String, ? extends HttpHandler> map) {
    map.keySet().forEach(ContextPathCompositeHandler::assertValidContextPath);
    return new LinkedHashMap<>(map);
  }

  private static void assertValidContextPath(String contextPath) {
    Assert.hasText(contextPath, "Context path must not be empty");
    if (contextPath.equals("/")) {
      return;
    }
    Assert.isTrue(contextPath.startsWith("/"), "Context path must begin with '/'");
    Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with '/'");
  }

  @Override
  public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
    // Remove underlying context path first (e.g. Servlet container)
    String path = request.getPath().pathWithinApplication().value();
    for (Map.Entry<String, HttpHandler> entry : handlerMap.entrySet()) {
      if (path.startsWith(entry.getKey())) {
        String contextPath = request.getPath().contextPath().value() + entry.getKey();
        ServerHttpRequest newRequest = request.mutate().contextPath(contextPath).build();
        return entry.getValue().handle(newRequest, response);
      }
    }

    response.setStatusCode(HttpStatus.NOT_FOUND);
    return response.setComplete();
  }

}
