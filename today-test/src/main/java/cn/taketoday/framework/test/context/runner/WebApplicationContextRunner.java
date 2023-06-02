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

package cn.taketoday.framework.test.context.runner;

import java.util.function.Supplier;

import cn.taketoday.framework.test.context.assertj.AssertableWebApplicationContext;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.web.servlet.ConfigurableWebApplicationContext;
import cn.taketoday.web.servlet.WebApplicationContext;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a Servlet
 * based {@link ConfigurableWebApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public final class WebApplicationContextRunner extends
        AbstractApplicationContextRunner<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

  /**
   * Create a new {@link WebApplicationContextRunner} instance using an
   * {@link AnnotationConfigServletWebApplicationContext} with a
   * {@link MockServletContext} as the underlying source.
   *
   * @see #withMockServletContext(Supplier)
   */
  public WebApplicationContextRunner() {
    this(withMockServletContext(AnnotationConfigServletWebApplicationContext::new));
  }

  /**
   * Create a new {@link WebApplicationContextRunner} instance using the specified
   * {@code contextFactory} as the underlying source.
   *
   * @param contextFactory a supplier that returns a new instance on each call
   */
  public WebApplicationContextRunner(Supplier<ConfigurableWebApplicationContext> contextFactory) {
    super(contextFactory, WebApplicationContextRunner::new);
  }

  private WebApplicationContextRunner(RunnerConfiguration<ConfigurableWebApplicationContext> configuration) {
    super(configuration, WebApplicationContextRunner::new);
  }

  /**
   * Decorate the specified {@code contextFactory} to set a {@link MockServletContext}
   * on each newly created {@link WebApplicationContext}.
   *
   * @param contextFactory the context factory to decorate
   * @return an updated supplier that will set the {@link MockServletContext}
   */
  public static Supplier<ConfigurableWebApplicationContext> withMockServletContext(
          Supplier<ConfigurableWebApplicationContext> contextFactory) {
    return (contextFactory != null) ? () -> {
      ConfigurableWebApplicationContext context = contextFactory.get();
      context.setServletContext(new MockServletContext());
      return context;
    } : null;
  }

}
