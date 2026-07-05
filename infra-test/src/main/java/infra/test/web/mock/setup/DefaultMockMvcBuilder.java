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

/**
 * A concrete implementation of {@link AbstractMockMvcBuilder} that provides
 * the {@link ApplicationContext} supplied to it as a constructor argument.
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
  protected ApplicationContext initApplicationContext() {
    return this.context;
  }

}
