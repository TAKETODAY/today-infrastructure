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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;

import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Callback interface that can be used to filter specific annotation types.
 *
 * <p>Note that the {@link MergedAnnotations} model (which this interface has been
 * designed for) always ignores lang annotations according to the {@link #PLAIN}
 * filter (for efficiency reasons). Any additional filters and even custom filter
 * implementations apply within this boundary and may only narrow further from here.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @see MergedAnnotations
 * @since 4.0
 */
@FunctionalInterface
public interface AnnotationFilter {

  /**
   * {@link AnnotationFilter} that matches annotations in the
   * {@code java.lang} and {@code cn.taketoday.lang.Nullable},
   * {@code cn.taketoday.lang.NonNull}
   * <p>This is the default filter in the {@link MergedAnnotations} model.
   */
  AnnotationFilter PLAIN = new AnnotationFilter() {

    @Override
    public boolean matches(Class<?> type) {
      return type == Nullable.class
              || type == NonNull.class
              || matches(type.getName());
    }

    @Override
    public boolean matches(String typeName) {
      return typeName.startsWith("java.lang.");
    }
  };

  /**
   * {@link AnnotationFilter} that matches annotations in the
   * {@code java} and {@code jakarta} packages and their subpackages.
   */
  AnnotationFilter JAVA = packages("java", "jakarta", "javax");

  /**
   * {@link AnnotationFilter} that always matches and can be used when no
   * relevant annotation types are expected to be present at all.
   */
  AnnotationFilter ALL = new AnnotationFilter() {
    @Override
    public boolean matches(Annotation annotation) {
      return true;
    }

    @Override
    public boolean matches(Class<?> type) {
      return true;
    }

    @Override
    public boolean matches(String typeName) {
      return true;
    }

    @Override
    public String toString() {
      return "All annotations filtered";
    }
  };

  /**
   * {@link AnnotationFilter} that never matches and can be used when no
   * filtering is needed (allowing for any annotation types to be present).
   *
   * @see #PLAIN
   */
  AnnotationFilter NONE = new AnnotationFilter() {
    @Override
    public boolean matches(Annotation annotation) {
      return false;
    }

    @Override
    public boolean matches(Class<?> type) {
      return false;
    }

    @Override
    public boolean matches(String typeName) {
      return false;
    }

    @Override
    public String toString() {
      return "No annotation filtering";
    }
  };

  /**
   * Test if the given annotation matches the filter.
   *
   * @param annotation the annotation to test
   * @return {@code true} if the annotation matches
   */
  default boolean matches(Annotation annotation) {
    return matches(annotation.annotationType());
  }

  /**
   * Test if the given type matches the filter.
   *
   * @param type the annotation type to test
   * @return {@code true} if the annotation matches
   */
  default boolean matches(Class<?> type) {
    return matches(type.getName());
  }

  /**
   * Test if the given type name matches the filter.
   *
   * @param typeName the fully qualified class name of the annotation type to test
   * @return {@code true} if the annotation matches
   */
  boolean matches(String typeName);

  /**
   * Create a new {@link AnnotationFilter} that matches annotations in the
   * specified packages.
   *
   * @param packages the annotation packages that should match
   * @return a new {@link AnnotationFilter} instance
   */
  static AnnotationFilter packages(String... packages) {
    return new PackagesAnnotationFilter(packages);
  }

}
