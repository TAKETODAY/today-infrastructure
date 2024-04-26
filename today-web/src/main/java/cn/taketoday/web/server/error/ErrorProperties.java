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

package cn.taketoday.web.server.error;

import cn.taketoday.beans.factory.annotation.Value;

/**
 * Configuration properties for web error handling.
 *
 * @author Michael Stummvoll
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ErrorProperties {

  /**
   * Path of the error controller.
   */
  @Value("${error.path:/error}")
  public String path = "/error";

  /**
   * Include the "exception" attribute.
   */
  public boolean includeException;

  /**
   * When to include the "trace" attribute.
   */
  public IncludeAttribute includeStacktrace = IncludeAttribute.NEVER;

  /**
   * When to include "message" attribute.
   */
  public IncludeAttribute includeMessage = IncludeAttribute.NEVER;

  /**
   * When to include "errors" attribute.
   */
  public IncludeAttribute includeBindingErrors = IncludeAttribute.NEVER;

  /**
   * When to include "path" attribute.
   */
  public IncludeAttribute includePath = IncludeAttribute.ALWAYS;

  public final Whitelabel whitelabel = new Whitelabel();

  public static class Whitelabel {

    /**
     * Whether to enable the default error page displayed in browsers in case of a
     * server error.
     */
    public boolean enabled = true;

  }

}
