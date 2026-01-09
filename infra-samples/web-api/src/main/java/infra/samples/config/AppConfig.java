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

package infra.samples.config;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.MediaType;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.ServerRequest;
import infra.web.handler.function.ServerResponse;

import static infra.web.handler.function.RequestPredicates.GET;
import static infra.web.handler.function.RequestPredicates.param;
import static infra.web.handler.function.RouterFunctions.route;
import static infra.web.handler.function.ServerResponse.badRequest;
import static infra.web.handler.function.ServerResponse.ok;

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
