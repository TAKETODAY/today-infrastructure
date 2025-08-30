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

package infra.context.annotation;

import java.util.function.Consumer;

import infra.aop.framework.autoproxy.AutoProxyUtils;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.beans.factory.annotation.EnableDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.event.DefaultEventListenerFactory;
import infra.context.event.EventListenerMethodProcessor;
import infra.context.support.GenericApplicationContext;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Utility class that allows for convenient registration of common
 * {@link BeanPostProcessor} and
 * {@link BeanFactoryPostProcessor}
 * definitions for annotation-based configuration.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationClassPostProcessor
 * @since 4.0
 */
public abstract class AnnotationConfigUtils {

  /**
   * The bean name of the internally managed Configuration annotation processor.
   */
  public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME =
          "infra.context.annotation.internalConfigurationAnnotationProcessor";

  /**
   * The bean name of the internally managed BeanNameGenerator for use when processing
   * {@link Configuration} classes. Set by {@link AnnotationConfigApplicationContext}
   * and {@code AnnotationConfigWebApplicationContext} during bootstrap in order to make
   * any custom name generation strategy available to the underlying
   * {@link ConfigurationClassPostProcessor}.
   */
  public static final String CONFIGURATION_BEAN_NAME_GENERATOR =
          "infra.context.annotation.internalConfigurationBeanNameGenerator";

  /**
   * The bean name of the internally managed Autowired annotation processor.
   */
  public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
          "infra.context.annotation.internalAutowiredAnnotationProcessor";

  /**
   * The bean name of the internally managed JPA annotation processor.
   */
  public static final String PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME =
          "infra.context.annotation.internalPersistenceAnnotationProcessor";

  private static final String PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME =
          "infra.orm.jpa.support.PersistenceAnnotationBeanPostProcessor";

  /**
   * The bean name of the internally managed @EventListener annotation processor.
   */
  public static final String EVENT_LISTENER_PROCESSOR_BEAN_NAME =
          "infra.context.event.internalEventListenerProcessor";

  /**
   * The bean name of the internally managed EventListenerFactory.
   */
  public static final String EVENT_LISTENER_FACTORY_BEAN_NAME =
          "infra.context.event.internalEventListenerFactory";

  /**
   * The bean name of the internally managed common annotation processor.
   */
  public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME =
          "infra.context.annotation.internalCommonAnnotationProcessor";

  private static final boolean jsr250Present = isPresent("javax.annotation.PostConstruct");
  private static final boolean jakartaAnnotationsPresent = isPresent("jakarta.annotation.PostConstruct");
  private static final boolean jpaPresent = isPresent("jakarta.persistence.EntityManagerFactory")
          && isPresent(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME);

  private static boolean isPresent(String className) {
    ClassLoader classLoader = AnnotationConfigUtils.class.getClassLoader();
    return ClassUtils.isPresent(className, classLoader);
  }

