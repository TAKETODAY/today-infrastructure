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

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationContext;
import infra.test.web.mock.request.RequestPostProcessor;

/**
 * An empty method implementation of {@link MockMvcConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class MockMvcConfigurerAdapter implements MockMvcConfigurer {

  @Override
  public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
  }

  @Override
  @Nullable
  public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, ApplicationContext cxt) {
    return null;
  }

}
