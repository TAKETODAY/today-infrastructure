/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation;

import infra.aop.scope.ScopedProxyUtils;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.BeanDefinitionRegistry;

/**
 * Delegate factory class used to just introduce an AOP framework dependency
 * when actually creating a scoped proxy.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScopedProxyUtils#createScopedProxy
 * @since 4.0 2022/3/7 21:30
 */
final class ScopedProxyCreator {

  private ScopedProxyCreator() {

  }

  public static BeanDefinitionHolder createScopedProxy(
          BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry, boolean proxyTargetClass) {

    return ScopedProxyUtils.createScopedProxy(definitionHolder, registry, proxyTargetClass);
  }

  public static String getTargetBeanName(String originalBeanName) {
    return ScopedProxyUtils.getTargetBeanName(originalBeanName);
  }

}
