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

package cn.taketoday.app.loader.net.protocol;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Utility used to register loader {@link URLStreamHandler URL handlers}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class Handlers {

  private static final String PROTOCOL_HANDLER_PACKAGES = "java.protocol.handler.pkgs";

  private static final String PACKAGE = Handlers.class.getPackageName();

  private Handlers() {
  }

  /**
   * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
   * {@link URLStreamHandler} will be located to deal with jar URLs.
   */
  public static void register() {
    String packages = System.getProperty(PROTOCOL_HANDLER_PACKAGES, "");
    packages = (!packages.isEmpty() && !packages.contains(PACKAGE)) ? packages + "|" + PACKAGE : PACKAGE;
    System.setProperty(PROTOCOL_HANDLER_PACKAGES, packages);
    resetCachedUrlHandlers();
  }

  /**
   * Reset any cached handlers just in case a jar protocol has already been used. We
   * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
   * should have no effect other than clearing the handlers cache.
   */
  private static void resetCachedUrlHandlers() {
    try {
      URL.setURLStreamHandlerFactory(null);
    }
    catch (Error ex) {
      // Ignore
    }
  }

}
