/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.samples.config;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.MediaType;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.handler.function.RouterFunction;
import cn.taketoday.web.handler.function.ServerRequest;
import cn.taketoday.web.handler.function.ServerResponse;

import static cn.taketoday.web.handler.function.RequestPredicates.GET;
import static cn.taketoday.web.handler.function.RequestPredicates.param;
import static cn.taketoday.web.handler.function.RouterFunctions.route;
import static cn.taketoday.web.handler.function.ServerResponse.badRequest;
import static cn.taketoday.web.handler.function.ServerResponse.ok;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/27 11:00
 */
@Configuration(proxyBeanMethods = false)
public class AppConfig {
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  private ServerResponse params(ServerRequest request) {
    return ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request.params());
  }

  private ServerRequest before(ServerRequest request) {
    log.info("before3: {}", request.uri());
    return request;
  }

  @Component
  public RouterFunction<ServerResponse> functions() {
    return route()
            .GET("/functions/params", this::params)
            .filter((request, next) -> {
              log.info("before: {}", request.uri());
              // before
              ServerResponse response = next.handle(request);
              // after
              log.info("after: {}", request.uri());
              return response;
            })
            .before(request -> {
              log.info("before2: {}", request.uri());
              return request;
            })
            .before(this::before)
            .after((serverRequest, response) -> {
              // log
              return response;
            })
            .build();
  }

  @Bean
  RouterFunction<ServerResponse> composedRoutes() {
    return route(GET("/functions/users"), req -> ok().body(req.uri()))
            .and(route(GET("/functions/users/{id}"), req -> {
              String id = req.pathVariable("id");
              return ok().body(id);
            }).filter((request, next) -> {
              log.info("before: {}", request);
              // before
              ServerResponse response = next.handle(request);
              // after
              log.info("after: {}", request);
              return response;
            }))
            .and(route(GET("/functions/name").and(param("name")), res -> badRequest().build()));
  }
}
