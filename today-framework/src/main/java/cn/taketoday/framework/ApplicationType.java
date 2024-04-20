/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

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
  NORMAL,

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

  public static final String WEB_INDICATOR_CLASS = "cn.taketoday.web.RequestContext";

  public static final String REACTOR_INDICATOR_CLASS = ReactiveStreams.REACTOR_INDICATOR_CLASS;

  public static final String NETTY_INDICATOR_CLASS = "io.netty.bootstrap.ServerBootstrap";

  public static ApplicationType forClasspath() {
    ClassLoader classLoader = ApplicationType.class.getClassLoader();
    if (ClassUtils.isPresent(WEB_INDICATOR_CLASS, classLoader) && ClassUtils.isPresent(NETTY_INDICATOR_CLASS, classLoader)) {
      if (ClassUtils.isPresent(REACTOR_INDICATOR_CLASS, classLoader)) {
        return ApplicationType.REACTIVE_WEB;
      }
      return ApplicationType.NETTY_WEB;
    }
    return ApplicationType.NORMAL;
  }

  static class Hints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      registerTypeIfPresent(WEB_INDICATOR_CLASS, classLoader, hints);
      registerTypeIfPresent(NETTY_INDICATOR_CLASS, classLoader, hints);
      registerTypeIfPresent(REACTOR_INDICATOR_CLASS, classLoader, hints);
    }

    private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
      if (ClassUtils.isPresent(typeName, classLoader)) {
        hints.reflection().registerType(TypeReference.of(typeName));
      }
    }

  }

}
