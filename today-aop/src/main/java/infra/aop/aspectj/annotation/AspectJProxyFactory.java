/*
 * Copyright 2002-present the original author or authors.
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

package infra.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.Advisor;
import infra.aop.aspectj.AspectJProxyUtils;
import infra.aop.aspectj.SimpleAspectInstanceFactory;
import infra.aop.framework.ProxyCreatorSupport;
import infra.aop.framework.ProxyFactory;
import infra.aop.support.AopUtils;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.lang.Assert;
import infra.util.ClassUtils;

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
 * @see ProxyFactory
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
    Assert.notNull(target, "Target object is required");
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
