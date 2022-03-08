/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Registers an auto proxy creator against the current {@link BeanDefinitionRegistry}
 * as appropriate based on an {@code @Enable*} annotation having {@code mode} and
 * {@code proxyTargetClass} attributes set to the correct values.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.cache.annotation.EnableCaching
 * @see cn.taketoday.transaction.annotation.EnableTransactionManagement
 * @since 4.0 2022/1/11 23:38
 */
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
  private static final Logger log = LoggerFactory.getLogger(AutoProxyRegistrar.class);

  /**
   * The bean name of the internally managed auto-proxy creator.
   */
  public static final String AUTO_PROXY_CREATOR_BEAN_NAME = "cn.taketoday.aop.internalAutoProxyCreator";

  /**
   * Register, escalate, and configure the standard auto proxy creator (APC) against the
   * given registry. Works by finding the nearest annotation declared on the importing
   * {@code @Configuration} class that has both {@code mode} and {@code proxyTargetClass}
   * attributes. If {@code mode} is set to {@code PROXY}, the APC is registered; if
   * {@code proxyTargetClass} is set to {@code true}, then the APC is forced to use
   * subclass (CGLIB) proxying.
   * <p>Several {@code @Enable*} annotations expose both {@code mode} and
   * {@code proxyTargetClass} attributes. It is important to note that most of these
   * capabilities end up sharing a {@linkplain #AUTO_PROXY_CREATOR_BEAN_NAME
   * single APC}. For this reason, this implementation doesn't "care" exactly which
   * annotation it finds -- as long as it exposes the right {@code mode} and
   * {@code proxyTargetClass} attributes, the APC can be registered and configured all
   * the same.
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();

    boolean candidateFound = false;
    Set<String> annTypes = importMetadata.getAnnotationTypes();
    for (String annType : annTypes) {
      MergedAnnotation<Annotation> annotation = importMetadata.getAnnotation(annType);
      if (annotation.isPresent()) {
        Optional<AdviceMode> mode = annotation.getValue("mode", AdviceMode.class);
        if (mode.isPresent()) {
          candidateFound = true;
          if (mode.get() == AdviceMode.PROXY) {
            registerAutoProxyCreatorIfNecessary(registry);
            boolean force = false;
            Optional<Boolean> proxyTargetClass = annotation.getValue("proxyTargetClass", boolean.class);
            if (proxyTargetClass.isPresent() && proxyTargetClass.get()) {
              forceAutoProxyCreatorToUseClassProxying(registry);
              force = true;
            }

            Optional<Boolean> exposeProxy = annotation.getValue("exposeProxy", boolean.class);
            if (exposeProxy.isPresent() && exposeProxy.get()) {
              forceAutoProxyCreatorToExposeProxy(registry);
              force = true;
            }

            if (force) {
              return;
            }
          }
        }
      }
    }

    if (!candidateFound && log.isInfoEnabled()) {
      String name = getClass().getSimpleName();
      log.info(String.format("%s was imported but no annotations were found " +
              "having both 'mode' and 'proxyTargetClass' attributes of type " +
              "AdviceMode and boolean respectively. This means that auto proxy " +
              "creator registration and configuration may not have occurred as " +
              "intended, and components may not be proxied as expected. Check to " +
              "ensure that %s has been @Import'ed on the same class where these " +
              "annotations are declared; otherwise remove the import of %s " +
              "altogether.", name, name, name));
    }
  }

  public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
    BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
    if (definition != null) {
      definition.addPropertyValue("proxyTargetClass", Boolean.TRUE);
    }
  }

  public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
    BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
    if (definition != null) {
      definition.addPropertyValue("exposeProxy", Boolean.TRUE);
    }
  }

  @Nullable
  public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
    return registerAutoProxyCreatorIfNecessary(registry, null);
  }

  @Nullable
  public static BeanDefinition registerAutoProxyCreatorIfNecessary(
          BeanDefinitionRegistry registry, @Nullable Object source) {
    return registerAutoProxyCreatorIfNecessary(AspectAutoProxyCreator.class, registry, source);
  }

  @Nullable
  public static BeanDefinition registerAutoProxyCreatorIfNecessary(Class<?> cls, BeanDefinitionRegistry registry) {
    return registerAutoProxyCreatorIfNecessary(cls, registry, null);
  }

  @Nullable
  public static BeanDefinition registerAutoProxyCreatorIfNecessary(
          Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    BeanDefinition beanDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
    if (beanDefinition != null) {
      beanDefinition.setBeanClassName(cls.getName());
      return null;
    }

    beanDefinition = new BeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.addPropertyValue("order", Ordered.HIGHEST_PRECEDENCE);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
    return beanDefinition;
  }

}
