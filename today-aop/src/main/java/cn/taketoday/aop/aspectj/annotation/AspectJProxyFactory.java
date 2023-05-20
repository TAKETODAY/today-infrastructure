/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;

import java.io.Serial;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.aspectj.AspectJProxyUtils;
import cn.taketoday.aop.aspectj.SimpleAspectInstanceFactory;
import cn.taketoday.aop.framework.ProxyCreatorSupport;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * AspectJ-based proxy factory, allowing for programmatic building
 * of proxies which include AspectJ aspects (code style as well
 * Java 5 annotation style).
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @see #addAspect(Object)
 * @see #addAspect(Class)
 * @see #getProxy()
 * @see #getProxy(ClassLoader)
 * @see cn.taketoday.aop.framework.ProxyFactory
 * @since 4.0
 */
public class AspectJProxyFactory extends ProxyCreatorSupport {
  @Serial
  private static final long serialVersionUID = 1L;

  /** Cache for singleton aspect instances. */
  private static final ConcurrentHashMap<Class<?>, Object> aspectCache = new ConcurrentHashMap<>();

  private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();

  /**
   * Create a new AspectJProxyFactory.
   */
  public AspectJProxyFactory() { }

  /**
   * Create a new AspectJProxyFactory.
   * <p>Will proxy all interfaces that the given target implements.
   *
   * @param target the target object to be proxied
   */
  public AspectJProxyFactory(Object target) {
    Assert.notNull(target, "Target object must not be null");
    setInterfaces(ClassUtils.getAllInterfaces(target));
    setTarget(target);
  }

  /**
   * Create a new {@code AspectJProxyFactory}.
   * No target, only interfaces. Must add interceptors.
   */
  public AspectJProxyFactory(Class<?>... interfaces) {
    setInterfaces(interfaces);
  }

  /**
   * Add the supplied aspect instance to the chain. The type of the aspect instance
   * supplied must be a singleton aspect. True singleton lifecycle is not honoured when
   * using this method - the caller is responsible for managing the lifecycle of any
   * aspects added in this way.
   *
   * @param aspectInstance the AspectJ aspect instance
   */
  public void addAspect(Object aspectInstance) {
    Class<?> aspectClass = aspectInstance.getClass();
    String aspectName = aspectClass.getName();
    AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
    if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
      throw new IllegalArgumentException(
              "Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
    }
    addAdvisorsFromAspectInstanceFactory(
            new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
  }

  /**
   * Add an aspect of the supplied type to the end of the advice chain.
   *
   * @param aspectClass the AspectJ aspect class
   */
  public void addAspect(Class<?> aspectClass) {
    String aspectName = aspectClass.getName();
    AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
    var instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
    addAdvisorsFromAspectInstanceFactory(instanceFactory);
  }

  /**
   * Add all {@link Advisor Advisors} from the supplied {@link MetadataAwareAspectInstanceFactory}
   * to the current chain. Exposes any special purpose {@link Advisor Advisors} if needed.
   *
   * @see AspectJProxyUtils#makeAdvisorChainAspectJCapableIfNecessary(List)
   */
  private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
    List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
    Class<?> targetClass = getTargetClass();
    Assert.state(targetClass != null, "Unresolvable target class");
    advisors = AopUtils.filterAdvisors(advisors, targetClass);
    AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
    AnnotationAwareOrderComparator.sort(advisors);
    addAdvisors(advisors);
  }

  /**
   * Create an {@link AspectMetadata} instance for the supplied aspect type.
   *
   * @throws IllegalArgumentException aspectClass is not an @AspectJ aspect
   */
  private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
    return new AspectMetadata(aspectClass, aspectName);
  }

  /**
   * Create a {@link MetadataAwareAspectInstanceFactory} for the supplied aspect type. If the aspect type
   * has no per clause, then a {@link SingletonMetadataAwareAspectInstanceFactory} is returned, otherwise
   * a {@link PrototypeAspectInstanceFactory} is returned.
   */
  private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
          AspectMetadata am, Class<?> aspectClass, String aspectName) {

    MetadataAwareAspectInstanceFactory instanceFactory;
    if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
      // Create a shared aspect instance.
      Object instance = getSingletonAspectInstance(aspectClass);
      instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
    }
    else {
      // Create a factory for independent aspect instances.
      instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
    }
    return instanceFactory;
  }

  /**
   * Get the singleton aspect instance for the supplied aspect type.
   * An instance is created if one cannot be found in the instance cache.
   */
  private Object getSingletonAspectInstance(Class<?> aspectClass) {
    return aspectCache.computeIfAbsent(aspectClass,
            clazz -> new SimpleAspectInstanceFactory(clazz).getAspectInstance());
  }

  /**
   * Create a new proxy according to the settings in this factory.
   * <p>Can be called repeatedly. Effect will vary if we've added
   * or removed interfaces. Can add and remove interceptors.
   * <p>Uses a default class loader: Usually, the thread context class loader
   * (if necessary for proxy creation).
   *
   * @return the new proxy
   */
  @SuppressWarnings("unchecked")
  public <T> T getProxy() {
    return (T) createAopProxy().getProxy();
  }

  /**
   * Create a new proxy according to the settings in this factory.
   * <p>Can be called repeatedly. Effect will vary if we've added
   * or removed interfaces. Can add and remove interceptors.
   * <p>Uses the given class loader (if necessary for proxy creation).
   *
   * @param classLoader the class loader to create the proxy with
   * @return the new proxy
   */
  @SuppressWarnings("unchecked")
  public <T> T getProxy(@Nullable ClassLoader classLoader) {
    return (T) createAopProxy().getProxy(classLoader);
  }

}
