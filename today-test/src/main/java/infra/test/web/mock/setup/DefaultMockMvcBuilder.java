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

package infra.test.web.mock.setup;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.support.WebApplicationContextUtils;

/**
 * A concrete implementation of {@link AbstractMockMvcBuilder} that provides
 * the {@link WebApplicationContext} supplied to it as a constructor argument.
 *
 * <p>In addition, if the {@link MockContext} in the supplied
 * {@code WebApplicationContext} does not contain an entry for the
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
 * key, the root {@code WebApplicationContext} will be detected and stored
 * in the {@code MockContext} under the
 * {@code ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} key.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
public class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {

  private final ApplicationContext context;

  /**
   * Protected constructor. Not intended for direct instantiation.
   *
   * @see MockMvcBuilders#webAppContextSetup(ApplicationContext)
   */
  protected DefaultMockMvcBuilder(ApplicationContext context) {
    Assert.notNull(context, "WebApplicationContext is required");
    this.context = context;
  }

  @Override
  protected ApplicationContext initWebAppContext() {
    if (context instanceof WebApplicationContext applicationContext) {
      MockContext mockContext = applicationContext.getMockContext();
      Assert.state(mockContext != null, "No MockContext");
      ApplicationContext rootWac = WebApplicationContextUtils.getWebApplicationContext(mockContext);

      if (rootWac == null) {
        rootWac = this.context;
        ApplicationContext parent = this.context.getParent();
        while (parent != null) {
          if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
            rootWac = parent;
            break;
          }
          parent = parent.getParent();
        }
        mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
      }
    }
    return this.context;
  }

}
