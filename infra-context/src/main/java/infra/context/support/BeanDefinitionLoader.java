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

package infra.context.support;

import infra.beans.factory.Aware;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactoryAware;
import infra.context.BootstrapContext;

/**
 * Strategy interface for loading bean definitions into a {@link BootstrapContext}.
 * <p>
 * Implementations of this interface are responsible for registering bean definitions
 * with the provided {@code BootstrapContext}. During the instantiation process,
 * loader instances may implement any of the following {@link Aware Aware} interfaces
 * to receive specific infrastructure callbacks:
 * <ul>
 * <li>{@link infra.context.EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware}</li>
 * <li>{@link BeanClassLoaderAware}</li>
 * <li>{@link infra.context.ResourceLoaderAware}</li>
 * <li>{@link infra.context.BootstrapContextAware}</li>
 * <li>{@link infra.context.ApplicationContextAware}</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BootstrapContext
 * @since 4.0 2018-06-23 11:18:22
 */
public interface BeanDefinitionLoader {

  /**
   * Load bean definitions into the given {@link BootstrapContext}.
   *
   * @param bootstrapContext the context to load bean definitions into
   */
  void loadBeanDefinitions(BootstrapContext bootstrapContext);

}
