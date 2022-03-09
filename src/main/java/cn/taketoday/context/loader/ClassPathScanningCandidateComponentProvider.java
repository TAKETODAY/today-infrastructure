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

package cn.taketoday.context.loader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.annotation.Lookup;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.annotation.ConditionEvaluator;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.ScannedGenericBeanDefinition;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Indexed;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Repository;
import cn.taketoday.lang.Service;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * A component provider that provides candidate components from a base package. Can
 * use {@link CandidateComponentsIndex the index} if it is available of scans the
 * classpath otherwise. Candidate components are identified by applying exclude and
 * include filters. {@link AnnotationTypeFilter}, {@link AssignableTypeFilter} include
 * filters on an annotation/superclass that are annotated with {@link Indexed} are
 * supported: if any other include filter is specified, the index is ignored and
 * classpath scanning is used instead.
 *
 * <p>This implementation is based on framework 's
 * {@link MetadataReader MetadataReader}
 * facility, backed by an ASM {@link ClassReader ClassReader}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/9 21:33
 */
public class ClassPathScanningCandidateComponentProvider
        extends ClassPathScanningComponentProvider implements EnvironmentCapable {
  private static final Logger log = LoggerFactory.getLogger(ClassPathScanningCandidateComponentProvider.class);

  private final ArrayList<TypeFilter> includeFilters = new ArrayList<>();
  private final ArrayList<TypeFilter> excludeFilters = new ArrayList<>();

  @Nullable
  private Environment environment;

  @Nullable
  private ConditionEvaluator conditionEvaluator;

  @Nullable
  private CandidateComponentsIndex componentsIndex;

  private Predicate<AnnotationMetadata> candidateComponentPredicate = this::isCandidateComponent;

  public ClassPathScanningCandidateComponentProvider() { }

  /**
   * Create a ClassPathScanningCandidateComponentProvider with a {@link StandardEnvironment}.
   *
   * @param useDefaultFilters whether to register the default filters for the
   * {@link Component @Component}, {@link Repository @Repository},
   * {@link Service @Service}, and {@link cn.taketoday.web.annotation.Controller @Controller}
   * stereotype annotations
   * @see #registerDefaultFilters()
   */
  public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters) {
    this(useDefaultFilters, new StandardEnvironment());
  }

  /**
   * Create a ClassPathScanningCandidateComponentProvider with the given {@link Environment}.
   *
   * @param useDefaultFilters whether to register the default filters for the
   * {@link Component @Component}, {@link Repository @Repository},
   * {@link Service @Service}, and {@link cn.taketoday.web.annotation.Controller @Controller}
   * stereotype annotations
   * @param environment the Environment to use
   * @see #registerDefaultFilters()
   */
  public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters, Environment environment) {
    if (useDefaultFilters) {
      registerDefaultFilters();
    }
    setEnvironment(environment);
    setResourceLoader(null);
  }

  /**
   * Add an include type filter to the <i>end</i> of the inclusion list.
   */
  public void addIncludeFilter(TypeFilter includeFilter) {
    this.includeFilters.add(includeFilter);
  }

  /**
   * Add an exclude type filter to the <i>front</i> of the exclusion list.
   */
  public void addExcludeFilter(TypeFilter excludeFilter) {
    this.excludeFilters.add(0, excludeFilter);
  }

  /**
   * Reset the configured type filters.
   *
   * @param useDefaultFilters whether to re-register the default filters for
   * the {@link Component @Component}, {@link Repository @Repository},
   * {@link Service @Service}, and {@link cn.taketoday.web.annotation.Controller @Controller}
   * stereotype annotations
   * @see #registerDefaultFilters()
   */
  public void resetFilters(boolean useDefaultFilters) {
    this.includeFilters.clear();
    this.excludeFilters.clear();
    if (useDefaultFilters) {
      registerDefaultFilters();
    }
  }

  /**
   * Register the default filter for {@link Component @Component}.
   * <p>This will implicitly register all annotations that have the
   * {@link Component @Component} meta-annotation including the
   * {@link Repository @Repository}, {@link Service @Service}, and
   * {@link cn.taketoday.web.annotation.Controller @Controller} stereotype annotations.
   * <p>Also supports Jakarta EE's {@link jakarta.annotation.ManagedBean} and
   * JSR-330's {@link jakarta.inject.Named} annotations, if available.
   */
  protected void registerDefaultFilters() {
    this.includeFilters.add(new AnnotationTypeFilter(Component.class));
    ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();
    try {
      this.includeFilters.add(new AnnotationTypeFilter(
              ClassUtils.forName("jakarta.annotation.ManagedBean", cl), false));
      log.trace("JSR-250 'jakarta.annotation.ManagedBean' found and supported for component scanning");
    }
    catch (ClassNotFoundException ex) {
      // JSR-250 1.1 API (as included in Jakarta EE) not available - simply skip.
    }
    try {
      this.includeFilters.add(new AnnotationTypeFilter(
              ClassUtils.forName("jakarta.inject.Named", cl), false));
      log.trace("JSR-330 'jakarta.inject.Named' annotation found and supported for component scanning");
    }
    catch (ClassNotFoundException ex) {
      // JSR-330 API not available - simply skip.
    }
  }

  /**
   * Set the Environment to use when resolving placeholders and evaluating
   * {@link Conditional @Conditional}-annotated component classes.
   * <p>The default is a {@link StandardEnvironment}.
   *
   * @param environment the Environment to use
   */
  public void setEnvironment(Environment environment) {
    Assert.notNull(environment, "Environment must not be null");
    this.environment = environment;
    this.conditionEvaluator = null;
  }

  @Override
  public final Environment getEnvironment() {
    if (this.environment == null) {
      this.environment = new StandardEnvironment();
    }
    return this.environment;
  }

  /**
   * Return the {@link BeanDefinitionRegistry} used by this scanner, if any.
   */
  @Nullable
  protected BeanDefinitionRegistry getRegistry() {
    return null;
  }

  /**
   * Set the {@link ResourceLoader} to use for resource locations.
   * This will typically be a {@link PatternResourceLoader} implementation.
   * <p>Default is a {@code PathMatchingResourcePatternResolver}, also capable of
   * resource pattern resolving through the {@code ResourcePatternResolver} interface.
   *
   * @see cn.taketoday.core.io.PatternResourceLoader
   * @see cn.taketoday.core.io.PathMatchingPatternResourceLoader
   */
  @Override
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    super.setResourceLoader(resourceLoader);
    this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(getResourceLoader().getClassLoader());
  }

  /**
   * Set the {@link Predicate} to use for candidate component testing.
   */
  public void setCandidateComponentPredicate(@Nullable Predicate<AnnotationMetadata> candidateComponentPredicate) {
    this.candidateComponentPredicate = candidateComponentPredicate == null ? this::isCandidateComponent : candidateComponentPredicate;
  }

  /**
   * Scan the class path for candidate components.
   *
   * @param basePackage the package to check for annotated classes
   * @return a corresponding Set of autodetected bean definitions
   */
  public Set<BeanDefinition> findCandidateComponents(String basePackage) {
    try {
      LinkedHashSet<BeanDefinition> candidates = new LinkedHashSet<>();
      scanCandidateComponents(basePackage, (metadataReader, metadataReaderFactory) -> {
        ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
        sbd.setSource(metadataReader.getResource());
        candidates.add(sbd);
      });
      return candidates;
    }
    catch (IOException ex) {
      throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
    }
  }

  /**
   * Scan the class path for candidate components.
   *
   * @param basePackage the package to check for annotated classes
   */
  public void scanCandidateComponents(
          String basePackage, MetadataReaderConsumer metadataReaderConsumer) throws IOException {
    if (componentsIndex != null && indexSupportsIncludeFilters()) {
      scanCandidateComponentsFromIndex(
              componentsIndex, basePackage, new FilteredMetadataReaderConsumer(metadataReaderConsumer));
    }
    else {
      scan(basePackage, new FilteredMetadataReaderConsumer(metadataReaderConsumer));
    }
  }

  /**
   * Determine if the index can be used by this instance.
   *
   * @return {@code true} if the index is available and the configuration of this
   * instance is supported by it, {@code false} otherwise
   */
  private boolean indexSupportsIncludeFilters() {
    for (TypeFilter includeFilter : this.includeFilters) {
      if (!indexSupportsIncludeFilter(includeFilter)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine if the specified include {@link TypeFilter} is supported by the index.
   *
   * @param filter the filter to check
   * @return whether the index supports this include filter
   * @see #extractStereotype(TypeFilter)
   */
  private boolean indexSupportsIncludeFilter(TypeFilter filter) {
    if (filter instanceof AnnotationTypeFilter) {
      Class<? extends Annotation> annotation = ((AnnotationTypeFilter) filter).getAnnotationType();
      return (AnnotationUtils.isAnnotationDeclaredLocally(Indexed.class, annotation)
              || annotation.getName().startsWith("javax."));
    }
    else if (filter instanceof AssignableTypeFilter) {
      Class<?> target = ((AssignableTypeFilter) filter).getTargetType();
      return AnnotationUtils.isAnnotationDeclaredLocally(Indexed.class, target);
    }
    return false;
  }

  /**
   * Extract the stereotype to use for the specified compatible filter.
   *
   * @param filter the filter to handle
   * @return the stereotype in the index matching this filter
   * @see #indexSupportsIncludeFilter(TypeFilter)
   */
  @Nullable
  private String extractStereotype(TypeFilter filter) {
    if (filter instanceof AnnotationTypeFilter) {
      return ((AnnotationTypeFilter) filter).getAnnotationType().getName();
    }
    if (filter instanceof AssignableTypeFilter) {
      return ((AssignableTypeFilter) filter).getTargetType().getName();
    }
    return null;
  }

  private void scanCandidateComponentsFromIndex(
          CandidateComponentsIndex index, String basePackage,
          MetadataReaderConsumer metadataReaderConsumer) throws IOException {
    HashSet<String> types = new HashSet<>();
    for (TypeFilter filter : this.includeFilters) {
      String stereotype = extractStereotype(filter);
      if (stereotype == null) {
        throw new IllegalArgumentException("Failed to extract stereotype from " + filter);
      }
      types.addAll(index.getCandidateTypes(basePackage, stereotype));
    }

    MetadataReaderFactory metadataReaderFactory = getMetadataReaderFactory();
    for (String type : types) {
      MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(type);
      metadataReaderConsumer.accept(metadataReader, metadataReaderFactory);
    }
  }

  /**
   * Resolve the specified base package into a pattern specification for
   * the package search path.
   * <p>The default implementation resolves placeholders against system properties,
   * and converts a "."-based package path to a "/"-based resource path.
   *
   * @param basePackage the base package as specified by the user
   * @return the pattern specification to be used for package searching
   */
  @Override
  protected String resolveBasePackage(String basePackage) {
    return ClassUtils.convertClassNameToResourcePath(
            getEnvironment().resolveRequiredPlaceholders(basePackage));
  }

  /**
   * Determine whether the given class does not match any exclude filter
   * and does match at least one include filter.
   *
   * @param metadataReader the ASM ClassReader for the class
   * @param factory a factory for obtaining metadata readers
   * for other classes (such as superclasses and interfaces)
   * @return whether the class qualifies as a candidate component
   */
  protected boolean isCandidateComponent(
          MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
    for (TypeFilter tf : excludeFilters) {
      if (tf.match(metadataReader, factory)) {
        return false;
      }
    }
    for (TypeFilter tf : includeFilters) {
      if (tf.match(metadataReader, factory)) {
        return isConditionMatch(metadataReader);
      }
    }
    return false;
  }

  /**
   * Determine whether the given class is a candidate component based on any
   * {@code @Conditional} annotations.
   *
   * @param metadataReader the ASM ClassReader for the class
   * @return whether the class qualifies as a candidate component
   */
  private boolean isConditionMatch(MetadataReader metadataReader) {
    if (conditionEvaluator == null) {
      this.conditionEvaluator = new ConditionEvaluator(
              environment, getResourceLoader(), getRegistry());
    }
    return conditionEvaluator.passCondition(metadataReader.getAnnotationMetadata());
  }

  /**
   * Determine whether the given metadata qualifies as candidate.
   * <p>The default implementation checks whether the class is not an interface
   * and not dependent on an enclosing class.
   * <p>Can be overridden in subclasses.
   *
   * @param metadata the metadata to check
   * @return whether the bean definition qualifies as a candidate component
   */
  protected boolean isCandidateComponent(AnnotationMetadata metadata) {
    return metadata.isIndependent() && (
            metadata.isConcrete() || (metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))
    );
  }

  // includeFilters excludeFilters Consumer
  class FilteredMetadataReaderConsumer implements MetadataReaderConsumer {
    final MetadataReaderConsumer metadataReaderConsumer;

    FilteredMetadataReaderConsumer(MetadataReaderConsumer metadataReaderConsumer) {
      this.metadataReaderConsumer = metadataReaderConsumer;
    }

    @Override
    public void accept(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
      if (isCandidateComponent(metadataReader, factory)
              && candidateComponentPredicate.test(metadataReader.getAnnotationMetadata())) {
        metadataReaderConsumer.accept(metadataReader, factory);
      }
    }
  }
}
