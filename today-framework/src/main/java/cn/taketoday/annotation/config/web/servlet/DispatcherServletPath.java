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

package cn.taketoday.annotation.config.web.servlet;

import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.web.servlet.DispatcherServlet;

/**
 * Interface that can be used by auto-configurations that need path details for the
 * {@link DispatcherServletAutoConfiguration#DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME
 * default} {@link DispatcherServlet}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/2 21:50
 */
@FunctionalInterface
public interface DispatcherServletPath {

  /**
   * Returns the configured path of the dispatcher servlet.
   *
   * @return the configured path
   */
  String getPath();

  /**
   * Return a form of the given path that's relative to the dispatcher servlet path.
   *
   * @param path the path to make relative
   * @return the relative path
   */
  default String getRelativePath(String path) {
    String prefix = getPrefix();
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return prefix + path;
  }

  /**
   * Return a cleaned up version of the path that can be used as a prefix for URLs. The
   * resulting path will have path will not have a trailing slash.
   *
   * @return the prefix
   * @see #getRelativePath(String)
   */
  default String getPrefix() {
    String result = getPath();
    int index = result.indexOf('*');
    if (index != -1) {
      result = result.substring(0, index);
    }
    if (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  /**
   * Return a URL mapping pattern that can be used with a
   * {@link ServletRegistrationBean} to map the dispatcher servlet.
   *
   * @return the path as a servlet URL mapping
   */
  default String getServletUrlMapping() {
    if (getPath().equals("") || getPath().equals("/")) {
      return "/";
    }
    if (getPath().contains("*")) {
      return getPath();
    }
    if (getPath().endsWith("/")) {
      return getPath() + "*";
    }
    return getPath() + "/*";
  }

}
