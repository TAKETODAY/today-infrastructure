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

package infra.test.context.aot;

import infra.aot.hint.RuntimeHints;
import infra.context.annotation.ImportRuntimeHints;

/**
 * Contract for registering {@link RuntimeHints} for integration tests run with
 * the <em>Infra TestContext Framework</em> based on the {@link ClassLoader}
 * of the deployment unit. Implementations should, if possible, use the specified
 * {@link ClassLoader} to determine if hints have to be contributed.
 *
 * <p>Implementations of this interface must be registered statically in
 * {@code META-INF/config/aot.factories} by using the fully qualified name of this
 * interface as the key. A standard no-arg constructor is required for implementations.
 *
 * <p>This API serves as a companion to the core
 * {@link infra.aot.hint.RuntimeHintsRegistrar RuntimeHintsRegistrar}
 * API. If you need to register global hints for testing support that are not
 * specific to particular test classes, favor implementing {@code RuntimeHintsRegistrar}
 * over this API.
 *
 * <p>As an alternative to implementing and registering a {@code TestRuntimeHintsRegistrar},
 * you may choose to annotate a test class with
 * {@link infra.aot.hint.annotation.Reflective @Reflective},
 * {@link infra.aot.hint.annotation.RegisterReflectionForBinding @RegisterReflectionForBinding},
 * or {@link ImportRuntimeHints @ImportRuntimeHints}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.aot.hint.RuntimeHintsRegistrar
 * @since 4.0
 */
public interface TestRuntimeHintsRegistrar {

  /**
   * Contribute hints to the given {@link RuntimeHints} instance.
   *
   * @param runtimeHints the {@code RuntimeHints} to use
   * @param testClass the test class to process
   * @param classLoader the classloader to use
   */
  void registerHints(RuntimeHints runtimeHints, Class<?> testClass, ClassLoader classLoader);

}
