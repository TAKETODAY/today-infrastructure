/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;

/**
 * Jetty {@link WebAppContext} used by {@link JettyWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JettyEmbeddedWebAppContext extends WebAppContext {

  @Override
  protected ServletHandler newServletHandler() {
    return new JettyEmbeddedServletHandler();
  }

  void deferredInitialize() throws Exception {
    ((JettyEmbeddedServletHandler) getServletHandler()).deferredInitialize();
  }

  private static class JettyEmbeddedServletHandler extends ServletHandler {

    @Override
    public void initialize() throws Exception { }

    void deferredInitialize() throws Exception {
      super.initialize();
    }

  }

}
