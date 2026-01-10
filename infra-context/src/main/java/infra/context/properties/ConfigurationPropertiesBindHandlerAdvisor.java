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

package infra.context.properties;

import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindHandler;

/**
 * Allows additional functionality to be applied to the {@link BindHandler} used by the
 * {@link ConfigurationPropertiesBindingPostProcessor}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractBindHandler
 * @since 4.0
 */
@FunctionalInterface
public interface ConfigurationPropertiesBindHandlerAdvisor {

  /**
   * Apply additional functionality to the source bind handler.
   *
   * @param bindHandler the source bind handler
   * @return a replacement bind handler that delegates to the source and provides
   * additional functionality
   */
  BindHandler apply(BindHandler bindHandler);

}
