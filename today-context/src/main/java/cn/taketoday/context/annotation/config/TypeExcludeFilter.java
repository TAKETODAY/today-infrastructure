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

package cn.taketoday.context.annotation.config;

import java.io.IOException;
import java.util.Collection;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.TypeFilter;

/**
 * Provides exclusion {@link TypeFilter TypeFilters} that are loaded from the
 * {@link BeanFactory} and automatically applied to {@code InfraApplication}
 * scanning. Can also be used directly with {@code @ComponentScan} as follows:
 * <pre class="code">
 * &#064;ComponentScan(excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
 * </pre>
 * <p>
 * Implementations should provide a subclass registered with {@link BeanFactory} and
 * override the {@link #match(MetadataReader, MetadataReaderFactory)} method. They should
 * also implement a valid {@link #hashCode() hashCode} and {@link #equals(Object) equals}
 * methods so that they can be used as part of test's application context caches.
 * <p>
 * Note that {@code TypeExcludeFilters} are initialized very early in the application
 * lifecycle, they should generally not have dependencies on any other beans. They are
 * primarily used internally to support {@code today-test}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 23:45
 */
public class TypeExcludeFilter implements TypeFilter, BeanFactoryAware {

  private BeanFactory beanFactory;

  private Collection<TypeExcludeFilter> delegates;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public boolean match(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
    if (this.beanFactory != null && getClass() == TypeExcludeFilter.class) {
      for (TypeExcludeFilter delegate : getDelegates()) {
        if (delegate.match(metadataReader, factory)) {
          return true;
        }
      }
    }
    return false;
  }

  private Collection<TypeExcludeFilter> getDelegates() {
    Collection<TypeExcludeFilter> delegates = this.delegates;
    if (delegates == null) {
      delegates = beanFactory.getBeansOfType(TypeExcludeFilter.class).values();
      this.delegates = delegates;
    }
    return delegates;
  }

  @Override
  public boolean equals(Object obj) {
    throw new IllegalStateException("TypeExcludeFilter %s has not implemented equals".formatted(getClass()));
  }

  @Override
  public int hashCode() {
    throw new IllegalStateException("TypeExcludeFilter %s has not implemented hashCode".formatted(getClass()));
  }

}

