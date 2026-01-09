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

package infra.test.context.web.socket;

import org.jspecify.annotations.Nullable;

import infra.context.ConfigurableApplicationContext;
import infra.mock.api.MockContext;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;
import infra.web.mock.WebApplicationContext;

/**
 * {@link ContextCustomizer} that instantiates a new {@link MockServerContainer}
 * and stores it in the {@code MockContext} under the attribute named
 * {@code "jakarta.websocket.server.ServerContainer"}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class MockServerContainerContextCustomizer implements ContextCustomizer {

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
    if (context instanceof WebApplicationContext wac) {
      MockContext sc = wac.getMockContext();
      if (sc != null) {
        sc.setAttribute("jakarta.websocket.server.ServerContainer", new MockServerContainer());
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other != null && getClass() == other.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
