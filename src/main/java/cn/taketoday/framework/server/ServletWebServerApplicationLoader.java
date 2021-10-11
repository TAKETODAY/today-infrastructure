/**
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework.server;

import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.servlet.WebServletApplicationLoader;

/**
 * @author TODAY <br>
 * 2019-10-13 23:29
 */
public class ServletWebServerApplicationLoader extends WebServletApplicationLoader {

  private Supplier<List<WebApplicationInitializer>> initializersSupplier;

  public ServletWebServerApplicationLoader(ApplicationContext context, Supplier<List<WebApplicationInitializer>> initializersSupplier) {
    this.initializersSupplier = initializersSupplier;
    setApplicationContext(context);
  }

  @Override
  protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {
    initializers.addAll(initializersSupplier.get());
    super.configureInitializer(initializers, config);
    initializersSupplier = null;
  }

}
