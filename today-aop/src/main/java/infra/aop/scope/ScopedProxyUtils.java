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

package infra.aop.scope;

import infra.aop.framework.autoproxy.AutoProxyUtils;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.lang.Nullable;

/**
 * Utility class for creating a scoped proxy.
 *
 * <p>Used by ScopedProxyBeanDefinitionDecorator and ClassPathBeanDefinitionScanner.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:16
 */
public abstract class ScopedProxyUtils {

  private static final String TARGET_NAME_PREFIX = "scopedTarget.";

  private static final int TARGET_NAME_PREFIX_LENGTH = TARGET_NAME_PREFIX.length();

  /**
   * Generate a scoped proxy for the supplied target bean, registering the target
   * bean with an internal name and setting 'targetBeanName' on the scoped proxy.
   *
   * @param definition the original bean definition
   * @param registry the bean definition registry
   * @param proxyTargetClass whether to create a target class proxy
   * @return the scoped proxy definition
   * @see #getTargetBeanName(String)
   * @see #getOriginalBeanName(String)
   */
  public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,
          BeanDefinitionRegistry registry, boolean proxyTargetClass) {

    String originalBeanName = definition.getBeanName();
    BeanDefinition targetDefinition = definition.getBeanDefinition();
    String targetBeanName = getTargetBeanName(originalBeanName);

    // Create a scoped proxy definition for the original bean name,
    // "hiding" the target bean in an internal target definition.
    RootBeanDefinition proxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);
    proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));
    proxyDefinition.setOriginatingBeanDefinition(targetDefinition);
    proxyDefinition.setSource(definition.getSource());
    proxyDefinition.setRole(targetDefinition.getRole());

    proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);
    if (proxyTargetClass) {
      targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
      // ScopedProxyFactoryBean's "proxyTargetClass" default is TRUE, so we don't need to set it explicitly here.
    }
    else {
      proxyDefinition.getPropertyValues().add("proxyTargetClass", Boolean.FALSE);
    }

    // Copy autowire settings from original bean definition.
    proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
    proxyDefinition.setPrimary(targetDefinition.isPrimary());
    if (targetDefinition instanceof AbstractBeanDefinition) {
      proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
    }

    // The target bean should be ignored in favor of the scoped proxy.
    targetDefinition.setAutowireCandidate(false);
    targetDefinition.setPrimary(false);

    // Register the target bean as separate bean in the factory.
    registry.registerBeanDefinition(targetBeanName, targetDefinition);

    // Return the scoped proxy definition as primary bean definition
    // (potentially an inner bean).
    return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());
  }

  /**
   * Generate the bean name that is used within the scoped proxy to reference the target bean.
   *
   * @param originalBeanName the original name of bean
   * @return the generated bean to be used to reference the target bean
   * @see #getOriginalBeanName(String)
   */
  public static String getTargetBeanName(String originalBeanName) {
    return TARGET_NAME_PREFIX + originalBeanName;
  }

  /**
   * Get the original bean name for the provided {@linkplain #getTargetBeanName
   * target bean name}.
   *
   * @param targetBeanName the target bean name for the scoped proxy
   * @return the original bean name
   * @throws IllegalArgumentException if the supplied bean name does not refer
   * to the target of a scoped proxy
   * @see #getTargetBeanName(String)
   * @see #isScopedTarget(String)
   */
  public static String getOriginalBeanName(@Nullable String targetBeanName) {
    if (isScopedTarget(targetBeanName)) {
      return targetBeanName.substring(TARGET_NAME_PREFIX_LENGTH);
    }
    throw new IllegalArgumentException(
            "bean name '" + targetBeanName + "' does not refer to the target of a scoped proxy");
  }

  /**
   * Determine if the {@code beanName} is the name of a bean that references
   * the target bean within a scoped proxy.
   */
  public static boolean isScopedTarget(@Nullable String beanName) {
    return beanName != null && beanName.startsWith(TARGET_NAME_PREFIX);
  }

}
