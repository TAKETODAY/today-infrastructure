/*
 * Copyright 2017 - 2025 the original author or authors.
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
