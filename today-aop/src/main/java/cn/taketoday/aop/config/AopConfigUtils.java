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

package cn.taketoday.aop.config;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import cn.taketoday.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import cn.taketoday.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
 * @see AopNamespaceUtils
 * @since 4.0
 */
public abstract class AopConfigUtils {

  /**
   * The bean name of the internally managed auto-proxy creator.
   */
  public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
          "cn.taketoday.aop.config.internalAutoProxyCreator";

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
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
      BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
      definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
    }
  }

  public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
      BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
      definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
    }
  }

  @Nullable
  private static BeanDefinition registerOrEscalateApcAsRequired(
          Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
      BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
      if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
        int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
        int requiredPriority = findPriorityForClass(cls);
        if (currentPriority < requiredPriority) {
          apcDefinition.setBeanClassName(cls.getName());
        }
      }
      return null;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    beanDefinition.setEnableDependencyInjection(false);
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
