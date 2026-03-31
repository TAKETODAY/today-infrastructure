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

package infra.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import infra.beans.BeanUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;
import infra.test.context.TestContextAnnotationUtils;
import infra.util.ReflectionUtils;

import static infra.core.annotation.MergedAnnotations.SearchStrategy.DIRECT;

/**
 * Utility methods for working with bean overrides.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class BeanOverrideUtils {

  private static final Comparator<MergedAnnotation<? extends Annotation>> reversedMetaDistance =
          Comparator.<MergedAnnotation<? extends Annotation>>comparingInt(MergedAnnotation::getDistance).reversed();

  /**
   * Process the given {@code testClass} and build the corresponding
   * {@link BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
   * fields in the test class and its type hierarchy.
   * <p>This method does not search the enclosing class hierarchy and does not
   * search for {@code @BeanOverride} declarations on classes or interfaces.
   *
   * @param testClass the test class to process
   * @return a list of bean override handlers
   * @see #findAllHandlers(Class)
   */
  public static List<BeanOverrideHandler> findHandlersForFields(Class<?> testClass) {
    return findHandlers(testClass, true);
  }

  /**
   * Process the given {@code testClass} and build the corresponding
   * {@link BeanOverrideHandler} list derived from {@link BeanOverride @BeanOverride}
   * fields in the test class and in its type hierarchy as well as from
   * {@code @BeanOverride} declarations on classes and interfaces.
   * <p>This method additionally searches for {@code @BeanOverride} declarations
   * in the enclosing class hierarchy based on
   * {@link TestContextAnnotationUtils#searchEnclosingClass(Class)} semantics.
   *
   * @param testClass the test class to process
   * @return a list of bean override handlers
   * @see #findHandlersForFields(Class)
   */
  public static List<BeanOverrideHandler> findAllHandlers(Class<?> testClass) {
    return findHandlers(testClass, false);
  }

  private static List<BeanOverrideHandler> findHandlers(Class<?> testClass, boolean localFieldsOnly) {
    List<BeanOverrideHandler> handlers = new ArrayList<>();
    findHandlers(testClass, testClass, handlers, localFieldsOnly, new HashSet<>());
    return handlers;
  }

  /**
   * Find handlers using tail recursion to ensure that "locally declared" bean overrides
   * take precedence over inherited bean overrides.
   * <p>Note: the search algorithm is effectively the inverse of the algorithm used in
   * {@link infra.test.context.TestContextAnnotationUtils#findAnnotationDescriptor(Class, Class)},
   * but with tail recursion the semantics should be the same.
   *
   * @param clazz the class in/on which to search
   * @param testClass the original test class
   * @param handlers the list of handlers found
   * @param localFieldsOnly whether to search only on local fields within the type hierarchy
   * @param visitedTypes the set of types already visited
   * @since 5.0
   */
  private static void findHandlers(Class<?> clazz, Class<?> testClass, List<BeanOverrideHandler> handlers,
          boolean localFieldsOnly, Set<Class<?>> visitedTypes) {

    // 0) Ensure that we do not process the same class or interface multiple times.
    if (!visitedTypes.add(clazz)) {
      return;
    }

    // 1) Search enclosing class hierarchy.
    if (!localFieldsOnly && TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
      findHandlers(clazz.getEnclosingClass(), testClass, handlers, localFieldsOnly, visitedTypes);
    }

    // 2) Search class hierarchy.
    Class<?> superclass = clazz.getSuperclass();
    if (superclass != null && superclass != Object.class) {
      findHandlers(superclass, testClass, handlers, localFieldsOnly, visitedTypes);
    }

    if (!localFieldsOnly) {
      // 3) Search interfaces.
      for (Class<?> ifc : clazz.getInterfaces()) {
        findHandlers(ifc, testClass, handlers, localFieldsOnly, visitedTypes);
      }

      // 4) Process current class.
      processClass(clazz, testClass, handlers);
    }

    // 5) Process fields in current class.
    ReflectionUtils.doWithLocalFields(clazz, field -> processField(field, testClass, handlers));
  }

  private static void processClass(Class<?> clazz, Class<?> testClass, List<BeanOverrideHandler> handlers) {
    processElement(clazz, (processor, composedAnnotation) ->
            processor.createHandlers(composedAnnotation, testClass).forEach(handlers::add));
  }

  private static void processField(Field field, Class<?> testClass, List<BeanOverrideHandler> handlers) {
    AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
    processElement(field, (processor, composedAnnotation) -> {
      Assert.state(!Modifier.isStatic(field.getModifiers()),
              () -> "@BeanOverride field must not be static: " + field);
      Assert.state(overrideAnnotationFound.compareAndSet(false, true),
              () -> "Multiple @BeanOverride annotations found on field: " + field);
      handlers.add(processor.createHandler(composedAnnotation, testClass, field));
    });
  }

  private static void processElement(AnnotatedElement element, BiConsumer<BeanOverrideProcessor, Annotation> consumer) {
    MergedAnnotations.from(element, DIRECT)
            .stream(BeanOverride.class)
            .sorted(reversedMetaDistance)
            .forEach(mergedAnnotation -> {
              MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
              Assert.state(metaSource != null, "@BeanOverride annotation must be meta-present");

              BeanOverride beanOverride = mergedAnnotation.synthesize();
              BeanOverrideProcessor processor = BeanUtils.newInstance(beanOverride.value());
              Annotation composedAnnotation = metaSource.synthesize();
              consumer.accept(processor, composedAnnotation);
            });
  }

}
