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

package infra.context.support;

import infra.beans.factory.Aware;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactoryAware;
import infra.context.BootstrapContext;

/**
 * load bean definitions
 * <p>
 * instantiate the class may implement any of the following
 * {@link Aware Aware} interfaces
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

  void loadBeanDefinitions(BootstrapContext loadingContext);

}
