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

package infra.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;

import infra.aop.framework.ProxyConfig;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.Conventions;
import infra.util.StringUtils;

/**
 * Utilities for auto-proxy aware components.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractAutoProxyCreator
 * @see AbstractBeanFactoryAwareAdvisingPostProcessor
 * @since 4.0 2022/3/9 16:34
 */
public abstract class AutoProxyUtils {

  /**
   * The bean name of the internally managed auto-proxy creator.
   *
   * @since 5.0
   */
  public static final String DEFAULT_PROXY_CONFIG_BEAN_NAME =
          "infra.aop.framework.autoproxy.defaultProxyConfig";

  /**
   * Bean definition attribute that may indicate the interfaces to be proxied
   * (in case of it getting proxied in the first place). The value is either
   * a single interface {@code Class} or an array of {@code Class}, with an
   * empty array specifically signalling that all implemented interfaces need
   * to be proxied.
   *
   * @see #determineExposedInterfaces
   * @since 5.0
   */
  public static final String EXPOSED_INTERFACES_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "exposedInterfaces");

  /**
   * Attribute value for specifically signalling that all implemented interfaces
   * need to be proxied (through an empty {@code Class} array).
   *
   * @see #EXPOSED_INTERFACES_ATTRIBUTE
   * @since 5.0
   */
  public static final Object ALL_INTERFACES_ATTRIBUTE_VALUE = new Class<?>[0];

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
  public static boolean shouldProxyTargetClass(ConfigurableBeanFactory beanFactory, @Nullable String beanName) {
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
   * @see BeanFactory#getType(String)
   */
  @Nullable
  public static Class<?> determineTargetClass(ConfigurableBeanFactory beanFactory, @Nullable String beanName) {
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
  static void exposeTargetClass(ConfigurableBeanFactory beanFactory, @Nullable String beanName, Class<?> targetClass) {
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

  /**
   * Apply default ProxyConfig settings to the given ProxyConfig instance, if necessary.
   *
   * @param proxyConfig the current ProxyConfig instance
   * @param beanFactory the BeanFactory to take the default ProxyConfig from
   * @see #DEFAULT_PROXY_CONFIG_BEAN_NAME
   * @see ProxyConfig#copyDefault
   * @since 5.0
   */
  static void applyDefaultProxyConfig(ProxyConfig proxyConfig, BeanFactory beanFactory) {
    if (beanFactory.containsBean(DEFAULT_PROXY_CONFIG_BEAN_NAME)) {
      ProxyConfig defaultProxyConfig = beanFactory.getBean(DEFAULT_PROXY_CONFIG_BEAN_NAME, ProxyConfig.class);
      proxyConfig.copyDefault(defaultProxyConfig);
    }
  }

  /**
   * Determine the specific interfaces for proxying the given bean, if any.
   * Checks the {@link #EXPOSED_INTERFACES_ATTRIBUTE "exposedInterfaces" attribute}
   * of the corresponding bean definition.
   *
   * @param beanFactory the containing ConfigurableListableBeanFactory
   * @param beanName the name of the bean
   * @return whether the given bean should be proxied with its target class
   * @see #EXPOSED_INTERFACES_ATTRIBUTE
   * @since 5.0
   */
  @Nullable
  static Class<?>[] determineExposedInterfaces(ConfigurableBeanFactory beanFactory, @Nullable String beanName) {
    if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
      BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
      Object interfaces = bd.getAttribute(EXPOSED_INTERFACES_ATTRIBUTE);
      if (interfaces instanceof Class<?>[] ifcs) {
        return ifcs;
      }
      else if (interfaces instanceof Class<?> ifc) {
        return new Class<?>[] { ifc };
      }
    }
    return null;
  }

}

