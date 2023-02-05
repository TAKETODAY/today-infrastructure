/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework;

import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.ServletDetector;

/**
 * An enumeration of possible types of application.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/15 14:58
 */
public enum ApplicationType {

  /**
   * The application should not run as a web application and should not start an
   * embedded web server.
   */
  NONE_WEB,

  /**
   * The application should run as a servlet-based web application and should start an
   * embedded servlet web server.
   */
  SERVLET_WEB,

  /**
   * The application should run as a reactive web application and should start an
   * embedded reactive web server.
   */
  REACTIVE_WEB,

  /**
   * The application should run as a netty web application and should start an
   * embedded netty web server.
   */
  NETTY_WEB;

  public static final String SERVLET_INDICATOR_CLASS = ServletDetector.SERVLET_CLASS;
  public static final String REACTOR_INDICATOR_CLASS = "reactor.core.publisher.Flux";
  public static final String NETTY_INDICATOR_CLASS = "io.netty.bootstrap.ServerBootstrap";

  static ApplicationType fromClasspath() {
    if (ServletDetector.isPresent) {
      return ApplicationType.SERVLET_WEB;
    }

    if (ClassUtils.isPresent(NETTY_INDICATOR_CLASS)) {
      if (ClassUtils.isPresent(REACTOR_INDICATOR_CLASS)) {
        return ApplicationType.REACTIVE_WEB;
      }
      return ApplicationType.NETTY_WEB;
    }
    return ApplicationType.NONE_WEB;
  }

}
