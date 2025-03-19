/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.test.web.client;

import infra.core.env.Environment;
import infra.lang.Assert;
import infra.web.client.config.RootUriTemplateHandler;
import infra.web.util.DefaultUriBuilderFactory;
import infra.web.util.UriTemplateHandler;

/**
 * {@link UriTemplateHandler} will automatically prefix relative URIs with
 * <code>localhost:$&#123;local.server.port&#125;</code>.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LocalHostUriTemplateHandler extends RootUriTemplateHandler {

  private static final String PREFIX = "server.mock.";

  private final Environment environment;

  private final String scheme;

  /**
   * Create a new {@code LocalHostUriTemplateHandler} that will generate {@code http}
   * URIs using the given {@code environment} to determine the context path and port.
   *
   * @param environment the environment used to determine the port
   */
  public LocalHostUriTemplateHandler(Environment environment) {
    this(environment, "http");
  }

  /**
   * Create a new {@code LocalHostUriTemplateHandler} that will generate URIs with the
   * given {@code scheme} and use the given {@code environment} to determine the
   * context-path and port.
   *
   * @param environment the environment used to determine the port
   * @param scheme the scheme of the root uri
   */
  public LocalHostUriTemplateHandler(Environment environment, String scheme) {
    this(environment, scheme, new DefaultUriBuilderFactory());
  }

  /**
   * Create a new {@code LocalHostUriTemplateHandler} that will generate URIs with the
   * given {@code scheme}, use the given {@code environment} to determine the
   * context-path and port and delegate to the given template {@code handler}.
   *
   * @param environment the environment used to determine the port
   * @param scheme the scheme of the root uri
   * @param handler the delegate handler
   */
  public LocalHostUriTemplateHandler(Environment environment, String scheme, UriTemplateHandler handler) {
    super(handler);
    Assert.notNull(environment, "Environment is required");
    Assert.notNull(scheme, "Scheme is required");
    this.environment = environment;
    this.scheme = scheme;
  }

  @Override
  public String getRootUri() {
    String port = this.environment.getProperty("local.server.port", "8080");
    String contextPath = this.environment.getProperty(PREFIX + "context-path", "");
    return this.scheme + "://localhost:" + port + contextPath;
  }

}
