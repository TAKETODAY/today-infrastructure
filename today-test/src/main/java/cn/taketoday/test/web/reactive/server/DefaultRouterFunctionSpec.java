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

package cn.taketoday.test.web.reactive.server;

import cn.taketoday.web.reactive.function.server.HandlerStrategies;
import cn.taketoday.web.reactive.function.server.RouterFunction;
import cn.taketoday.web.reactive.function.server.RouterFunctions;
import cn.taketoday.web.server.WebHandler;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Spec for setting up server-less testing against a RouterFunction.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
class DefaultRouterFunctionSpec extends AbstractMockServerSpec<WebTestClient.RouterFunctionSpec>
        implements WebTestClient.RouterFunctionSpec {

  private final RouterFunction<?> routerFunction;

  private HandlerStrategies handlerStrategies = HandlerStrategies.withDefaults();

  DefaultRouterFunctionSpec(RouterFunction<?> routerFunction) {
    this.routerFunction = routerFunction;
  }

  @Override
  public WebTestClient.RouterFunctionSpec handlerStrategies(HandlerStrategies handlerStrategies) {
    this.handlerStrategies = handlerStrategies;
    return this;
  }

  @Override
  protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
    WebHandler webHandler = RouterFunctions.toWebHandler(this.routerFunction, this.handlerStrategies);
    return WebHttpHandlerBuilder.webHandler(webHandler)
            .filters(filters -> filters.addAll(this.handlerStrategies.webFilters()))
            .exceptionHandlers(handlers -> handlers.addAll(this.handlerStrategies.exceptionHandlers()))
            .localeContextResolver(this.handlerStrategies.localeContextResolver());
  }

}
