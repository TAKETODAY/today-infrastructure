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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import infra.context.ConfigurableApplicationContext;
import infra.mock.api.MockContext;

/**
 * Interface to be implemented by configurable web application contexts.
 *
 * <p>Note: The setters of this interface need to be called before an
 * invocation of the {@link #refresh} method inherited from
 * {@link infra.context.ConfigurableApplicationContext}.
 * They do not cause an initialization of the context on their own.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 17:52
 */
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

  /**
   * Set the MockContext for this web application context.
   * <p>Does not cause an initialization of the context: refresh needs to be
   * called after the setting of all configuration properties.
   *
   * @see #refresh()
   */
  void setMockContext(@Nullable MockContext mockContext);

}
