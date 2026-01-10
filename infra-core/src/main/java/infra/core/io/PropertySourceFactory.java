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

package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.env.CompositePropertySource;
import infra.core.env.PropertySource;

/**
 * Strategy interface for creating resource-based {@link PropertySource} wrappers.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultPropertySourceFactory
 * @since 4.0 2021/10/28 17:34
 */
public interface PropertySourceFactory {

  /**
   * Create a {@link PropertySource} that wraps the given resource.
   * <p>Implementations will typically create {@link ResourcePropertySource}
   * instances, with {@link PropertySourceProcessor} automatically adapting
   * property source names via {@link ResourcePropertySource#withResourceName()}
   * if necessary, e.g. when combining multiple sources for the same name
   * into a {@link CompositePropertySource}.
   * Custom implementations with custom {@link PropertySource} types need
   * to make sure to expose distinct enough names, possibly deriving from
   * {@link ResourcePropertySource} where possible.
   *
   * @param name the name of the property source
   * (can be {@code null} in which case the factory implementation
   * will have to generate a name based on the given resource)
   * @param resource the resource (potentially encoded) to wrap
   * @return the new {@link PropertySource} (never {@code null})
   * @throws IOException if resource resolution failed
   */
  PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
