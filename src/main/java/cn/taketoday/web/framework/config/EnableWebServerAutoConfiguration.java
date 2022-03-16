/*
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

package cn.taketoday.web.framework.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.properties.Props;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.framework.server.AbstractServletWebServer;
import cn.taketoday.web.framework.server.JettyServer;
import cn.taketoday.web.framework.server.TomcatServer;

/**
 * @author TODAY 2021/8/14 12:34
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(WebServerAutoConfiguration.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableWebServerAutoConfiguration {

}

@Configuration(proxyBeanMethods = false)
class WebServerAutoConfiguration {

  @MissingBean
  @Props(prefix = "server") // @since 1.0.3
  static AbstractServletWebServer webServer() {
    if (ClassUtils.isPresent("org.eclipse.jetty.server.Server")) {
      return new JettyServer();
    }
    else if (ClassUtils.isPresent("io.undertow.Undertow")) {
      return new UndertowServer();
    }
    return new TomcatServer();
  }

}
