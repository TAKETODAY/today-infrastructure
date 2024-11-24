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

import infra.beans.factory.config.AbstractFactoryBean;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Abstract base class for FactoryBeans operating on the
 * JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.util.ServiceLoader
 * @since 4.0
 */
public abstract class AbstractServiceLoaderBasedFactoryBean extends AbstractFactoryBean<Object> {

  @Nullable
  private Class<?> serviceType;

  /**
   * Specify the desired service type (typically the service's public API).
   */
  public void setServiceType(@Nullable Class<?> serviceType) {
    this.serviceType = serviceType;
  }

  /**
   * Return the desired service type.
   */
  @Nullable
  public Class<?> getServiceType() {
    return this.serviceType;
  }

  /**
   * Delegates to {@link #getObjectToExpose(java.util.ServiceLoader)}.
   *
   * @return the object to expose
   */
  @Override
  protected Object createBeanInstance() {
    Assert.notNull(getServiceType(), "Property 'serviceType' is required");
    return getObjectToExpose(ServiceLoader.load(getServiceType(), getBeanClassLoader()));
  }

  /**
   * Determine the actual object to expose for the given ServiceLoader.
   * <p>Left to concrete subclasses.
   *
   * @param serviceLoader the ServiceLoader for the configured service class
   * @return the object to expose
   */
  protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);

}
