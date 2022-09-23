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

package cn.taketoday.framework.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.DeterminableImports;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} to allow {@code @Import} annotations to be used directly on
 * test classes.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ImportsContextCustomizerFactory
 */
class ImportsContextCustomizer implements ContextCustomizer {

  private static final String TEST_CLASS_NAME_ATTRIBUTE = "testClassName";

  private final String testClassName;

  private final ContextCustomizerKey key;

  ImportsContextCustomizer(Class<?> testClass) {
    this.testClassName = testClass.getName();
    this.key = new ContextCustomizerKey(testClass);
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context,
          MergedContextConfiguration mergedContextConfiguration) {
    BeanDefinitionRegistry registry = getBeanDefinitionRegistry(context);
    AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
    registerCleanupPostProcessor(registry, reader);
    registerImportsConfiguration(registry, reader);
  }

  private void registerCleanupPostProcessor(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
    BeanDefinition definition = registerBean(registry, reader, ImportsCleanupPostProcessor.BEAN_NAME,
            ImportsCleanupPostProcessor.class);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    definition.getConstructorArgumentValues().addIndexedArgumentValue(0, this.testClassName);
  }

  private void registerImportsConfiguration(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
    BeanDefinition definition = registerBean(registry, reader, ImportsConfiguration.BEAN_NAME,
            ImportsConfiguration.class);
    definition.setAttribute(TEST_CLASS_NAME_ATTRIBUTE, this.testClassName);
  }

