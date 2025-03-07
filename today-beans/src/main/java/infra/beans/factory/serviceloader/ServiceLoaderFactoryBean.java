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
package infra.beans.factory.serviceloader;

import java.util.ServiceLoader;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} that exposes the JDK 1.6 {@link java.util.ServiceLoader}
 * for the configured service class.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @see java.util.ServiceLoader
 * @since 4.0
 */
public class ServiceLoaderFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

  @Override
  protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
    return serviceLoader;
  }

  @Override
  public Class<?> getObjectType() {
    return ServiceLoader.class;
  }

}
