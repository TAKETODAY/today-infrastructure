/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.framework.reactive;

import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.framework.server.WebServerApplicationLoader;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.handler.DispatcherHandler;

/**
 * @author TODAY <br>
 * 2020-03-30 17:13
 */
public class NettyWebServerApplicationLoader extends WebServerApplicationLoader {

  public NettyWebServerApplicationLoader(Supplier<List<WebApplicationInitializer>> initializersSupplier) {
    super(initializersSupplier);
  }

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    setApplicationContext(context);
    super.onStartup(context);
  }

  @Override
  protected DispatcherHandler createDispatcher(WebApplicationContext context) {
    final ReactiveDispatcher dispatcherServlet = new ReactiveDispatcher();
    dispatcherServlet.setApplicationContext(context);
    return dispatcherServlet;
  }
}
