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

package infra.app.test.context;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.context.annotation.ClassPathScanningCandidateComponentProvider;
import infra.core.type.filter.AnnotationTypeFilter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Utility class to find a class annotated with a particular annotation in a hierarchy.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class AnnotatedClassFinder {

  private static final Map<String, Class<?>> cache = Collections.synchronizedMap(new Cache(40));

  private final Class<? extends Annotation> annotationType;

  private final ClassPathScanningCandidateComponentProvider scanner;

  /**
   * Create a new instance with the {@code annotationType} to find.
   *
   * @param annotationType the annotation to find
   */
  public AnnotatedClassFinder(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "AnnotationType is required");
    this.annotationType = annotationType;
    this.scanner = new ClassPathScanningCandidateComponentProvider(false);
    this.scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
    this.scanner.setResourcePattern("*.class");
  }

  /**
   * Find the first {@link Class} that is annotated with the target annotation, starting
   * from the package defined by the given {@code source} up to the root.
   *
   * @param source the source class to use to initiate the search
   * @return the first {@link Class} annotated with the target annotation within the
   * hierarchy defined by the given {@code source} or {@code null} if none is found.
   */
  @Nullable
  public Class<?> findFromClass(Class<?> source) {
    Assert.notNull(source, "Source is required");
    return findFromPackage(ClassUtils.getPackageName(source));
  }

  /**
   * Find the first {@link Class} that is annotated with the target annotation, starting
   * from the package defined by the given {@code source} up to the root.
   *
   * @param source the source package to use to initiate the search
   * @return the first {@link Class} annotated with the target annotation within the
   * hierarchy defined by the given {@code source} or {@code null} if none is found.
   */
  @Nullable
  public Class<?> findFromPackage(String source) {
    Assert.notNull(source, "Source is required");
    Class<?> configuration = cache.get(source);
    if (configuration == null) {
      configuration = scanPackage(source);
      cache.put(source, configuration);
    }
    return configuration;
  }

  @Nullable
  private Class<?> scanPackage(String source) {
    while (!source.isEmpty()) {
      Set<AnnotatedBeanDefinition> components = this.scanner.findCandidateComponents(source);
      if (!components.isEmpty()) {
        Assert.state(components.size() == 1, () -> "Found multiple @" + this.annotationType.getSimpleName()
                + " annotated classes " + components);
        return ClassUtils.resolveClassName(components.iterator().next().getBeanClassName(), null);
      }
      source = getParentPackage(source);
    }
    return null;
  }

  private String getParentPackage(String sourcePackage) {
    int lastDot = sourcePackage.lastIndexOf('.');
    return (lastDot != -1) ? sourcePackage.substring(0, lastDot) : "";
  }

  /**
   * Cache implementation based on {@link LinkedHashMap}.
   */
  private static class Cache extends LinkedHashMap<String, Class<?>> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int maxSize;

    Cache(int maxSize) {
      super(16, 0.75f, true);
      this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Class<?>> eldest) {
      return size() > this.maxSize;
    }

  }

}
