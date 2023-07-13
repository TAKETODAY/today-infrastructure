/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.support.AbstractContextLoader;

/**
 * Strategy interface for customizing {@link ConfigurableApplicationContext
 * application contexts} that are created and managed by the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Customizers are created by {@link ContextCustomizerFactory} implementations.
 *
 * <p><strong>WARNING</strong>: implementations must implement correct
 * {@link Object#equals(Object) equals} and {@link Object#hashCode() hashCode}
 * methods since customizers form part of the {@link MergedContextConfiguration}
 * which is used as a cache key.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextCustomizerFactory
 * @see AbstractContextLoader#customizeContext
 * @since 4.0
 */
@FunctionalInterface
public interface ContextCustomizer {

  /**
   * Customize the supplied {@code ConfigurableApplicationContext} <em>after</em>
   * bean definitions have been loaded into the context but <em>before</em> the
   * context has been refreshed.
   *
   * @param context the context to customize
   * @param mergedConfig the merged context configuration
   */
  void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig);

}
