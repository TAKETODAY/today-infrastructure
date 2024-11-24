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