  private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
    if (context instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
      return beanDefinitionRegistry;
    }
    if (context instanceof AbstractApplicationContext abstractContext) {
      return (BeanDefinitionRegistry) abstractContext.getBeanFactory();
    }
    throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
  }

  private BeanDefinition registerBean(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader,
          String beanName, Class<?> type) {
    reader.registerBean(type, beanName);
    return registry.getBeanDefinition(beanName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    // ImportSelectors are flexible so the only safe cache key is the test class
    ImportsContextCustomizer other = (ImportsContextCustomizer) obj;
    return this.key.equals(other.key);
  }

  @Override
  public int hashCode() {
    return this.key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("key", this.key).toString();
  }

  /**
   * {@link Configuration @Configuration} registered to trigger the
   * {@link ImportsSelector}.
   */
  @Configuration(proxyBeanMethods = false)
  @Import(ImportsSelector.class)
  static class ImportsConfiguration {

    static final String BEAN_NAME = ImportsConfiguration.class.getName();

  }

  /**
   * {@link ImportSelector} that returns the original test class so that direct
   * {@code @Import} annotations are processed.
   */
  static class ImportsSelector implements ImportSelector, BeanFactoryAware {

    private static final String[] NO_IMPORTS = {};

    private ConfigurableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
      this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      BeanDefinition definition = this.beanFactory.getBeanDefinition(ImportsConfiguration.BEAN_NAME);
      Object testClassName = definition.getAttribute(TEST_CLASS_NAME_ATTRIBUTE);
      return (testClassName != null) ? new String[] { (String) testClassName } : NO_IMPORTS;
    }

  }

  /**
   * {@link BeanDefinitionRegistryPostProcessor} to cleanup temporary configuration
   * added to load imports.
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  static class ImportsCleanupPostProcessor implements BeanDefinitionRegistryPostProcessor {

    static final String BEAN_NAME = ImportsCleanupPostProcessor.class.getName();

    private final String testClassName;

    ImportsCleanupPostProcessor(String testClassName) {
      this.testClassName = testClassName;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      try {
        String[] names = registry.getBeanDefinitionNames();
        for (String name : names) {
          BeanDefinition definition = registry.getBeanDefinition(name);
          if (this.testClassName.equals(definition.getBeanClassName())) {
            registry.removeBeanDefinition(name);
          }
        }
        registry.removeBeanDefinition(ImportsConfiguration.BEAN_NAME);
      }
      catch (NoSuchBeanDefinitionException ex) {
      }
    }

  }

  /**
   * The key used to ensure correct application context caching. Keys are generated
   * based on <em>all</em> the annotations used with the test that aren't core Java or
   * Kotlin annotations. We must use something broader than just {@link Import @Import}
   * annotations since an {@code @Import} may use an {@link ImportSelector} which could
   * make decisions based on anything available from {@link AnnotationMetadata}.
   */
  static class ContextCustomizerKey {

    private static final Class<?>[] NO_IMPORTS = {};

    private static final Set<AnnotationFilter> ANNOTATION_FILTERS;

    static {
      Set<AnnotationFilter> filters = new HashSet<>();
      filters.add(new JavaLangAnnotationFilter());
      filters.add(new KotlinAnnotationFilter());
      filters.add(new SpockAnnotationFilter());
      filters.add(new JUnitAnnotationFilter());
      ANNOTATION_FILTERS = Collections.unmodifiableSet(filters);
    }

    private final Set<Object> key;

    ContextCustomizerKey(Class<?> testClass) {
      Set<Annotation> annotations = new HashSet<>();
      Set<Class<?>> seen = new HashSet<>();
      collectClassAnnotations(testClass, annotations, seen);
      Set<Object> determinedImports = determineImports(annotations, testClass);
      this.key = Collections.unmodifiableSet((determinedImports != null) ? determinedImports : annotations);
    }

    private void collectClassAnnotations(Class<?> classType, Set<Annotation> annotations, Set<Class<?>> seen) {
      if (seen.add(classType)) {
        collectElementAnnotations(classType, annotations, seen);
        for (Class<?> interfaceType : classType.getInterfaces()) {
          collectClassAnnotations(interfaceType, annotations, seen);
        }
        if (classType.getSuperclass() != null) {
          collectClassAnnotations(classType.getSuperclass(), annotations, seen);
        }
      }
    }

    private void collectElementAnnotations(AnnotatedElement element, Set<Annotation> annotations,
            Set<Class<?>> seen) {
      for (Annotation annotation : element.getDeclaredAnnotations()) {
        if (!isIgnoredAnnotation(annotation)) {
          annotations.add(annotation);
          collectClassAnnotations(annotation.annotationType(), annotations, seen);
        }
      }
    }

    private boolean isIgnoredAnnotation(Annotation annotation) {
      for (AnnotationFilter annotationFilter : ANNOTATION_FILTERS) {
        if (annotationFilter.isIgnored(annotation)) {
          return true;
        }
      }
      return false;
    }

    private Set<Object> determineImports(Set<Annotation> annotations, Class<?> testClass) {
      Set<Object> determinedImports = new LinkedHashSet<>();
      AnnotationMetadata testClassMetadata = AnnotationMetadata.introspect(testClass);
      for (Annotation annotation : annotations) {
        for (Class<?> source : getImports(annotation)) {
          Set<Object> determinedSourceImports = determineImports(source, testClassMetadata);
          if (determinedSourceImports == null) {
            return null;
          }
          determinedImports.addAll(determinedSourceImports);
        }
      }
      return determinedImports;
    }

    private Class<?>[] getImports(Annotation annotation) {
      if (annotation instanceof Import importAnnotation) {
        return importAnnotation.value();
      }
      return NO_IMPORTS;
    }

    private Set<Object> determineImports(Class<?> source, AnnotationMetadata metadata) {
      if (DeterminableImports.class.isAssignableFrom(source)) {
        // We can determine the imports
        return ((DeterminableImports) instantiate(source)).determineImports(metadata);
      }
      if (ImportSelector.class.isAssignableFrom(source)
              || ImportBeanDefinitionRegistrar.class.isAssignableFrom(source)) {
        // Standard ImportSelector and ImportBeanDefinitionRegistrar could
        // use anything to determine the imports so we can't be sure
        return null;
      }
      // The source itself is the import
      return Collections.singleton(source.getName());
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> source) {
      try {
        Constructor<?> constructor = source.getDeclaredConstructor();
        ReflectionUtils.makeAccessible(constructor);
        return (T) constructor.newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Unable to instantiate DeterminableImportSelector " + source.getName(),
                ex);
      }
    }

    @Override
    public boolean equals(Object obj) {
      return (obj != null && getClass() == obj.getClass() && this.key.equals(((ContextCustomizerKey) obj).key));
    }

    @Override
    public int hashCode() {
      return this.key.hashCode();
    }

    @Override
    public String toString() {
      return this.key.toString();
    }

  }

  /**
   * Filter used to limit considered annotations.
   */
  private interface AnnotationFilter {

    boolean isIgnored(Annotation annotation);

  }

  /**
   * {@link AnnotationFilter} for {@literal java.lang} annotations.
   */
  private static final class JavaLangAnnotationFilter implements AnnotationFilter {

    @Override
    public boolean isIgnored(Annotation annotation) {
      return AnnotationUtils.isInJavaLangAnnotationPackage(annotation);
    }

  }

  /**
   * {@link AnnotationFilter} for Kotlin annotations.
   */
  private static final class KotlinAnnotationFilter implements AnnotationFilter {

    @Override
    public boolean isIgnored(Annotation annotation) {
      return "kotlin.Metadata".equals(annotation.annotationType().getName())
              || isInKotlinAnnotationPackage(annotation);
    }

    private boolean isInKotlinAnnotationPackage(Annotation annotation) {
      return annotation.annotationType().getName().startsWith("kotlin.annotation.");
    }

  }

  /**
   * {@link AnnotationFilter} for Spock annotations.
   */
  private static final class SpockAnnotationFilter implements AnnotationFilter {

    @Override
    public boolean isIgnored(Annotation annotation) {
      return annotation.annotationType().getName().startsWith("org.spockframework.")
              || annotation.annotationType().getName().startsWith("spock.");
    }

  }

  /**
   * {@link AnnotationFilter} for JUnit annotations.
   */
  private static final class JUnitAnnotationFilter implements AnnotationFilter {

    @Override
    public boolean isIgnored(Annotation annotation) {
      return annotation.annotationType().getName().startsWith("org.junit.");
    }

  }

}
