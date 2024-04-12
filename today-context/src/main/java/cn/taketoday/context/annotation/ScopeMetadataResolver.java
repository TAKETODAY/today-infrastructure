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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.config.BeanDefinition;

/**
 * Strategy interface for resolving the scope of bean definitions.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Scope
 * @since 4.0 2021/10/26 15:56
 */
@FunctionalInterface
public interface ScopeMetadataResolver {

  /**
   * Resolve the {@link ScopeMetadata} appropriate to the supplied
   * bean {@code definition}.
   * <p>Implementations can of course use any strategy they like to
   * determine the scope metadata, but some implementations that
   * immediately to mind might be to use source level annotations
   * present on {@link BeanDefinition#getBeanClassName() the class} of the
   * supplied {@code definition}, or to use metadata present in the
   * {@link BeanDefinition#getAttributeNames()} of the supplied {@code definition}.
   *
   * @param definition the target bean definition
   * @return the relevant scope metadata; never {@code null}
   */
  ScopeMetadata resolveScopeMetadata(BeanDefinition definition);

}
