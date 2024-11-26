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

package infra.context.annotation;

import java.io.IOException;
import java.util.Set;

import infra.aop.AopInfrastructureBean;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.bytecode.proxy.Enhancer;
import infra.context.BootstrapContext;
import infra.context.event.EventListenerFactory;
import infra.core.Conventions;
import infra.core.Ordered;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.Order;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;

/**
 * Utilities for identifying {@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ConfigurationClassUtils {

  public static final String CONFIGURATION_CLASS_FULL = "full";
  public static final String CONFIGURATION_CLASS_LITE = "lite";

  /**
   * When set to {@link Boolean#TRUE}, this attribute signals that the bean class
   * for the given {@link BeanDefinition} should be considered as a candidate
   * configuration class in 'lite' mode by default.
   * <p>For example, a class registered directly with an {@code ApplicationContext}
   * should always be considered a configuration class candidate.
   */
  static final String CANDIDATE_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "candidate");

  public static final String CONFIGURATION_CLASS_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

  private static final String ORDER_ATTRIBUTE =
          Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "order");

  private static final Logger log = LoggerFactory.getLogger(ConfigurationClassUtils.class);

  private static final Set<String> candidateIndicators = Set.of(
          Import.class.getName(), Component.class.getName(),
          ComponentScan.class.getName(), ImportResource.class.getName()
  );

  /**
   * Initialize a configuration class proxy for the specified class.
   *
   * @param userClass the configuration class to initialize
   */
  @SuppressWarnings("unused") // Used by AOT-optimized generated code
  public static Class<?> initializeConfigurationClass(Class<?> userClass) {
    Class<?> configurationClass = new ConfigurationClassEnhancer().enhance(userClass, null);
    Enhancer.registerStaticCallbacks(configurationClass, ConfigurationClassEnhancer.CALLBACKS);
    return configurationClass;
  }

  /**
   * Check whether the given bean definition is a candidate for a configuration class
   * (or a nested component class declared within a configuration/component class,
   * to be auto-registered as well), and mark it accordingly.
   *
   * @param beanDef the bean definition to check
   * @param bootstrapContext the current bootstrap context
   * @return whether the candidate qualifies as (any kind of) configuration class
   */
  static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, BootstrapContext bootstrapContext) {
    String className = beanDef.getBeanClassName();
    if (className == null || beanDef.getFactoryMethodName() != null) {
      return false;
    }

    AnnotationMetadata metadata;
    if (beanDef instanceof AnnotatedBeanDefinition annotated
            && className.equals(annotated.getMetadata().getClassName())) {
      // Can reuse the pre-parsed metadata from the given BeanDefinition...
      metadata = annotated.getMetadata();
    }
    else if (beanDef instanceof AbstractBeanDefinition abd && abd.hasBeanClass()) {
      // Check already loaded Class if present...
      // since we possibly can't even load the class file for this Class.
      Class<?> beanClass = abd.getBeanClass();
      if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass)
              || BeanPostProcessor.class.isAssignableFrom(beanClass)
              || AopInfrastructureBean.class.isAssignableFrom(beanClass)
              || EventListenerFactory.class.isAssignableFrom(beanClass)) {
        return false;
      }
      metadata = AnnotationMetadata.introspect(beanClass);
    }
    else {
      try {
        metadata = bootstrapContext.getAnnotationMetadata(className);
      }
      catch (IOException ex) {
        if (log.isDebugEnabled()) {
          log.debug("Could not find class file for introspecting configuration annotations: {}",
                  className, ex);
        }
        return false;
      }
    }

    MergedAnnotation<Configuration> config = metadata.getAnnotation(Configuration.class);
    if (config.isPresent() && !Boolean.FALSE.equals(config.getBoolean("proxyBeanMethods"))) {
      beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
    }
    else if (config.isPresent()
            || Boolean.TRUE.equals(beanDef.getAttribute(CANDIDATE_ATTRIBUTE))
            || isConfigurationCandidate(metadata)) {
      beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
    }
    else {
      return false;
    }

    // It's a full or lite configuration candidate... Let's determine the order value, if any.
    Integer order = getOrder(metadata);
    if (order != null) {
      beanDef.setAttribute(ORDER_ATTRIBUTE, order);
    }

    return true;
  }

  /**
   * Check the given metadata for a configuration class candidate
   * (or nested component class declared within a configuration/component class).
   *
   * @param metadata the metadata of the annotated class
   * @return {@code true} if the given class is to be registered for
   * configuration class processing; {@code false} otherwise
   */
  static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
    // Do not consider an interface or an annotation...
    if (metadata.isInterface()) {
      return false;
    }

    // Any of the typical annotations found?
    for (String indicator : candidateIndicators) {
      if (metadata.isAnnotated(indicator)) {
        return true;
      }
    }

    // Finally, let's look for @Component methods...
    return hasComponentMethods(metadata);
  }

  static boolean hasComponentMethods(AnnotationMetadata metadata) {
    try {
      return metadata.hasAnnotatedMethods(Component.class.getName());
    }
    catch (Throwable ex) {
      log.debug("Failed to introspect @Component methods on class [{}]: {}", metadata.getClassName(), ex.toString());
      return false;
    }
  }

  /**
   * Determine the order for the given configuration class metadata.
   *
   * @param metadata the metadata of the annotated class
   * @return the {@code @Order} annotation value on the configuration class,
   * or {@code Ordered.LOWEST_PRECEDENCE} if none declared
   */
  @Nullable
  public static Integer getOrder(AnnotationMetadata metadata) {
    MergedAnnotation<Order> orderAnnotation = metadata.getAnnotation(Order.class);
    return orderAnnotation.isPresent() ? orderAnnotation.getIntValue() : null;
  }

  /**
   * Determine the order for the given configuration class bean definition,
   * as set by {@link #checkConfigurationClassCandidate}.
   *
   * @param beanDef the bean definition to check
   * @return the {@link Order @Order} annotation value on the configuration class,
   * or {@link Ordered#LOWEST_PRECEDENCE} if none declared
   */
  public static int getOrder(BeanDefinition beanDef) {
    Object order = beanDef.getAttribute(ORDER_ATTRIBUTE);
    if (order instanceof Integer) {
      return (int) order;
    }
    return Ordered.LOWEST_PRECEDENCE;
  }

}
