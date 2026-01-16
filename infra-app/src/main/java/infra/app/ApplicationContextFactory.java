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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.beans.BeanUtils;
import infra.context.ConfigurableApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;

/**
 * Strategy interface for creating the {@link ConfigurableApplicationContext} used by a
 * {@link Application}. Created contexts should be returned in their default form,
 * with the {@code Application} responsible for configuring and refreshing the
 * context.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/15 14:56
 */
public interface ApplicationContextFactory {

  /**
   * A default {@link ApplicationContextFactory} implementation that will create an
   * appropriate context for the {@link ApplicationType}.
   */
  ApplicationContextFactory DEFAULT = new DefaultApplicationContextFactory();

  /**
   * Creates the {@link ConfigurableApplicationContext application context} for a
   * {@link Application}, respecting the given {@code ApplicationType}.
   *
   * @param type the application type
   * @return the newly created application context
   */
  @Nullable
  ConfigurableApplicationContext create(ApplicationType type);

  /**
   * Return the {@link Environment} type expected to be set on the
   * {@link #create(ApplicationType) created} application context. The result of this
   * method can be used to convert an existing environment instance to the correct type.
   *
   * @param applicationType the web application type or {@code null}
   * @return the expected application context type or {@code null} to use the default
   * @since 5.0
   */
  default @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(@Nullable ApplicationType applicationType) {
    return null;
  }

  /**
   * Create a new {@link Environment} to be set on the
   * {@link #create(ApplicationType) created} application context. The result of this
   * method must match the type returned by
   * {@link #getEnvironmentType(ApplicationType)}.
   *
   * @param applicationType the web application type or {@code null}
   * @return an environment instance or {@code null} to use the default
   * @since 5.0
   */
  default @Nullable ConfigurableEnvironment createEnvironment(@Nullable ApplicationType applicationType) {
    return null;
  }

  /**
   * Creates an {@code ApplicationContextFactory} that will create contexts by
   * instantiating the given {@code contextClass} via its primary constructor.
   *
   * @param contextClass the context class
   * @return the factory that will instantiate the context class
   * @see BeanUtils#newInstance(Class)
   */
  static ApplicationContextFactory forClass(Class<? extends ConfigurableApplicationContext> contextClass) {
    return forSupplier(() -> BeanUtils.newInstance(contextClass));
  }

  /**
   * Creates an {@code ApplicationContextFactory} that will create contexts by calling
   * the given {@link Supplier}.
   *
   * @param supplier the context supplier, for example
   * {@code AnnotationConfigApplicationContext::new}
   * @return the factory that will instantiate the context class
   */
  static ApplicationContextFactory forSupplier(Supplier<ConfigurableApplicationContext> supplier) {
    return applicationType -> supplier.get();
  }

}
