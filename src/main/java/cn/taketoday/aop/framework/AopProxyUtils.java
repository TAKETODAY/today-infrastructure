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

package cn.taketoday.aop.framework;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.aop.TargetClassAware;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.DecoratingProxy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Utility methods for AOP proxy factories.
 * Mainly for internal use within the AOP framework.
 *
 * <p>See {@link AopUtils} for a collection of
 * generic AOP utility methods which do not depend on AOP framework internals.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:49
 * @see AopUtils
 * @since 3.0
 */
public abstract class AopProxyUtils {

  /**
   * Bean definition attribute that may indicate whether a given bean is supposed
   * to be proxied with its target class (in case of it getting proxied in the first
   * place). The value is {@code Boolean.TRUE} or {@code Boolean.FALSE}.
   * <p>Proxy factories can set this attribute if they built a target class proxy
   * for a specific bean, and want to enforce that bean can always be cast
   * to its target class (even if AOP advices get applied through auto-proxying).
   *
   * @see #shouldProxyTargetClass
   * @since 4.0
   */
  public static final String PRESERVE_TARGET_CLASS_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(AopProxyUtils.class, "preserveTargetClass");

  /**
   * Bean definition attribute that indicates the original target class of an
   * auto-proxied bean, e.g. to be used for the introspection of annotations
   * on the target class behind an interface-based proxy.
   *
   * @see #determineTargetClass
   * @since 4.0
   */
  public static final String ORIGINAL_TARGET_CLASS_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(AopProxyUtils.class, "originalTargetClass");

  /**
   * Determine whether the given bean should be proxied with its target
   * class rather than its interfaces. Checks the
   * {@link #PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
   * of the corresponding bean definition.
   *
   * @param beanFactory the containing ConfigurableBeanFactory
   * @param beanName the name of the bean
   * @return whether the given bean should be proxied with its target class
   * @since 4.0
   */
  public static boolean shouldProxyTargetClass(
          ConfigurableBeanFactory beanFactory, @Nullable String beanName) {
    if (beanName != null) {
      BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
      if (definition != null) {
        return Boolean.TRUE.equals(definition.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
      }
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
   * @since 4.0
   */
  @Nullable
  public static Class<?> determineTargetClass(
          ConfigurableBeanFactory beanFactory, @Nullable String beanName) {
    if (beanName == null) {
      return null;
    }

    BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
    if (definition != null) {
      Object attribute = definition.getAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE);
      if (attribute instanceof Class) {
        return (Class<?>) attribute;
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
   * @since 4.0
   */
  static void exposeTargetClass(
          ConfigurableBeanFactory beanFactory, @Nullable String beanName, Class<?> targetClass) {
    if (beanName != null) {
      BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
      if (definition != null) {
        definition.setAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE, targetClass);
      }
    }
  }

  /**
   * Obtain the singleton target object behind the given proxy, if any.
   *
   * @param candidate the (potential) proxy to check
   * @return the singleton target object managed in a {@link SingletonTargetSource},
   * or {@code null} in any other case (not a proxy, not an existing singleton target)
   * @see Advised#getTargetSource()
   * @see SingletonTargetSource#getTarget()
   */
  public static Object getSingletonTarget(Object candidate) {
    if (candidate instanceof Advised) {
      TargetSource targetSource = ((Advised) candidate).getTargetSource();
      if (targetSource instanceof SingletonTargetSource) {
        return ((SingletonTargetSource) targetSource).getTarget();
      }
    }
    return null;
  }

  /**
   * Determine the ultimate target class of the given bean instance, traversing
   * not only a top-level proxy but any number of nested proxies as well &mdash;
   * as long as possible without side effects, that is, just for singleton targets.
   *
   * @param candidate the instance to check (might be an AOP proxy)
   * @return the ultimate target class (or the plain class of the given
   * object as fallback; never {@code null})
   * @see TargetClassAware#getTargetClass()
   * @see Advised#getTargetSource()
   */
  public static Class<?> ultimateTargetClass(Object candidate) {
    Assert.notNull(candidate, "Candidate object must not be null");
    Object current = candidate;
    Class<?> result = null;
    while (current instanceof TargetClassAware) {
      result = ((TargetClassAware) current).getTargetClass();
      current = getSingletonTarget(current);
    }
    if (result == null) {
      result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
    }
    return result;
  }

  /**
   * Determine the complete set of interfaces to proxy for the given AOP configuration.
   * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
   * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
   * {@link StandardProxy} marker interface.
   *
   * @param advised the proxy config
   * @return the complete set of interfaces to proxy
   * @see StandardProxy
   * @see Advised
   * @since 3.0
   */
  public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
    return completeProxiedInterfaces(advised, false);
  }

  /**
   * Determine the complete set of interfaces to proxy for the given AOP configuration.
   * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
   * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
   * {@link StandardProxy} marker interface.
   *
   * @param advised the proxy config
   * @param decoratingProxy whether to expose the {@link DecoratingProxy} interface
   * @return the complete set of interfaces to proxy
   * @see StandardProxy
   * @see Advised
   * @see DecoratingProxy
   * @since 3.0
   */
  public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
    Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
    if (specifiedInterfaces.length == 0) {
      // No user-specified interfaces: check whether target class is an interface.
      Class<?> targetClass = advised.getTargetClass();
      if (targetClass != null) {
        if (targetClass.isInterface()) {
          advised.setInterfaces(targetClass);
        }
        else if (Proxy.isProxyClass(targetClass) || isLambda(targetClass)) {
          advised.setInterfaces(targetClass.getInterfaces());
        }
        specifiedInterfaces = advised.getProxiedInterfaces();
      }
    }
    List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
    for (Class<?> ifc : specifiedInterfaces) {
      // Only non-sealed interfaces are actually eligible for JDK proxying (on JDK 17)
      if (!ifc.isSealed()) {
        proxiedInterfaces.add(ifc);
      }
    }
    if (!advised.isInterfaceProxied(StandardProxy.class)) {
      proxiedInterfaces.add(StandardProxy.class);
    }
    if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
      proxiedInterfaces.add(Advised.class);
    }
    if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
      proxiedInterfaces.add(DecoratingProxy.class);
    }
    return ClassUtils.toClassArray(proxiedInterfaces);
  }

