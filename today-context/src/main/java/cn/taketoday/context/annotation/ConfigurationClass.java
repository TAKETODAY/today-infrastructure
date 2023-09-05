/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import cn.taketoday.beans.factory.parsing.Location;
import cn.taketoday.beans.factory.parsing.Problem;
import cn.taketoday.beans.factory.parsing.ProblemReporter;
import cn.taketoday.beans.factory.support.BeanDefinitionReader;
import cn.taketoday.core.io.DescriptiveResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;

/**
 * Represents a user-defined {@link Configuration @Configuration} class.
 * <p>Includes a set of {@link Component} methods, including all such methods
 * defined in the ancestry of the class, in a 'flattened-out' manner.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ComponentMethod
 * @see ConfigurationClassParser
 * @since 4.0
 */
final class ConfigurationClass {

  public final AnnotationMetadata metadata;

  public final Resource resource;

  @Nullable
  public String beanName;

  /**
   * Return the configuration classes that imported this class,
   * or an empty Set if this configuration was not imported.
   *
   * @see #isImported()
   */
  public final LinkedHashSet<ConfigurationClass> importedBy = new LinkedHashSet<>(1);

  public final LinkedHashSet<ComponentMethod> componentMethods = new LinkedHashSet<>();

  public final LinkedHashMap<String, Class<? extends BeanDefinitionReader>> importedResources = new LinkedHashMap<>();

  public final LinkedHashMap<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars = new LinkedHashMap<>();

  public final HashSet<String> skippedComponentMethods = new HashSet<>();

  /**
   * Create a new {@link ConfigurationClass} with the given name.
   *
   * @param metadataReader reader used to parse the underlying {@link Class}
   * @param beanName must not be {@code null}
   * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
   */
  ConfigurationClass(MetadataReader metadataReader, @Nullable String beanName) {
    this.metadata = metadataReader.getAnnotationMetadata();
    this.resource = metadataReader.getResource();
    this.beanName = beanName;
  }

  /**
   * Create a new {@link ConfigurationClass} representing a class that was imported
   * using the {@link Import} annotation or automatically processed as a nested
   * configuration class (if importedBy is not {@code null}).
   *
   * @param metadataReader reader used to parse the underlying {@link Class}
   * @param importedBy the configuration class importing this one or {@code null}
   */
  ConfigurationClass(MetadataReader metadataReader, @Nullable ConfigurationClass importedBy) {
    this.metadata = metadataReader.getAnnotationMetadata();
    this.resource = metadataReader.getResource();
    this.importedBy.add(importedBy);
  }

  /**
   * Create a new {@link ConfigurationClass} with the given name.
   *
   * @param clazz the underlying {@link Class} to represent
   * @param beanName name of the {@code @Configuration} class bean
   * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
   */
  ConfigurationClass(Class<?> clazz, @Nullable String beanName) {
    this.metadata = AnnotationMetadata.introspect(clazz);
    this.resource = new DescriptiveResource(clazz.getName());
    this.beanName = beanName;
  }

  /**
   * Create a new {@link ConfigurationClass} representing a class that was imported
   * using the {@link Import} annotation or automatically processed as a nested
   * configuration class (if imported is {@code true}).
   *
   * @param clazz the underlying {@link Class} to represent
   * @param importedBy the configuration class importing this one (or {@code null})
   */
  ConfigurationClass(Class<?> clazz, @Nullable ConfigurationClass importedBy) {
    this.metadata = AnnotationMetadata.introspect(clazz);
    this.resource = new DescriptiveResource(clazz.getName());
    this.importedBy.add(importedBy);
  }

  /**
   * Create a new {@link ConfigurationClass} with the given name.
   *
   * @param metadata the metadata for the underlying class to represent
   * @param beanName name of the {@code @Configuration} class bean
   * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
   */
  ConfigurationClass(AnnotationMetadata metadata, @Nullable String beanName) {
    this.metadata = metadata;
    this.resource = new DescriptiveResource(metadata.getClassName());
    this.beanName = beanName;
  }

  String getSimpleName() {
    return ClassUtils.getShortName(metadata.getClassName());
  }

  void setBeanName(@Nullable String beanName) {
    this.beanName = beanName;
  }

  /**
   * Return whether this configuration class was registered via @{@link Import} or
   * automatically registered due to being nested within another configuration class.
   *
   * @see #importedBy
   */
  public boolean isImported() {
    return !this.importedBy.isEmpty();
  }

  /**
   * Merge the imported-by declarations from the given configuration class into this one.
   */
  void mergeImportedBy(ConfigurationClass otherConfigClass) {
    this.importedBy.addAll(otherConfigClass.importedBy);
  }

  void addMethod(ComponentMethod method) {
    this.componentMethods.add(method);
  }

  void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
    this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
  }

  void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
    this.importedResources.put(importedResource, readerClass);
  }

  void validate(ProblemReporter problemReporter) {
    // A configuration class may not be final (CGLIB limitation) unless it declares proxyBeanMethods=false
    var annotation = metadata.getAnnotation(Configuration.class);
    if (annotation.isPresent()
            && annotation.getValue("proxyBeanMethods", boolean.class).orElse(true)) {
      if (metadata.isFinal()) {
        problemReporter.error(new FinalConfigurationProblem());
      }
      for (ComponentMethod componentMethod : componentMethods) {
        componentMethod.validate(problemReporter);
      }
    }

    // A configuration class may not contain overloaded bean methods unless it declares enforceUniqueMethods=false
    if (annotation.isPresent() && annotation.getBoolean("enforceUniqueMethods")) {
      Map<String, MethodMetadata> beanMethodsByName = new LinkedHashMap<>();
      for (ComponentMethod beanMethod : componentMethods) {
        MethodMetadata current = beanMethod.metadata;
        MethodMetadata existing = beanMethodsByName.put(current.getMethodName(), current);
        if (existing != null && existing.getDeclaringClassName().equals(current.getDeclaringClassName())) {
          problemReporter.error(new BeanMethodOverloadingProblem(existing.getMethodName()));
        }
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ConfigurationClass &&
            metadata.getClassName().equals(((ConfigurationClass) other).metadata.getClassName())));
  }

  @Override
  public int hashCode() {
    return metadata.getClassName().hashCode();
  }

  @Override
  public String toString() {
    return "ConfigurationClass: beanName '" + this.beanName + "', " + this.resource;
  }

  /**
   * Configuration classes must be non-final to accommodate CGLIB subclassing.
   */
  private class FinalConfigurationProblem extends Problem {

    FinalConfigurationProblem() {
      super(String.format("@Configuration class '%s' may not be final, when proxyBeanMethods is enabled. Remove the final modifier to continue.",
              getSimpleName()), new Location(resource, metadata));
    }
  }

  /**
   * Configuration classes are not allowed to contain overloaded bean methods
   * by default
   */
  private class BeanMethodOverloadingProblem extends Problem {

    BeanMethodOverloadingProblem(String methodName) {
      super(String.format("@Configuration class '%s' contains overloaded @Bean methods with name '%s'. Use " +
                      "unique method names for separate bean definitions (with individual conditions etc) " +
                      "or switch '@Configuration.enforceUniqueMethods' to 'false'.",
              getSimpleName(), methodName), new Location(resource, metadata));
    }
  }

}
