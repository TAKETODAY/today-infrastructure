/*
 * Copyright 2002-present the original author or authors.
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
