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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.filter.TypeFilter;

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

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private Collection<TypeExcludeFilter> delegates;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public boolean match(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
    BeanFactory beanFactory = this.beanFactory;
    if (beanFactory != null && getClass() == TypeExcludeFilter.class) {
      for (TypeExcludeFilter delegate : getDelegates(beanFactory)) {
        if (delegate.match(metadataReader, factory)) {
          return true;
        }
      }
    }
    return false;
  }

  private Collection<TypeExcludeFilter> getDelegates(BeanFactory beanFactory) {
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

