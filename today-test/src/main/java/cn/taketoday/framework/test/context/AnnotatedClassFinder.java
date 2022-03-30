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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.loader.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * Utility class to find a class annotated with a particular annotation in a hierarchy.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
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
    Assert.notNull(annotationType, "AnnotationType must not be null");
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
  public Class<?> findFromClass(Class<?> source) {
    Assert.notNull(source, "Source must not be null");
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
  public Class<?> findFromPackage(String source) {
    Assert.notNull(source, "Source must not be null");
    Class<?> configuration = cache.get(source);
    if (configuration == null) {
      configuration = scanPackage(source);
      cache.put(source, configuration);
    }
    return configuration;
  }

  private Class<?> scanPackage(String source) {
    while (!source.isEmpty()) {
      Set<BeanDefinition> components = this.scanner.findCandidateComponents(source);
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
