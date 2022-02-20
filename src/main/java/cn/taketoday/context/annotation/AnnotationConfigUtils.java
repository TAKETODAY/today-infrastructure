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

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.StandardDependenciesBeanPostProcessor;
import cn.taketoday.context.loader.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
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
   * The bean name of the internally managed BeanNamePopulator for use when processing
   * {@link Configuration} classes. Set by {@link StandardApplicationContext}
   * and {@code AnnotationConfigWebApplicationContext} during bootstrap in order to make
   * any custom name generation strategy available to the underlying
   * {@link ConfigurationClassPostProcessor}.
   */
  public static final String CONFIGURATION_BEAN_NAME_GENERATOR =
          "cn.taketoday.context.annotation.internalConfigurationBeanNamePopulator";

  /**
   * The bean name of the internally managed common annotation processor.
   */
  public static final String jakarta_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalJakartaAnnotationProcessor";

  /**
   * The bean name of the internally managed JSR-250 annotation processor.
   */
  private static final String JSR250_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalJsr250AnnotationProcessor";

  /**
   * The bean name of the internally managed Autowired annotation processor.
   */
  public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
          "cn.taketoday.context.annotation.internalAutowiredAnnotationProcessor";

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
    StandardBeanFactory beanFactory = unwrapStandardBeanFactory(registry);
    if (beanFactory != null) {
      if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
        beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
      }

      if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
      }
    }

    if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      BeanDefinition def = new BeanDefinition(StandardDependenciesBeanPostProcessor.class);
      registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME);
    }

    // Check for JSR-250 support, and if present add an InitDestroyAnnotationBeanPostProcessor
    // for the javax variant of PostConstruct/PreDestroy.
    if (ClassUtils.isPresent("jakarta.annotation.PostConstruct", classLoader)
            && !registry.containsBeanDefinition(jakarta_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      try {
        BeanDefinition def = new BeanDefinition(InitDestroyAnnotationBeanPostProcessor.class);
        def.propertyValues().add("initAnnotationType", classLoader.loadClass("jakarta.annotation.PostConstruct"));
        def.propertyValues().add("destroyAnnotationType", classLoader.loadClass("jakarta.annotation.PreDestroy"));
        registerPostProcessor(registry, def, jakarta_ANNOTATION_PROCESSOR_BEAN_NAME);
      }
      catch (ClassNotFoundException ex) {
        // Failed to load javax variants of the annotation types -> ignore.
      }
    }

    // Check for JSR-250 support, and if present add an InitDestroyAnnotationBeanPostProcessor
    // for the javax variant of PostConstruct/PreDestroy.
    if (jsr250Present && !registry.containsBeanDefinition(JSR250_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      try {
        BeanDefinition def = new BeanDefinition(InitDestroyAnnotationBeanPostProcessor.class);
        def.propertyValues().add("initAnnotationType", classLoader.loadClass("javax.annotation.PostConstruct"));
        def.propertyValues().add("destroyAnnotationType", classLoader.loadClass("javax.annotation.PreDestroy"));
        registerPostProcessor(registry, def, JSR250_ANNOTATION_PROCESSOR_BEAN_NAME);
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
      registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME);
    }

  }

  private static void registerPostProcessor(
          BeanDefinitionRegistry registry, BeanDefinition definition, String beanName) {
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(beanName, definition);
  }

  @Nullable
  private static StandardBeanFactory unwrapStandardBeanFactory(BeanDefinitionRegistry registry) {
    if (registry instanceof StandardBeanFactory) {
      return (StandardBeanFactory) registry;
    }
    else if (registry instanceof GenericApplicationContext) {
      return ((GenericApplicationContext) registry).getBeanFactory();
    }
    else {
      return null;
    }
  }

  public static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd) {
    AnnotatedBeanDefinitionReader.applyAnnotationMetadata(abd);
  }

}
