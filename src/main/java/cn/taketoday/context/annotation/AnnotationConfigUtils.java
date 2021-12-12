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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.Lazy;
import cn.taketoday.beans.Primary;
import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionHolder;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.beans.factory.support.InitDestroyAnnotationBeanPostProcessor;
import cn.taketoday.context.DefaultApplicationContext;
import cn.taketoday.context.event.DefaultEventListenerFactory;
import cn.taketoday.context.event.MethodEventDrivenPostProcessor;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Utility class that allows for convenient registration of common
 * {@link cn.taketoday.beans.factory.BeanPostProcessor} and
 * {@link cn.taketoday.beans.factory.BeanFactoryPostProcessor}
 * definitions for annotation-based configuration.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see ConfigurationClassPostProcessor
 * @since 4.0
 */
public abstract class AnnotationConfigUtils {

  /**
   * The bean name of the internally managed Configuration annotation processor.
   */
  public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalConfigurationAnnotationProcessor";

  /**
   * The bean name of the internally managed BeanNameGenerator for use when processing
   * {@link Configuration} classes. Set by {@link cn.taketoday.context.StandardApplicationContext}
   * and {@code AnnotationConfigWebApplicationContext} during bootstrap in order to make
   * any custom name generation strategy available to the underlying
   * {@link ConfigurationClassPostProcessor}.
   */
  public static final String CONFIGURATION_BEAN_NAME_GENERATOR =
          "cn.taketoday.context.annotation.internalConfigurationBeanNameGenerator";

  /**
   * The bean name of the internally managed Autowired annotation processor.
   */
  public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalAutowiredAnnotationProcessor";

  /**
   * The bean name of the internally managed common annotation processor.
   */
  public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalCommonAnnotationProcessor";

  /**
   * The bean name of the internally managed JSR-250 annotation processor.
   */
  private static final String JSR250_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalJsr250AnnotationProcessor";

  /**
   * The bean name of the internally managed JPA annotation processor.
   */
  public static final String PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalPersistenceAnnotationProcessor";

  private static final String PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME =
          "cn.taketoday.orm.jpa.support.PersistenceAnnotationBeanPostProcessor";

  /**
   * The bean name of the internally managed @EventListener annotation processor.
   */
  public static final String EVENT_LISTENER_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.event.internalEventListenerProcessor";

  /**
   * The bean name of the internally managed EventListenerFactory.
   */
  public static final String EVENT_LISTENER_FACTORY_BEAN_NAME =
          "cn.taketoday.context.event.internalEventListenerFactory";

  private static final ClassLoader classLoader = AnnotationConfigUtils.class.getClassLoader();

  private static final boolean jakartaAnnotationsPresent =
          ClassUtils.isPresent("jakarta.annotation.PostConstruct", classLoader);

  private static final boolean jsr250Present =
          ClassUtils.isPresent("javax.annotation.PostConstruct", classLoader);

  private static final boolean jpaPresent =
          ClassUtils.isPresent("jakarta.persistence.EntityManagerFactory", classLoader) &&
                  ClassUtils.isPresent(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, classLoader);

