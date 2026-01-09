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

package infra.beans.factory.serviceloader;

import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.ServiceLoader;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.FactoryBean;

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
