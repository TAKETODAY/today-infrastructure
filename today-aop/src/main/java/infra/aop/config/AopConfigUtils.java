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

package infra.aop.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import infra.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import infra.aop.framework.ProxyConfig;
import infra.aop.framework.autoproxy.AutoProxyUtils;
import infra.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.Ordered;
import infra.lang.Assert;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator should be registered yet multiple concrete
 * implementations are available. This class provides a simple escalation protocol,
 * allowing a caller to request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AopNamespaceUtils
 * @since 4.0
 */
public abstract class AopConfigUtils {

  /**
   * The bean name of the internally managed auto-proxy creator.
   */
  public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
          "infra.aop.config.internalAutoProxyCreator";

  /**
   * Stores the auto proxy creator classes in escalation order.
   */
  private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);

  static {
    // Set up the escalation list...
    APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
    APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
    APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
  }

  @Nullable
  public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
    return registerAutoProxyCreatorIfNecessary(registry, null);
  }

  @Nullable
  public static BeanDefinition registerAutoProxyCreatorIfNecessary(
          BeanDefinitionRegistry registry, @Nullable Object source) {

    return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
  }

  @Nullable
  public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
    return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
  }

  @Nullable
  public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(
          BeanDefinitionRegistry registry, @Nullable Object source) {

    return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
  }

  @Nullable
  public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
    return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
  }

  @Nullable
  public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
          BeanDefinitionRegistry registry, @Nullable Object source) {

    return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
  }

  public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
    defaultProxyConfig(registry).getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
  }

  public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
    defaultProxyConfig(registry).getPropertyValues().add("exposeProxy", Boolean.TRUE);
  }

  private static BeanDefinition defaultProxyConfig(BeanDefinitionRegistry registry) {
    if (registry.containsBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME)) {
      return registry.getBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME);
    }
    RootBeanDefinition beanDefinition = new RootBeanDefinition(ProxyConfig.class);
    beanDefinition.setSource(AopConfigUtils.class);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    beanDefinition.setEnableDependencyInjection(false);
    registry.registerBeanDefinition(AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME, beanDefinition);
    return beanDefinition;
  }

  @Nullable
  private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
    Assert.notNull(registry, "BeanDefinitionRegistry is required");
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
      BeanDefinition def = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
      if (!cls.getName().equals(def.getBeanClassName())) {
        int currentPriority = findPriorityForClass(def.getBeanClassName());
        int requiredPriority = findPriorityForClass(cls);
        if (currentPriority < requiredPriority) {
          def.setBeanClassName(cls.getName());
        }
      }
      return null;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.setEnableDependencyInjection(false);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
    registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
    return beanDefinition;
  }

  private static int findPriorityForClass(Class<?> clazz) {
    return APC_PRIORITY_LIST.indexOf(clazz);
  }

  private static int findPriorityForClass(@Nullable String className) {
    for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
      Class<?> clazz = APC_PRIORITY_LIST.get(i);
      if (clazz.getName().equals(className)) {
        return i;
      }
    }
    throw new IllegalArgumentException(
            "Class name [" + className + "] is not a known auto-proxy creator class");
  }

}
