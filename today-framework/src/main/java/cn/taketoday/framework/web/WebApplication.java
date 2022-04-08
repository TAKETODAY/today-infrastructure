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
package cn.taketoday.framework.web;

import cn.taketoday.framework.Application;
import cn.taketoday.framework.web.context.WebServerApplicationContext;
import cn.taketoday.lang.Experimental;

/**
 * Web Application Runner
 *
 * @author TODAY 2018-10-16 15:46
 */
public class WebApplication extends Application {

  public WebApplication(Class<?>... configSources) {
    super(configSources);
  }

  /**
   * Startup Reactive Web Application
   *
   * @param config config
   * @param args Startup arguments
   */
  @Experimental
  public static WebServerApplicationContext runReactive(Class<?> config, String... args) {
    return (WebServerApplicationContext) new WebApplication(config).run(args);
  }

}
