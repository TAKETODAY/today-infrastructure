/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for determining whether a specific bean definition
 * qualifies as an autowire candidate for a specific dependency.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/22 21:00
 */
public interface AutowireCandidateResolver {

  /**
   * Determine whether the given bean definition qualifies as an
   * autowire candidate for the given dependency.
   * <p>The default implementation checks
   * {@link BeanDefinition#isAutowireCandidate()}.
   *
   * @param definition the bean definition including bean name and aliases
   * @param descriptor the descriptor for the target method parameter or field
   * @return whether the bean definition qualifies as autowire candidate
   * @see BeanDefinition#isAutowireCandidate()
   */
  default boolean isAutowireCandidate(BeanDefinitionHolder definition, DependencyDescriptor descriptor) {
    return definition.getBeanDefinition().isAutowireCandidate();
  }

  /**
   * Determine whether the given descriptor is effectively required.
   * <p>The default implementation checks {@link DependencyDescriptor#isRequired()}.
   *
   * @param descriptor the descriptor for the target method parameter or field
   * @return whether the descriptor is marked as required or possibly indicating
   * non-required status some other way (e.g. through a parameter annotation)
   * @see DependencyDescriptor#isRequired()
   */
  default boolean isRequired(DependencyDescriptor descriptor) {
    return descriptor.isRequired();
  }

  /**
   * Determine whether a default value is suggested for the given dependency.
   * <p>The default implementation simply returns {@code null}.
   *
   * @param descriptor the descriptor for the target method parameter or field
   * @return the value suggested (typically an expression String),
   * or {@code null} if none found
   * @since 4.0
   */
  @Nullable
  default Object getSuggestedValue(DependencyDescriptor descriptor) {
    return null;
  }

  /**
   * Determine whether the given descriptor declares a qualifier beyond the type
   * (typically - but not necessarily - a specific kind of annotation).
   * <p>The default implementation returns {@code false}.
   *
   * @param descriptor the descriptor for the target method parameter or field
   * @return whether the descriptor declares a qualifier, narrowing the candidate
   * status beyond the type match
   * @see QualifierAnnotationAutowireCandidateResolver#hasQualifier
   */
  default boolean hasQualifier(DependencyDescriptor descriptor) {
    return false;
  }

  /**
   * Build a proxy for lazy resolution of the actual dependency target,
   * if demanded by the injection point.
   * <p>The default implementation simply returns {@code null}.
   *
   * @param descriptor the descriptor for the target method parameter or field
   * @param beanName the name of the bean that contains the injection point
   * @return the lazy resolution proxy for the actual dependency target,
   * or {@code null} if straight resolution is to be performed
   */
  @Nullable
  default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
    return null;
  }

  /**
   * Return a clone of this resolver instance if necessary, retaining its local
   * configuration and allowing for the cloned instance to get associated with
   * a new bean factory, or this original instance if there is no such state.
   * <p>The default implementation creates a separate instance via the default
   * class constructor, assuming no specific configuration state to copy.
   * Subclasses may override this with custom configuration state handling
   * or with standard {@link Cloneable} support (as implemented by Framework's
   * own configurable {@code AutowireCandidateResolver} variants), or simply
   * return {@code this} (as in {@link SimpleAutowireCandidateResolver}).
   *
   * @see GenericTypeAwareAutowireCandidateResolver#cloneIfNecessary()
   * @see ConfigurableBeanFactory#copyConfigurationFrom
   */
  default AutowireCandidateResolver cloneIfNecessary() {
    return BeanUtils.newInstance(getClass());
  }

}
