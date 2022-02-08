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

package cn.taketoday.web;

import cn.taketoday.util.ClassUtils;

/**
 * A common delegate for detecting Servlet's presence AND its features
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 11:39
 */
public abstract class ServletDetector {
  public static final String SERVLET_CLASS = "jakarta.servlet.Servlet";

  private static final boolean servletPresent;

  static {
    ClassLoader classLoader = ServletDetector.class.getClassLoader();
    servletPresent = ClassUtils.isPresent(SERVLET_CLASS, classLoader);
  }

  public static boolean isPresent() {
    return servletPresent;
  }

}
