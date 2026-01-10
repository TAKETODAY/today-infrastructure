/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import infra.core.annotation.AnnotatedElementUtils;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * A {@code Predicate} to match request handling component types if
 * <strong>any</strong> of the following selectors match:
 * <ul>
 * <li>Base packages -- for selecting handlers by their package.
 * <li>Assignable types -- for selecting handlers by super type.
 * <li>Annotations -- for selecting handlers annotated in a specific way.
 * </ul>
 * <p>Composability methods on {@link Predicate} can be used :
 * <pre>{@code
 * Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(RestController.class)
 *         .and(HandlerTypePredicate.forBasePackage("org.example"));
 * }</pre>
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
  private HandlerTypePredicate(Set<String> basePackages,
          List<Class<?>> assignableTypes, List<Class<? extends Annotation>> annotations) {
    this.annotations = Collections.unmodifiableList(annotations);
    this.basePackages = Collections.unmodifiableSet(basePackages);
    this.assignableTypes = Collections.unmodifiableList(assignableTypes);
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
    public Builder assignableType(Class<?> @Nullable ... types) {
      CollectionUtils.addAll(this.assignableTypes, types);
      return this;
    }

    /**
     * Match types that are annotated with one of the given annotations.
     *
     * @param annotations one or more annotations to check for
     */
    @SuppressWarnings("unchecked")
    public final Builder annotation(Class<? extends Annotation> @Nullable ... annotations) {
      CollectionUtils.addAll(this.annotations, annotations);
      return this;
    }

    public HandlerTypePredicate build() {
      return new HandlerTypePredicate(this.basePackages, this.assignableTypes, this.annotations);
    }
  }

}

