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

import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.http.HttpMethod;

/**
 * Variation of Jetty's {@link ErrorPageErrorHandler} that supports all {@link HttpMethod
 * HttpMethods} rather than just {@code GET}, {@code POST} and {@code HEAD}. By default
 * Jetty <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=446039">intentionally only
 * supports a limited set of HTTP methods</a> for error pages, however,
 * prefers Tomcat, Jetty and Undertow to all behave in the same way.
 *
 * @author Phillip Webb
 * @author Christoph Dreis
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JettyEmbeddedErrorHandler extends ErrorPageErrorHandler {

  @Override
  public boolean errorPageForMethod(String method) {
    return true;
  }

}
