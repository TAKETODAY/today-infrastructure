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

package cn.taketoday.framework.web.servlet.context.config;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.servlet.mock.MockServlet;
import cn.taketoday.framework.web.servlet.server.MockServletWebServerFactory;
import jakarta.servlet.Servlet;

/**
 * Example {@code @Configuration} for use with
 * {@code AnnotationConfigServletWebServerApplicationContextTests}.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
public class ExampleServletWebServerApplicationConfiguration {

  @Bean
  public MockServletWebServerFactory webServerFactory() {
    return new MockServletWebServerFactory();
  }

  @Bean
  public Servlet servlet() {
    return new MockServlet();
  }

}