  public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
    registerAnnotationConfigProcessors(registry, null);
  }

  /**
   * Register all relevant annotation post processors in the given registry.
   *
   * @param registry the registry to operate on
   */
  public static void registerAnnotationConfigProcessors(
          BeanDefinitionRegistry registry, @Nullable Consumer<BeanDefinitionHolder> consumer) {
    StandardBeanFactory beanFactory = unwrapStandardBeanFactory(registry);
    if (beanFactory != null) {
      if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
        beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
      }

      if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
      }
    }

    if (!registry.containsBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
      registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, consumer);
    }

    if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
      registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, consumer);
    }

    // Check for Jakarta Annotations support, and if present add the CommonAnnotationBeanPostProcessor.
    if ((jakartaAnnotationsPresent || jsr250Present)
            && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
      registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME, consumer);
    }

    // Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
    if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      RootBeanDefinition def = new RootBeanDefinition();
      try {
        def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
                AnnotationConfigUtils.class.getClassLoader()));
      }
      catch (ClassNotFoundException ex) {
        throw new IllegalStateException(
                "Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
      }
      registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME, consumer);
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
      RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
      registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME, consumer);
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
      RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
      registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME, consumer);
    }
  }

  private static void registerPostProcessor(BeanDefinitionRegistry registry,
          RootBeanDefinition definition, String beanName, @Nullable Consumer<BeanDefinitionHolder> consumer) {

    definition.setEnableDependencyInjection(false);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(beanName, definition);

    if (consumer != null) {
      consumer.accept(new BeanDefinitionHolder(definition, beanName));
    }
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
    applyAnnotationMetadata(abd, true);
  }

  static BeanDefinitionHolder applyScopedProxyMode(
          ScopeMetadata metadata, BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {

    ScopedProxyMode scopedProxyMode = metadata.getScopedProxyMode();
    if (scopedProxyMode.equals(ScopedProxyMode.NO)) {
      return definition;
    }
    boolean proxyTargetClass = scopedProxyMode.equals(ScopedProxyMode.TARGET_CLASS);
    return ScopedProxyCreator.createScopedProxy(definition, registry, proxyTargetClass);
  }

  public static void applyAnnotationMetadata(AnnotatedBeanDefinition definition, boolean detectDIStatus) {
    AnnotatedTypeMetadata metadata = definition.getFactoryMethodMetadata();
    if (metadata == null) {
      metadata = definition.getMetadata();
    }
    MergedAnnotations annotations = metadata.getAnnotations();
    applyAnnotationMetadata(annotations, definition, detectDIStatus);
  }

  public static void applyAnnotationMetadata(MergedAnnotations annotations, BeanDefinition definition, boolean detectDIStatus) {
    if (annotations.isPresent(Primary.class)) {
      definition.setPrimary(true);
    }

    if (annotations.isPresent(Fallback.class)) {
      definition.setFallback(true);
    }

    MergedAnnotation<Lazy> lazyMergedAnnotation = annotations.get(Lazy.class);
    if (lazyMergedAnnotation.isPresent()) {
      definition.setLazyInit(lazyMergedAnnotation.getBooleanValue());
    }
    else if (definition instanceof AnnotatedBeanDefinition annotated) {
      AnnotationMetadata metadata = annotated.getMetadata();
      lazyMergedAnnotation = metadata.getAnnotation(Lazy.class);

      if (lazyMergedAnnotation.isPresent()) {
        definition.setLazyInit(lazyMergedAnnotation.getBooleanValue());
      }
    }

    MergedAnnotation<Role> roleMergedAnnotation = annotations.get(Role.class);
    if (roleMergedAnnotation.isPresent()) {
      definition.setRole(roleMergedAnnotation.getIntValue());
    }

    MergedAnnotation<DependsOn> dependsOn = annotations.get(DependsOn.class);
    if (dependsOn.isPresent()) {
      definition.setDependsOn(dependsOn.getStringValueArray());
    }

    MergedAnnotation<Description> description = annotations.get(Description.class);
    if (description.isPresent()) {
      definition.setDescription(description.getStringValue());
    }

    if (detectDIStatus) {
      if (annotations.isPresent(EnableDependencyInjection.class)) {
        definition.setEnableDependencyInjection(true);
      }
      else if (annotations.isPresent(DisableDependencyInjection.class)) {
        definition.setEnableDependencyInjection(false);
      }
    }

    MergedAnnotation<Proxyable> proxyable = annotations.get(Proxyable.class);
    if (proxyable.isPresent()) {
      ProxyType mode = proxyable.getEnum("value", ProxyType.class);
      if (mode == ProxyType.TARGET_CLASS) {
        definition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
      }
      else {
        Class<?>[] ifcs = proxyable.getClassArray("interfaces");
        if (ifcs.length > 0 || mode == ProxyType.INTERFACES) {
          definition.setAttribute(AutoProxyUtils.EXPOSED_INTERFACES_ATTRIBUTE, ifcs);
        }
      }
    }

  }

}
