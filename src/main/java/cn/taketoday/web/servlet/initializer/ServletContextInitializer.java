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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet.initializer;

import jakarta.servlet.ServletContext;

import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.servlet.WebServletApplicationContext;

/**
 * @author TODAY <br>
 * 2019-01-15 19:28
 */
@FunctionalInterface
public interface ServletContextInitializer extends WebApplicationInitializer {

  @Override
  default void onStartup(WebApplicationContext applicationContext) throws Throwable {

    if (applicationContext instanceof WebServletApplicationContext) {
      onStartup(((WebServletApplicationContext) applicationContext).getServletContext());
    }
  }

  /**
   * Configure the given {@link ServletContext} with any servlets, filters,
   * listeners context-params and attributes necessary for initialization.
   *
   * @param servletContext the {@code ServletContext} to initialize
   * @throws Throwable if any call {@link Throwable} occurred
   */
  void onStartup(ServletContext servletContext) throws Throwable;
}
