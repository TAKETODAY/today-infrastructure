/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.error;

import infra.beans.factory.annotation.Value;

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
