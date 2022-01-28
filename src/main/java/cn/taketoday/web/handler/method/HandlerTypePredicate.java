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

package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@code Predicate} to match request handling component types if
 * <strong>any</strong> of the following selectors match:
 * <ul>
 * <li>Base packages -- for selecting handlers by their package.
 * <li>Assignable types -- for selecting handlers by super type.
 * <li>Annotations -- for selecting handlers annotated in a specific way.
 * </ul>
 * <p>Composability methods on {@link Predicate} can be used :
 * <pre class="code">
 * Predicate&lt;Class&lt;?&gt;&gt; predicate =
 * 		HandlerTypePredicate.forAnnotation(RestController.class)
 * 				.and(HandlerTypePredicate.forBasePackage("org.example"));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 21:46
 */
public final class HandlerTypePredicate implements Predicate<Class<?>> {

  private final Set<String> basePackages;
  private final List<Class<?>> assignableTypes;
  private final List<Class<? extends Annotation>> annotations;

  /**
   * Private constructor. See static factory methods.
   */
  private HandlerTypePredicate(
          Set<String> basePackages,
          List<Class<?>> assignableTypes,
          List<Class<? extends Annotation>> annotations) {
    this.basePackages = Collections.unmodifiableSet(basePackages);
    this.assignableTypes = Collections.unmodifiableList(assignableTypes);
    this.annotations = Collections.unmodifiableList(annotations);
  }

  @Override
  public boolean test(@Nullable Class<?> controllerType) {
    if (!hasSelectors()) {
      return true;
    }
    else if (controllerType != null) {
      for (String basePackage : this.basePackages) {
        if (controllerType.getName().startsWith(basePackage)) {
          return true;
        }
      }
      for (Class<?> clazz : this.assignableTypes) {
        if (ClassUtils.isAssignable(clazz, controllerType)) {
          return true;
        }
      }
      for (Class<? extends Annotation> annotationClass : this.annotations) {
        if (AnnotatedElementUtils.hasAnnotation(controllerType, annotationClass)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasSelectors() {
    return !this.basePackages.isEmpty() || !this.assignableTypes.isEmpty() || !this.annotations.isEmpty();
  }

  // Static factory methods

  /**
   * {@code Predicate} that applies to any handlers.
   */
  public static HandlerTypePredicate forAnyHandlerType() {
    return new HandlerTypePredicate(
            Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Match handlers declared under a base package, e.g. "org.example".
   *
   * @param packages one or more base package names
   */
  public static HandlerTypePredicate forBasePackage(String... packages) {
    return new Builder().basePackage(packages).build();
  }

  /**
   * Type-safe alternative to {@link #forBasePackage(String...)} to specify a
   * base package through a class.
   *
   * @param packageClasses one or more base package classes
   */
  public static HandlerTypePredicate forBasePackageClass(Class<?>... packageClasses) {
    return new Builder().basePackageClass(packageClasses).build();
  }

  /**
   * Match handlers that are assignable to a given type.
   *
   * @param types one or more handler super types
   */
  public static HandlerTypePredicate forAssignableType(Class<?>... types) {
    return new Builder().assignableType(types).build();
  }

  /**
   * Match handlers annotated with a specific annotation.
   *
   * @param annotations one or more annotations to check for
   */
  @SafeVarargs
  public static HandlerTypePredicate forAnnotation(Class<? extends Annotation>... annotations) {
    return new Builder().annotation(annotations).build();
  }

  /**
   * Return a builder for a {@code HandlerTypePredicate}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A {@link HandlerTypePredicate} builder.
   */
  public static class Builder {

    private final LinkedHashSet<String> basePackages = new LinkedHashSet<>();

    private final ArrayList<Class<?>> assignableTypes = new ArrayList<>();

    private final ArrayList<Class<? extends Annotation>> annotations = new ArrayList<>();

    /**
     * Match handlers declared under a base package, e.g. "org.example".
     *
     * @param packages one or more base package classes
     */
    public Builder basePackage(String... packages) {
      for (String aPackage : packages) {
        if (StringUtils.hasText(aPackage)) {
          addBasePackage(aPackage);
        }
      }
      return this;
    }

    /**
     * Type-safe alternative to {@link #forBasePackage(String...)} to specify a
     * base package through a class.
     *
     * @param packageClasses one or more base package names
     */
    public Builder basePackageClass(Class<?>... packageClasses) {
      for (Class<?> packageClass : packageClasses) {
        addBasePackage(ClassUtils.getPackageName(packageClass));
      }
      return this;
    }

    private void addBasePackage(String basePackage) {
      this.basePackages.add(basePackage.endsWith(".") ? basePackage : basePackage + ".");
    }

    /**
     * Match handlers that are assignable to a given type.
     *
     * @param types one or more handler super types
     */
    public Builder assignableType(@Nullable Class<?>... types) {
      CollectionUtils.addAll(this.assignableTypes, types);
      return this;
    }

    /**
     * Match types that are annotated with one of the given annotations.
     *
     * @param annotations one or more annotations to check for
     */
    @SuppressWarnings("unchecked")
    public final Builder annotation(@Nullable Class<? extends Annotation>... annotations) {
      CollectionUtils.addAll(this.annotations, annotations);
      return this;
    }

    public HandlerTypePredicate build() {
      return new HandlerTypePredicate(this.basePackages, this.assignableTypes, this.annotations);
    }
  }

}

