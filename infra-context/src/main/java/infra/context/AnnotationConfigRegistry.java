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

package infra.context;

import infra.beans.factory.BeanRegistrar;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;

/**
 * Common interface for annotation config application contexts,
 * defining {@link #register(Class[])} and {@link #scan(String...)} methods.
 *
 * @author TODAY 2021/9/30 23:06
 * @since 4.0
 */
public interface AnnotationConfigRegistry {

  /**
   * Register one or more component classes to be processed.
   * <p>Calls to {@code register} are idempotent; adding the same
   * component class more than once has no additional effect.
   *
   * Load {@link Import} beans from input components classes
   * <p>
   *
   * @param components one or more component classes,
   * e.g. {@link Configuration @Configuration} classes
   * @since 4.0
   */
  void register(Class<?>... components);

  /**
   * Perform a scan within the specified base packages.
   *
   * @param basePackages the packages to scan for component classes
   */
  void scan(String... basePackages);

  /**
   * Invoke the given registrars for registering their beans with this
   * application context.
   * <p>This can be used to register custom beans without inferring
   * annotation-based characteristics for primary/fallback/lazy-init,
   * rather specifying those programmatically if needed.
   *
   * @param registrars one or more {@link BeanRegistrar} instances
   * @see #register(Class[])
   * @since 5.0
   */
  void register(BeanRegistrar... registrars);

}
