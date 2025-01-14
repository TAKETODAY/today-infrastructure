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

package infra.app.test.context;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import infra.context.BootstrapContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.ImportSelector;
import infra.context.annotation.config.DeterminableImports;
import infra.core.Ordered;
import infra.core.annotation.AnnotationFilter;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.Order;
import infra.core.style.ToStringBuilder;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.test.context.ContextCustomizer;
import infra.test.context.MergedContextConfiguration;
import infra.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} to allow {@code @Import} annotations to be used directly on
 * test classes.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ImportsContextCustomizerFactory
 * @since 4.0
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
    BootstrapContext bootstrapContext = context.getBootstrapContext();
    BeanDefinitionRegistry registry = bootstrapContext.getRegistry();
    AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
    registerCleanupPostProcessor(registry, reader);
    registerImportsConfiguration(registry, reader);
  }

  private void registerCleanupPostProcessor(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
    BeanDefinition definition = registerBean(registry, reader,
            ImportsCleanupPostProcessor.BEAN_NAME, ImportsCleanupPostProcessor.class);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    definition.setEnableDependencyInjection(false);
    definition.getConstructorArgumentValues().addIndexedArgumentValue(0, this.testClassName);
  }

  private void registerImportsConfiguration(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
    BeanDefinition definition = registerBean(registry, reader,
            ImportsConfiguration.BEAN_NAME, ImportsConfiguration.class);
    definition.setAttribute(TEST_CLASS_NAME_ATTRIBUTE, this.testClassName);
  }

  private BeanDefinition registerBean(BeanDefinitionRegistry registry,
          AnnotatedBeanDefinitionReader reader, String beanName, Class<?> type) {
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
    public String[] selectImports(AnnotationMetadata importMetadata) {
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
      catch (NoSuchBeanDefinitionException ignored) { }
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

    private static final Set<AnnotationFilter> ANNOTATION_FILTERS;

    static {
      var annotationFilters = new LinkedHashSet<AnnotationFilter>();
      annotationFilters.add(AnnotationFilter.PLAIN);
      annotationFilters.add("kotlin.Metadata"::equals);
      annotationFilters.add(AnnotationFilter.packages("kotlin.annotation"));
      annotationFilters.add(AnnotationFilter.packages("org.spockframework", "spock"));
      annotationFilters.add(AnnotationFilter.packages("org.junit"));
      ANNOTATION_FILTERS = Collections.unmodifiableSet(annotationFilters);
    }

    private final Set<Object> key;

    ContextCustomizerKey(Class<?> testClass) {
      MergedAnnotations annotations = MergedAnnotations.search(MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
              .withAnnotationFilter(this::isFilteredAnnotation)
              .from(testClass);
      Set<Object> determinedImports = determineImports(annotations, testClass);
      if (determinedImports == null) {
        this.key = Collections.unmodifiableSet(synthesize(annotations));
      }
      else {
        HashSet<Object> key = new HashSet<>(determinedImports);
        annotations.stream()
                .filter(annotation -> annotation.getType().equals(ComponentScan.class))
                .map(MergedAnnotation::synthesize)
                .forEach(key::add);
        this.key = Collections.unmodifiableSet(key);
      }
    }

    private boolean isFilteredAnnotation(String typeName) {
      return ANNOTATION_FILTERS.stream().anyMatch(filter -> filter.matches(typeName));
    }

    @Nullable
    private Set<Object> determineImports(MergedAnnotations annotations, Class<?> testClass) {
      Set<Object> determinedImports = new LinkedHashSet<>();
      AnnotationMetadata metadata = AnnotationMetadata.introspect(testClass);
      for (MergedAnnotation<Import> annotation : annotations.stream(Import.class).toList()) {
        for (Class<?> source : annotation.getClassArray(MergedAnnotation.VALUE)) {
          Set<Object> determinedSourceImports = determineImports(source, metadata);
          if (determinedSourceImports == null) {
            return null;
          }
          determinedImports.addAll(determinedSourceImports);
        }
      }
      return determinedImports;
    }

    @Nullable
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

    private Set<Object> synthesize(MergedAnnotations annotations) {
      return annotations.stream().map(MergedAnnotation::synthesize).collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> source) {
      try {
        Constructor<?> constructor = source.getDeclaredConstructor();
        ReflectionUtils.makeAccessible(constructor);
        return (T) constructor.newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Unable to instantiate DeterminableImportSelector "
                + source.getName(), ex);
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

}
