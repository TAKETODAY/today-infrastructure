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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.lang.Nullable;

/**
 * {@link AutowireCandidateResolver} implementation to use when no annotation
 * support is available. This implementation checks the bean definition only.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/22 21:30
 */
public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver {

  @Override
  public boolean isAutowireCandidate(BeanDefinitionHolder definition, DependencyDescriptor descriptor) {
    return definition.getBeanDefinition().isAutowireCandidate();
  }

  @Override
  @Nullable
  public Object getSuggestedValue(DependencyDescriptor descriptor) {
    return null;
  }

  @Nullable
  @Override
  public String getSuggestedName(DependencyDescriptor descriptor) {
    return null;
  }

  @Override
  public boolean hasQualifier(DependencyDescriptor descriptor) {
    return false;
  }

  @Override
  @Nullable
  public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
    return null;
  }

  @Override
  @Nullable
  public Class<?> getLazyResolutionProxyClass(DependencyDescriptor descriptor, @Nullable String beanName) {
    return null;
  }

  /**
   * This implementation returns {@code this} as-is.
   */
  @Override
  public AutowireCandidateResolver cloneIfNecessary() {
    return this;
  }

}

