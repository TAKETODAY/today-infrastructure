/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.framework.autoproxy;

import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Utilities for auto-proxy aware components.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractAutoProxyCreator
 * @since 4.0 2022/3/9 16:34
 */
public abstract class AutoProxyUtils {

  /**
   * Bean definition attribute that may indicate whether a given bean is supposed
   * to be proxied with its target class (in case of it getting proxied in the first
   * place). The value is {@code Boolean.TRUE} or {@code Boolean.FALSE}.
   * <p>Proxy factories can set this attribute if they built a target class proxy
   * for a specific bean, and want to enforce that bean can always be cast
   * to its target class (even if AOP advices get applied through auto-proxying).
   *
   * @see #shouldProxyTargetClass
   */
  public static final String PRESERVE_TARGET_CLASS_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "preserveTargetClass");

  /**
   * Bean definition attribute that indicates the original target class of an
   * auto-proxied bean, e.g. to be used for the introspection of annotations
   * on the target class behind an interface-based proxy.
   *
   * @see #determineTargetClass
   */
  public static final String ORIGINAL_TARGET_CLASS_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "originalTargetClass");

  /**
   * Determine whether the given bean should be proxied with its target
   * class rather than its interfaces. Checks the
   * {@link #PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
   * of the corresponding bean definition.
   *
   * @param beanFactory the containing ConfigurableBeanFactory
   * @param beanName the name of the bean
   * @return whether the given bean should be proxied with its target class
   */
  public static boolean shouldProxyTargetClass(
          ConfigurableBeanFactory beanFactory, @Nullable String beanName) {

    if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
      BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
      return Boolean.TRUE.equals(bd.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
    }
    return false;
  }

  /**
   * Determine the original target class for the specified bean, if possible,
   * otherwise falling back to a regular {@code getType} lookup.
   *
   * @param beanFactory the containing ConfigurableBeanFactory
   * @param beanName the name of the bean
   * @return the original target class as stored in the bean definition, if any
   * @see cn.taketoday.beans.factory.BeanFactory#getType(String)
   */
  @Nullable
  public static Class<?> determineTargetClass(
          ConfigurableBeanFactory beanFactory, @Nullable String beanName) {

    if (beanName == null) {
      return null;
    }
    if (beanFactory.containsBeanDefinition(beanName)) {
      BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
      Class<?> targetClass = (Class<?>) bd.getAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE);
      if (targetClass != null) {
        return targetClass;
      }
    }
    return beanFactory.getType(beanName);
  }

  /**
   * Expose the given target class for the specified bean, if possible.
   *
   * @param beanFactory the containing ConfigurableBeanFactory
   * @param beanName the name of the bean
   * @param targetClass the corresponding target class
   */
  static void exposeTargetClass(
          ConfigurableBeanFactory beanFactory, @Nullable String beanName, Class<?> targetClass) {

    if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
      beanFactory.getMergedBeanDefinition(beanName).setAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE, targetClass);
    }
  }

  /**
   * Determine whether the given bean name indicates an "original instance"
   * according to {@link AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX},
   * skipping any proxy attempts for it.
   *
   * @param beanName the name of the bean
   * @param beanClass the corresponding bean class
   * @see AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
   */
  static boolean isOriginalInstance(String beanName, Class<?> beanClass) {
    if (StringUtils.isEmpty(beanName)
            || beanName.length() != beanClass.getName().length() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX.length()) {
      return false;
    }
    return beanName.startsWith(beanClass.getName())
            && beanName.endsWith(AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX);
  }

}

