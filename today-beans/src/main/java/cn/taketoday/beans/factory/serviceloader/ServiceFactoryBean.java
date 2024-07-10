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

package cn.taketoday.beans.factory.serviceloader;

import java.util.Iterator;
import java.util.ServiceLoader;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Nullable;

/**
 * {@link FactoryBean} that exposes the
 * 'primary' service for the configured service class, obtained through
 * the JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.util.ServiceLoader
 * @since 4.0
 */
public class ServiceFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

  @Override
  protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
    Iterator<?> it = serviceLoader.iterator();
    if (!it.hasNext()) {
      throw new IllegalStateException(
              "ServiceLoader could not find service for type [%s]".formatted(getServiceType()));
    }
    return it.next();
  }

  @Override
  @Nullable
  public Class<?> getObjectType() {
    return getServiceType();
  }

}