  /**
   * Register all relevant annotation post processors in the given registry.
   *
   * @param registry the registry to operate on
   */
  public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
    registerAnnotationConfigProcessors(registry, null);
  }

  /**
   * Register all relevant annotation post processors in the given registry.
   *
   * @param registry the registry to operate on
   * @param source the configuration source element (already extracted)
   * that this registration was triggered from. May be {@code null}.
   * @return a Set of BeanDefinitionHolders, containing all bean definitions
   * that have actually been registered by this call
   */
  public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
          BeanDefinitionRegistry registry, @Nullable Object source) {

    StandardBeanFactory beanFactory = unwrapStandardBeanFactory(registry);
    if (beanFactory != null) {
      if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
        beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
      }
    }

    Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);

    if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      BeanDefinition def = new BeanDefinition(ConfigurationClassPostProcessor.class);
      def.setSource(source);
      beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    // Check for JSR-250 support, and if present add an InitDestroyAnnotationBeanPostProcessor
    // for the javax variant of PostConstruct/PreDestroy.
    if (jsr250Present && !registry.containsBeanDefinition(JSR250_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      try {
        BeanDefinition def = new BeanDefinition(InitDestroyAnnotationBeanPostProcessor.class);
        def.propertyValues().add("initAnnotationType", classLoader.loadClass("javax.annotation.PostConstruct"));
        def.propertyValues().add("destroyAnnotationType", classLoader.loadClass("javax.annotation.PreDestroy"));
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, JSR250_ANNOTATION_PROCESSOR_BEAN_NAME));
      }
      catch (ClassNotFoundException ex) {
        // Failed to load javax variants of the annotation types -> ignore.
      }
    }

    // Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
    if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      BeanDefinition def = new BeanDefinition();
      try {
        def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
                AnnotationConfigUtils.class.getClassLoader()));
      }
      catch (ClassNotFoundException ex) {
        throw new IllegalStateException(
                "Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
      }
      def.setSource(source);
      beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
      BeanDefinition def = new BeanDefinition(MethodEventDrivenPostProcessor.class);
      def.setSource(source);
      beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
      BeanDefinition def = new BeanDefinition(DefaultEventListenerFactory.class);
      def.setSource(source);
      beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
    }

    return beanDefs;
  }

  private static BeanDefinitionHolder registerPostProcessor(
          BeanDefinitionRegistry registry, BeanDefinition definition, String beanName) {

    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(beanName, definition);
    return new BeanDefinitionHolder(definition, beanName);
  }

  @Nullable
  private static StandardBeanFactory unwrapStandardBeanFactory(BeanDefinitionRegistry registry) {
    if (registry instanceof StandardBeanFactory) {
      return (StandardBeanFactory) registry;
    }
    else if (registry instanceof DefaultApplicationContext) {
      return ((DefaultApplicationContext) registry).getBeanFactory();
    }
    else {
      return null;
    }
  }

  public static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd) {
    processCommonDefinitionAnnotations(abd, abd.getMetadata());
  }

  static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd, AnnotatedTypeMetadata metadata) {
    AnnotationAttributes lazy = attributesFor(metadata, Lazy.class);
    if (lazy != null) {
      abd.setLazyInit(lazy.getBoolean("value"));
    }
    else if (abd.getMetadata() != metadata) {
      lazy = attributesFor(abd.getMetadata(), Lazy.class);
      if (lazy != null) {
        abd.setLazyInit(lazy.getBoolean("value"));
      }
    }

    if (metadata.isAnnotated(Primary.class.getName())) {
      abd.setPrimary(true);
    }
    AnnotationAttributes dependsOn = attributesFor(metadata, DependsOn.class);
    if (dependsOn != null) {
      abd.setDependsOn(dependsOn.getStringArray("value"));
    }

    AnnotationAttributes role = attributesFor(metadata, Role.class);
    if (role != null) {
      abd.setRole(role.getNumber("value").intValue());
    }
    AnnotationAttributes description = attributesFor(metadata, Description.class);
    if (description != null) {
      abd.setDescription(description.getString("value"));
    }
  }

  @Nullable
  static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, Class<?> annotationClass) {
    return attributesFor(metadata, annotationClass.getName());
  }

  @Nullable
  static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, String annotationClassName) {
    return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotationClassName, false));
  }

  static Set<AnnotationAttributes> attributesForRepeatable(AnnotationMetadata metadata,
                                                           Class<?> containerClass, Class<?> annotationClass) {

    return attributesForRepeatable(metadata, containerClass.getName(), annotationClass.getName());
  }

  @SuppressWarnings("unchecked")
  static Set<AnnotationAttributes> attributesForRepeatable(
          AnnotationMetadata metadata, String containerClassName, String annotationClassName) {

    Set<AnnotationAttributes> result = new LinkedHashSet<>();

    // Direct annotation present?
    addAttributesIfNotNull(result, metadata.getAnnotationAttributes(annotationClassName, false));

    // Container annotation present?
    Map<String, Object> container = metadata.getAnnotationAttributes(containerClassName, false);
    if (container != null && container.containsKey("value")) {
      for (Map<String, Object> containedAttributes : (Map<String, Object>[]) container.get("value")) {
        addAttributesIfNotNull(result, containedAttributes);
      }
    }

    // Return merged result
    return Collections.unmodifiableSet(result);
  }

  private static void addAttributesIfNotNull(
          Set<AnnotationAttributes> result, @Nullable Map<String, Object> attributes) {

    if (attributes != null) {
      result.add(AnnotationAttributes.fromMap(attributes));
    }
  }

}