  /**
   * Extract the user-specified interfaces that the given proxy implements,
   * i.e. all non-Advised interfaces that the proxy implements.
   *
   * @param proxy the proxy to analyze (usually a JDK dynamic proxy)
   * @return all user-specified interfaces that the proxy implements,
   * in the original order (never {@code null} or empty)
   * @see Advised
   */
  public static Class<?>[] proxiedUserInterfaces(Object proxy) {
    Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
    int nonUserIfcCount = 0;
    if (proxy instanceof StandardProxy) {
      nonUserIfcCount++;
    }
    if (proxy instanceof Advised) {
      nonUserIfcCount++;
    }
    if (proxy instanceof DecoratingProxy) {
      nonUserIfcCount++;
    }
    Class<?>[] userInterfaces = Arrays.copyOf(proxyInterfaces, proxyInterfaces.length - nonUserIfcCount);
    Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
    return userInterfaces;
  }

  /**
   * Check equality of the proxies behind the given AdvisedSupport objects.
   * Not the same as equality of the AdvisedSupport objects:
   * rather, equality of interfaces, advisors and target sources.
   */
  public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
    return a == b || (
            equalsProxiedInterfaces(a, b)
                    && equalsAdvisors(a, b)
                    && a.getTargetSource().equals(b.getTargetSource())
    );
  }

  /**
   * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
   */
  public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
    return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
  }

  /**
   * Check equality of the advisors behind the given AdvisedSupport objects.
   */
  public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
    return a.getAdvisorCount() == b.getAdvisorCount() && Arrays.equals(a.getAdvisors(), b.getAdvisors());
  }

  /**
   * Determine if the supplied {@link Class} is a JVM-generated implementation
   * class for a lambda expression or method reference.
   * <p>This method makes a best-effort attempt at determining this, based on
   * checks that work on modern, main stream JVMs.
   *
   * @param clazz the class to check
   * @return {@code true} if the class is a lambda implementation class
   * @since 4.0
   */
  static boolean isLambda(Class<?> clazz) {
    return clazz.isSynthetic() && (clazz.getSuperclass() == Object.class)
            && (clazz.getInterfaces().length > 0) && clazz.getName().contains("$$Lambda");
  }

}
