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

package infra.context.annotation.config;

import java.util.EventListener;

import infra.beans.factory.Aware;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactoryAware;
import infra.context.ApplicationContextAware;
import infra.context.BootstrapContextAware;
import infra.context.EnvironmentAware;
import infra.context.ResourceLoaderAware;

/**
 * Listener that can be registered with {@code today.strategies} to receive details of
 * imported auto-configurations.
 * <p>
 * An {@link AutoConfigurationImportListener} may implement any of the following
 * {@link Aware Aware} interfaces, and their respective
 * methods will be called prior to
 * {@link #onAutoConfigurationImportEvent(AutoConfigurationImportEvent)}:
 * <ul>
 * <li>{@link EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware}</li>
 * <li>{@link BeanClassLoaderAware}</li>
 * <li>{@link ResourceLoaderAware}</li>
 * <li>{@link BootstrapContextAware}</li>
 * <li>{@link ApplicationContextAware}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.context.BootstrapContext
 * @since 4.0 2022/3/5 23:23
 */
@FunctionalInterface
public interface AutoConfigurationImportListener extends EventListener {

  /**
   * Handle an auto-configuration import event.
   *
   * @param event the event to respond to
   */
  void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event);

}
