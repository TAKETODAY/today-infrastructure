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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.annotation.MergedAnnotation.Adapt;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/7/28 21:15
 * @since 4.0
 */
public abstract class AnnotationUtils {

  /**
   * The attribute name for annotations with a single element.
   */
  public static final String VALUE = MergedAnnotation.VALUE;

  private static final AnnotationFilter JAVA_LANG_ANNOTATION_FILTER =
          AnnotationFilter.packages("java.lang.annotation");

  private static final Map<Class<? extends Annotation>, Map<String, DefaultValueHolder>> defaultValuesCache =
          new ConcurrentReferenceHashMap<>();

  /**
   * Inject {@link AnnotationAttributes} by reflect
   *
   * @param source Element attributes
   * @param annotationClass Annotated class
   * @param instance target instance
   * @return target instance
   * @throws ReflectionException if BeanProperty not found
   * @since 2.1.5
   */
  public static <A> A injectAttributes(
          AnnotationAttributes source, Class<?> annotationClass, A instance) {
    Class<?> implClass = instance.getClass();
    BeanMetadata metadata = BeanMetadata.ofClass(implClass);
    for (Method method : annotationClass.getDeclaredMethods()) {
      // method name must == field name
      String name = method.getName();
      BeanProperty beanProperty = metadata.getBeanProperty(name);
      if (beanProperty == null) {
        throw new ReflectionException(
                "You must specify a field: [" + name + "] in class: [" + implClass.getName() + "]");
      }
      beanProperty.setValue(instance, source.get(name));
    }
    return instance;
  }

  public static <A> A injectAttributes(
          MergedAnnotation<?> source, Class<?> annotationClass, A instance) {
    Class<?> implClass = instance.getClass();
    BeanMetadata metadata = BeanMetadata.ofClass(implClass);
    for (Method method : annotationClass.getDeclaredMethods()) {
      // method name must == field name
      String name = method.getName();
      BeanProperty beanProperty = metadata.getBeanProperty(name);
      if (beanProperty == null) {
        throw new ReflectionException(
                "You must specify a field: [" + name + "] in class: [" + implClass.getName() + "]");
      }
      Optional<Object> optional = source.getValue(name);
      optional.ifPresent(o -> beanProperty.setValue(instance, o));
    }
    return instance;
  }

  /**
   * Whether a {@link Annotation} present on {@link AnnotatedElement}
   *
   * @param <A> {@link Annotation} type
   * @param element Target {@link AnnotatedElement}
   * @param annType Target annotation type
   * @return Whether it's present
   */
  public static <A extends Annotation> boolean isPresent(
          @Nullable AnnotatedElement element, @Nullable Class<A> annType) {
    return AnnotatedElementUtils.isAnnotated(element, annType);
  }

  /**
   * If the supplied throwable is an {@link AnnotationConfigurationException},
   * it will be cast to an {@code AnnotationConfigurationException} and thrown,
   * allowing it to propagate to the caller.
   * <p>Otherwise, this method does nothing.
   *
   * @param ex the throwable to inspect
   */
  static void rethrowAnnotationConfigurationException(Throwable ex) {
    if (ex instanceof AnnotationConfigurationException) {
      throw (AnnotationConfigurationException) ex;
    }
  }

  /**
   * Handle the supplied annotation introspection exception.
   * <p>If the supplied exception is an {@link AnnotationConfigurationException},
   * it will simply be thrown, allowing it to propagate to the caller, and
   * nothing will be logged.
   * <p>Otherwise, this method logs an introspection failure (in particular for
   * a {@link TypeNotPresentException}) before moving on, assuming nested
   * {@code Class} values were not resolvable within annotation attributes and
   * thereby effectively pretending there were no annotations on the specified
   * element.
   *
   * @param element the element that we tried to introspect annotations on
   * @param ex the exception that we encountered
   * @see #rethrowAnnotationConfigurationException
   * @see IntrospectionFailureLogger
   */
  static void handleIntrospectionFailure(@Nullable AnnotatedElement element, Throwable ex) {
    rethrowAnnotationConfigurationException(ex);
    IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
    boolean meta = false;
    if (element instanceof Class && Annotation.class.isAssignableFrom((Class<?>) element)) {
      // Meta-annotation or (default) value lookup on an annotation type
      logger = IntrospectionFailureLogger.DEBUG;
      meta = true;
    }
    if (logger.isEnabled()) {
      String message = meta ?
                       "Failed to meta-introspect annotation " :
                       "Failed to introspect annotations on ";
      logger.log(message + element + ": " + ex);
    }
  }

  //

  /**
   * Determine whether the given class is a candidate for carrying one of the specified
   * annotations (at type, method or field level).
   *
   * @param clazz the class to introspect
   * @param annotationTypes the searchable annotation types
   * @return {@code false} if the class is known to have no such annotations at any level;
   * {@code true} otherwise. Callers will usually perform full method/field introspection
   * if {@code true} is being returned here.
   * @see #isCandidateClass(Class, Class)
   * @see #isCandidateClass(Class, String)
   */
  public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
    for (Class<? extends Annotation> annotationType : annotationTypes) {
      if (isCandidateClass(clazz, annotationType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the given class is a candidate for carrying the specified annotation
   * (at type, method or field level).
   *
   * @param clazz the class to introspect
   * @param annotationType the searchable annotation type
   * @return {@code false} if the class is known to have no such annotations at any level;
   * {@code true} otherwise. Callers will usually perform full method/field introspection
   * if {@code true} is being returned here.
   * @see #isCandidateClass(Class, String)
   */
  public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
    return isCandidateClass(clazz, annotationType.getName());
  }

  /**
   * Determine whether the given class is a candidate for carrying the specified annotation
   * (at type, method or field level).
   *
   * @param clazz the class to introspect
   * @param annotationName the fully-qualified name of the searchable annotation type
   * @return {@code false} if the class is known to have no such annotations at any level;
   * {@code true} otherwise. Callers will usually perform full method/field introspection
   * if {@code true} is being returned here.
   * @see #isCandidateClass(Class, Class)
   */
  public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
    if (annotationName.startsWith("java.")) {
      return true;
    }
    return !AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz);
  }

  /**
   * Get a single {@link Annotation} of {@code annotationType} from the supplied
   * annotation: either the given annotation itself or a direct meta-annotation
   * thereof.
   * <p>Note that this method supports only a single level of meta-annotations.
   * For support for arbitrary levels of meta-annotations, use one of the
   * {@code find*()} methods instead.
   *
   * @param annotation the Annotation to check
   * @param annotationType the annotation type to look for, both locally and as a meta-annotation
   * @return the first matching annotation, or {@code null} if not found
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <A extends Annotation> A getAnnotation(Annotation annotation, Class<A> annotationType) {
    // Shortcut: directly present on the element, with no merging needed?
    if (annotationType.isInstance(annotation)) {
      return synthesizeAnnotation((A) annotation, annotationType);
    }
    // Shortcut: no searchable annotations to be found on plain Java classes and core types...
    if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotation)) {
      return null;
    }
    // Exhaustive retrieval of merged annotations...
    return MergedAnnotations.from(annotation, new Annotation[] { annotation }, RepeatableContainers.none())
            .get(annotationType).withNonMergedAttributes()
            .synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
  }

  /**
   * Get a single {@link Annotation} of {@code annotationType} from the supplied
   * {@link AnnotatedElement}, where the annotation is either <em>present</em> or
   * <em>meta-present</em> on the {@code AnnotatedElement}.
   * <p>Note that this method supports only a single level of meta-annotations.
   * For support for arbitrary levels of meta-annotations, use
   * {@link #findAnnotation(AnnotatedElement, Class)} instead.
   *
   * @param annotatedElement the {@code AnnotatedElement} from which to get the annotation
   * @param annotationType the annotation type to look for, both locally and as a meta-annotation
   * @return the first matching annotation, or {@code null} if not found
   */
  @Nullable
  public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
    // Shortcut: directly present on the element, with no merging needed?
    if (AnnotationFilter.PLAIN.matches(annotationType)
            || AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
      return annotatedElement.getAnnotation(annotationType);
    }
    // Exhaustive retrieval of merged annotations...
    return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
            .get(annotationType).withNonMergedAttributes()
            .synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
  }

  private static <A extends Annotation> boolean isSingleLevelPresent(MergedAnnotation<A> mergedAnnotation) {
    int distance = mergedAnnotation.getDistance();
    return (distance == 0 || distance == 1);
  }

  /**
   * Get a single {@link Annotation} of {@code annotationType} from the
   * supplied {@link Method}, where the annotation is either <em>present</em>
   * or <em>meta-present</em> on the method.
   * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
   * <p>Note that this method supports only a single level of meta-annotations.
   * For support for arbitrary levels of meta-annotations, use
   * {@link #findAnnotation(Method, Class)} instead.
   *
   * @param method the method to look for annotations on
   * @param annotationType the annotation type to look for
   * @return the first matching annotation, or {@code null} if not found
   * @see cn.taketoday.core.BridgeMethodResolver#findBridgedMethod(Method)
   * @see #getAnnotation(AnnotatedElement, Class)
   */
  @Nullable
  public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationType) {
    Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
    return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
  }

  /**
   * Get all {@link Annotation Annotations} that are <em>present</em> on the
   * supplied {@link AnnotatedElement}.
   * <p>Meta-annotations will <em>not</em> be searched.
   *
   * @param annotatedElement the Method, Constructor or Field to retrieve annotations from
   * @return the annotations found, an empty array, or {@code null} if not
   * resolvable (e.g. because nested Class values in annotation attributes
   * failed to resolve at runtime)
   * @see AnnotatedElement#getAnnotations()
   */
  @Nullable
  public static Annotation[] getAnnotations(AnnotatedElement annotatedElement) {
    try {
      return synthesizeAnnotationArray(annotatedElement.getAnnotations(), annotatedElement);
    }
    catch (Throwable ex) {
      handleIntrospectionFailure(annotatedElement, ex);
      return null;
    }
  }

  /**
   * Get all {@link Annotation Annotations} that are <em>present</em> on the
   * supplied {@link Method}.
   * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
   * <p>Meta-annotations will <em>not</em> be searched.
   *
   * @param method the Method to retrieve annotations from
   * @return the annotations found, an empty array, or {@code null} if not
   * resolvable (e.g. because nested Class values in annotation attributes
   * failed to resolve at runtime)
   * @see cn.taketoday.core.BridgeMethodResolver#findBridgedMethod(Method)
   * @see AnnotatedElement#getAnnotations()
   */
  @Nullable
  public static Annotation[] getAnnotations(Method method) {
    try {
      return synthesizeAnnotationArray(BridgeMethodResolver.findBridgedMethod(method).getAnnotations(), method);
    }
    catch (Throwable ex) {
      handleIntrospectionFailure(method, ex);
      return null;
    }
  }

  /**
   * Get the <em>repeatable</em> {@linkplain Annotation annotations} of
   * {@code annotationType} from the supplied {@link AnnotatedElement}, where
   * such annotations are either <em>present</em>, <em>indirectly present</em>,
   * or <em>meta-present</em> on the element.
   * <p>This method mimics the functionality of Java 8's
   * {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}
   * with support for automatic detection of a <em>container annotation</em>
   * declared via @{@link java.lang.annotation.Repeatable} (when running on
   * Java 8 or higher) and with additional support for meta-annotations.
   * <p>Handles both single annotations and annotations nested within a
   * <em>container annotation</em>.
   * <p>Correctly handles <em>bridge methods</em> generated by the
   * compiler if the supplied element is a {@link Method}.
   * <p>Meta-annotations will be searched if the annotation is not
   * <em>present</em> on the supplied element.
   *
   * @param annotatedElement the element to look for annotations on
   * @param annotationType the annotation type to look for
   * @return the annotations found or an empty set (never {@code null})
   * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
   * @see cn.taketoday.core.BridgeMethodResolver#findBridgedMethod
   * @see java.lang.annotation.Repeatable
   * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
   */
  public static <A extends Annotation> Set<A> getRepeatableAnnotations(
          AnnotatedElement annotatedElement, Class<A> annotationType) {

    return getRepeatableAnnotations(annotatedElement, annotationType, null);
  }

  /**
   * Get the <em>repeatable</em> {@linkplain Annotation annotations} of
   * {@code annotationType} from the supplied {@link AnnotatedElement}, where
   * such annotations are either <em>present</em>, <em>indirectly present</em>,
   * or <em>meta-present</em> on the element.
   * <p>This method mimics the functionality of Java 8's
   * {@link java.lang.reflect.AnnotatedElement#getAnnotationsByType(Class)}
   * with additional support for meta-annotations.
   * <p>Handles both single annotations and annotations nested within a
   * <em>container annotation</em>.
   * <p>Correctly handles <em>bridge methods</em> generated by the
   * compiler if the supplied element is a {@link Method}.
   * <p>Meta-annotations will be searched if the annotation is not
   * <em>present</em> on the supplied element.
   *
   * @param annotatedElement the element to look for annotations on
   * @param annotationType the annotation type to look for
   * @param containerAnnotationType the type of the container that holds
   * the annotations; may be {@code null} if a container is not supported
   * or if it should be looked up via @{@link java.lang.annotation.Repeatable}
   * when running on Java 8 or higher
   * @return the annotations found or an empty set (never {@code null})
   * @see #getRepeatableAnnotations(AnnotatedElement, Class)
   * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
   * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see cn.taketoday.core.BridgeMethodResolver#findBridgedMethod
   * @see java.lang.annotation.Repeatable
   * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType
   */
  public static <A extends Annotation> Set<A> getRepeatableAnnotations(
          AnnotatedElement annotatedElement, Class<A> annotationType,
          @Nullable Class<? extends Annotation> containerAnnotationType) {

    RepeatableContainers repeatableContainers =
            containerAnnotationType != null
            ? RepeatableContainers.valueOf(annotationType, containerAnnotationType)
            : RepeatableContainers.standard();

    return MergedAnnotations.from(annotatedElement, SearchStrategy.SUPERCLASS, repeatableContainers)
            .stream(annotationType)
            .filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
            .map(MergedAnnotation::withNonMergedAttributes)
            .collect(MergedAnnotationCollectors.toAnnotationSet());
  }

  /**
   * Get the declared <em>repeatable</em> {@linkplain Annotation annotations}
   * of {@code annotationType} from the supplied {@link AnnotatedElement},
   * where such annotations are either <em>directly present</em>,
   * <em>indirectly present</em>, or <em>meta-present</em> on the element.
   * <p>This method mimics the functionality of Java 8's
   * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)}
   * with support for automatic detection of a <em>container annotation</em>
   * declared via @{@link java.lang.annotation.Repeatable} (when running on
   * Java 8 or higher) and with additional support for meta-annotations.
   * <p>Handles both single annotations and annotations nested within a
   * <em>container annotation</em>.
   * <p>Correctly handles <em>bridge methods</em> generated by the
   * compiler if the supplied element is a {@link Method}.
   * <p>Meta-annotations will be searched if the annotation is not
   * <em>present</em> on the supplied element.
   *
   * @param annotatedElement the element to look for annotations on
   * @param annotationType the annotation type to look for
   * @return the annotations found or an empty set (never {@code null})
   * @see #getRepeatableAnnotations(AnnotatedElement, Class)
   * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class)
   * @see cn.taketoday.core.BridgeMethodResolver#findBridgedMethod
   * @see java.lang.annotation.Repeatable
   * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
   */
  public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(
          AnnotatedElement annotatedElement, Class<A> annotationType) {
    return getDeclaredRepeatableAnnotations(annotatedElement, annotationType, null);
  }

  /**
   * Get the declared <em>repeatable</em> {@linkplain Annotation annotations}
   * of {@code annotationType} from the supplied {@link AnnotatedElement},
   * where such annotations are either <em>directly present</em>,
   * <em>indirectly present</em>, or <em>meta-present</em> on the element.
   * <p>This method mimics the functionality of Java 8's
   * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(Class)}
   * with additional support for meta-annotations.
   * <p>Handles both single annotations and annotations nested within a
   * <em>container annotation</em>.
   * <p>Correctly handles <em>bridge methods</em> generated by the
   * compiler if the supplied element is a {@link Method}.
   * <p>Meta-annotations will be searched if the annotation is not
   * <em>present</em> on the supplied element.
   *
   * @param annotatedElement the element to look for annotations on
   * @param annotationType the annotation type to look for
   * @param containerAnnotationType the type of the container that holds
   * the annotations; may be {@code null} if a container is not supported
   * or if it should be looked up via @{@link java.lang.annotation.Repeatable}
   * when running on Java 8 or higher
   * @return the annotations found or an empty set (never {@code null})
   * @see #getRepeatableAnnotations(AnnotatedElement, Class)
   * @see #getRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see #getDeclaredRepeatableAnnotations(AnnotatedElement, Class)
   * @see AnnotatedElementUtils#getMergedRepeatableAnnotations(AnnotatedElement, Class, Class)
   * @see cn.taketoday.core.BridgeMethodResolver#findBridgedMethod
   * @see java.lang.annotation.Repeatable
   * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType
   */
  public static <A extends Annotation> Set<A> getDeclaredRepeatableAnnotations(
          AnnotatedElement annotatedElement, Class<A> annotationType, @Nullable Class<? extends Annotation> containerAnnotationType) {
    RepeatableContainers repeatableContainers =
            containerAnnotationType != null
            ? RepeatableContainers.valueOf(annotationType, containerAnnotationType)
            : RepeatableContainers.standard();

    return MergedAnnotations.from(annotatedElement, SearchStrategy.DIRECT, repeatableContainers)
            .stream(annotationType)
            .map(MergedAnnotation::withNonMergedAttributes)
            .collect(MergedAnnotationCollectors.toAnnotationSet());
  }

  /**
   * Find a single {@link Annotation} of {@code annotationType} on the
   * supplied {@link AnnotatedElement}.
   * <p>Meta-annotations will be searched if the annotation is not
   * <em>directly present</em> on the supplied element.
   * <p><strong>Warning</strong>: this method operates generically on
   * annotated elements. In other words, this method does not execute
   * specialized search algorithms for classes or methods. If you require
   * the more specific semantics of {@link #findAnnotation(Class, Class)}
   * or {@link #findAnnotation(Method, Class)}, invoke one of those methods
   * instead.
   *
   * @param annotatedElement the {@code AnnotatedElement} on which to find the annotation
   * @param annotationType the annotation type to look for, both locally and as a meta-annotation
   * @return the first matching annotation, or {@code null} if not found
   */
  @Nullable
  public static <A extends Annotation> A findAnnotation(
          AnnotatedElement annotatedElement, @Nullable Class<A> annotationType) {

    if (annotationType == null) {
      return null;
    }

    // Shortcut: directly present on the element, with no merging needed?
    if (AnnotationFilter.PLAIN.matches(annotationType)
            || AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
      return annotatedElement.getDeclaredAnnotation(annotationType);
    }

    // Exhaustive retrieval of merged annotations...
    return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
            .get(annotationType).withNonMergedAttributes()
            .synthesize(MergedAnnotation::isPresent).orElse(null);
  }

  /**
   * Find a single {@link Annotation} of {@code annotationType} on the supplied
   * {@link Method}, traversing its super methods (i.e. from superclasses and
   * interfaces) if the annotation is not <em>directly present</em> on the given
   * method itself.
   * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
   * <p>Meta-annotations will be searched if the annotation is not
   * <em>directly present</em> on the method.
   * <p>Annotations on methods are not inherited by default, so we need to handle
   * this explicitly.
   *
   * @param method the method to look for annotations on
   * @param annotationType the annotation type to look for
   * @return the first matching annotation, or {@code null} if not found
   * @see #getAnnotation(Method, Class)
   */
  @Nullable
  public static <A extends Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
    if (annotationType == null) {
      return null;
    }

    // Shortcut: directly present on the element, with no merging needed?
    if (AnnotationFilter.PLAIN.matches(annotationType)
            || AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
      return method.getDeclaredAnnotation(annotationType);
    }

    // Exhaustive retrieval of merged annotations...
    return MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
            .get(annotationType).withNonMergedAttributes()
            .synthesize(MergedAnnotation::isPresent).orElse(null);
  }

  /**
   * Find a single {@link Annotation} of {@code annotationType} on the
   * supplied {@link Class}, traversing its interfaces, annotations, and
   * superclasses if the annotation is not <em>directly present</em> on
   * the given class itself.
   * <p>This method explicitly handles class-level annotations which are not
   * declared as {@link java.lang.annotation.Inherited inherited} <em>as well
   * as meta-annotations and annotations on interfaces</em>.
   * <p>The algorithm operates as follows:
   * <ol>
   * <li>Search for the annotation on the given class and return it if found.
   * <li>Recursively search through all annotations that the given class declares.
   * <li>Recursively search through all interfaces that the given class declares.
   * <li>Recursively search through the superclass hierarchy of the given class.
   * </ol>
   * <p>Note: in this context, the term <em>recursively</em> means that the search
   * process continues by returning to step #1 with the current interface,
   * annotation, or superclass as the class to look for annotations on.
   *
   * @param clazz the class to look for annotations on
   * @param annotationType the type of annotation to look for
   * @return the first matching annotation, or {@code null} if not found
   */
  @Nullable
  public static <A extends Annotation> A findAnnotation(Class<?> clazz, @Nullable Class<A> annotationType) {
    if (annotationType == null) {
      return null;
    }

    // Shortcut: directly present on the element, with no merging needed?
    if (AnnotationFilter.PLAIN.matches(annotationType)
            || AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
      A annotation = clazz.getDeclaredAnnotation(annotationType);
      if (annotation != null) {
        return annotation;
      }
      // For backwards compatibility, perform a superclass search with plain annotations
      // even if not marked as @Inherited: e.g. a findAnnotation search for @Deprecated
      Class<?> superclass = clazz.getSuperclass();
      if (superclass == null || superclass == Object.class) {
        return null;
      }
      return findAnnotation(superclass, annotationType);
    }

    // Exhaustive retrieval of merged annotations...
    return MergedAnnotations.from(clazz, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
            .get(annotationType).withNonMergedAttributes()
            .synthesize(MergedAnnotation::isPresent).orElse(null);
  }

  /**
   * Find the first {@link Class} in the inheritance hierarchy of the
   * specified {@code clazz} (including the specified {@code clazz} itself)
   * on which an annotation of the specified {@code annotationType} is
   * <em>directly present</em>.
   * <p>If the supplied {@code clazz} is an interface, only the interface
   * itself will be checked; the inheritance hierarchy for interfaces will
   * not be traversed.
   * <p>Meta-annotations will <em>not</em> be searched.
   * <p>The standard {@link Class} API does not provide a mechanism for
   * determining which class in an inheritance hierarchy actually declares
   * an {@link Annotation}, so we need to handle this explicitly.
   *
   * @param annotationType the annotation type to look for
   * @param clazz the class to check for the annotation on (may be {@code null})
   * @return the first {@link Class} in the inheritance hierarchy that
   * declares an annotation of the specified {@code annotationType},
   * or {@code null} if not found
   * @see Class#isAnnotationPresent(Class)
   * @see Class#getDeclaredAnnotations()
   */
  @Nullable
  public static Class<?> findAnnotationDeclaringClass(
          Class<? extends Annotation> annotationType, @Nullable Class<?> clazz) {

    if (clazz == null) {
      return null;
    }

    return (Class<?>) MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS)
            .get(annotationType, MergedAnnotation::isDirectlyPresent)
            .getSource();
  }

  /**
   * Find the first {@link Class} in the inheritance hierarchy of the
   * specified {@code clazz} (including the specified {@code clazz} itself)
   * on which at least one of the specified {@code annotationTypes} is
   * <em>directly present</em>.
   * <p>If the supplied {@code clazz} is an interface, only the interface
   * itself will be checked; the inheritance hierarchy for interfaces will
   * not be traversed.
   * <p>Meta-annotations will <em>not</em> be searched.
   * <p>The standard {@link Class} API does not provide a mechanism for
   * determining which class in an inheritance hierarchy actually declares
   * one of several candidate {@linkplain Annotation annotations}, so we
   * need to handle this explicitly.
   *
   * @param annotationTypes the annotation types to look for
   * @param clazz the class to check for the annotation on (may be {@code null})
   * @return the first {@link Class} in the inheritance hierarchy that
   * declares an annotation of at least one of the specified
   * {@code annotationTypes}, or {@code null} if not found
   * @see Class#isAnnotationPresent(Class)
   * @see Class#getDeclaredAnnotations()
   */
  @Nullable
  public static Class<?> findAnnotationDeclaringClassForTypes(
          List<Class<? extends Annotation>> annotationTypes, @Nullable Class<?> clazz) {

    if (clazz == null) {
      return null;
    }

    return (Class<?>) MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS)
            .stream()
            .filter(MergedAnnotationPredicates.typeIn(annotationTypes).and(MergedAnnotation::isDirectlyPresent))
            .map(MergedAnnotation::getSource)
            .findFirst().orElse(null);
  }

  /**
   * Determine whether an annotation of the specified {@code annotationType}
   * is declared locally (i.e. <em>directly present</em>) on the supplied
   * {@code clazz}.
   * <p>The supplied {@link Class} may represent any type.
   * <p>Meta-annotations will <em>not</em> be searched.
   * <p>Note: This method does <strong>not</strong> determine if the annotation
   * is {@linkplain java.lang.annotation.Inherited inherited}.
   *
   * @param annotationType the annotation type to look for
   * @param clazz the class to check for the annotation on
   * @return {@code true} if an annotation of the specified {@code annotationType}
   * is <em>directly present</em>
   * @see java.lang.Class#getDeclaredAnnotations()
   * @see java.lang.Class#getDeclaredAnnotation(Class)
   */
  public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
    return MergedAnnotations.from(clazz).get(annotationType).isDirectlyPresent();
  }

  /**
   * Determine whether an annotation of the specified {@code annotationType}
   * is <em>present</em> on the supplied {@code clazz} and is
   * {@linkplain java.lang.annotation.Inherited inherited}
   * (i.e. not <em>directly present</em>).
   * <p>Meta-annotations will <em>not</em> be searched.
   * <p>If the supplied {@code clazz} is an interface, only the interface
   * itself will be checked. In accordance with standard meta-annotation
   * semantics in Java, the inheritance hierarchy for interfaces will not
   * be traversed. See the {@linkplain java.lang.annotation.Inherited javadoc}
   * for the {@code @Inherited} meta-annotation for further details regarding
   * annotation inheritance.
   *
   * @param annotationType the annotation type to look for
   * @param clazz the class to check for the annotation on
   * @return {@code true} if an annotation of the specified {@code annotationType}
   * is <em>present</em> and <em>inherited</em>
   * @see Class#isAnnotationPresent(Class)
   * @see #isAnnotationDeclaredLocally(Class, Class)
   */
  public static boolean isAnnotationInherited(Class<? extends Annotation> annotationType, Class<?> clazz) {
    return MergedAnnotations.from(clazz, SearchStrategy.INHERITED_ANNOTATIONS)
            .stream(annotationType)
            .filter(MergedAnnotation::isDirectlyPresent)
            .findFirst().orElseGet(MergedAnnotation::missing)
            .getAggregateIndex() > 0;
  }

  /**
   * Determine if an annotation of type {@code metaAnnotationType} is
   * <em>meta-present</em> on the supplied {@code annotationType}.
   *
   * @param annotationType the annotation type to search on
   * @param metaAnnotationType the type of meta-annotation to search for
   * @return {@code true} if such an annotation is meta-present
   */
  public static boolean isAnnotationMetaPresent(
          Class<? extends Annotation> annotationType,
          @Nullable Class<? extends Annotation> metaAnnotationType) {
    if (metaAnnotationType == null) {
      return false;
    }
    // Shortcut: directly present on the element, with no merging needed?
    if (AnnotationFilter.PLAIN.matches(metaAnnotationType)
            || AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotationType)) {
      return annotationType.isAnnotationPresent(metaAnnotationType);
    }
    // Exhaustive retrieval of merged annotations...
    return MergedAnnotations.from(
            annotationType, SearchStrategy.INHERITED_ANNOTATIONS,
            RepeatableContainers.none()).isPresent(metaAnnotationType);
  }

  /**
   * Determine if the supplied {@link Annotation} is defined in the core JDK
   * {@code java.lang.annotation} package.
   *
   * @param annotation the annotation to check
   * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
   */
  public static boolean isInJavaLangAnnotationPackage(@Nullable Annotation annotation) {
    return (annotation != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotation));
  }

  /**
   * Determine if the {@link Annotation} with the supplied name is defined
   * in the core JDK {@code java.lang.annotation} package.
   *
   * @param annotationType the name of the annotation type to check
   * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
   */
  public static boolean isInJavaLangAnnotationPackage(@Nullable String annotationType) {
    return (annotationType != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotationType));
  }

  /**
   * Check the declared attributes of the given annotation, in particular covering
   * Google App Engine's late arrival of {@code TypeNotPresentExceptionProxy} for
   * {@code Class} values (instead of early {@code Class.getAnnotations() failure}.
   * <p>This method not failing indicates that {@link #getAnnotationAttributes(Annotation)}
   * won't failure either (when attempted later on).
   *
   * @param annotation the annotation to validate
   * @throws IllegalStateException if a declared {@code Class} attribute could not be read
   * @see Class#getAnnotations()
   * @see #getAnnotationAttributes(Annotation)
   */
  public static void validateAnnotation(Annotation annotation) {
    AttributeMethods.forAnnotationType(annotation.annotationType()).validate(annotation);
  }

  /**
   * Retrieve the given annotation's attributes as a {@link Map}, preserving all
   * attribute types.
   * <p>Equivalent to calling {@link #getAnnotationAttributes(Annotation, boolean, boolean)}
   * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
   * set to {@code false}.
   * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
   * However, the {@code Map} signature has been preserved for binary compatibility.
   *
   * @param annotation the annotation to retrieve the attributes for
   * @return the Map of annotation attributes, with attribute names as keys and
   * corresponding attribute values as values (never {@code null})
   * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
   * @see #getAnnotationAttributes(Annotation, boolean, boolean)
   * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
   */
  public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
    return getAnnotationAttributes(null, annotation);
  }

  /**
   * Retrieve the given annotation's attributes as a {@link Map}.
   * <p>Equivalent to calling {@link #getAnnotationAttributes(Annotation, boolean, boolean)}
   * with the {@code nestedAnnotationsAsMap} parameter set to {@code false}.
   * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
   * However, the {@code Map} signature has been preserved for binary compatibility.
   *
   * @param annotation the annotation to retrieve the attributes for
   * @param classValuesAsString whether to convert Class references into Strings (for
   * compatibility with {@link cn.taketoday.core.type.AnnotationMetadata})
   * or to preserve them as Class references
   * @return the Map of annotation attributes, with attribute names as keys and
   * corresponding attribute values as values (never {@code null})
   * @see #getAnnotationAttributes(Annotation, boolean, boolean)
   */
  public static Map<String, Object> getAnnotationAttributes(
          Annotation annotation, boolean classValuesAsString) {

    return getAnnotationAttributes(annotation, classValuesAsString, false);
  }

  /**
   * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
   * <p>This method provides fully recursive annotation reading capabilities on par with
   * the reflection-based {@link cn.taketoday.core.type.StandardAnnotationMetadata}.
   *
   * @param annotation the annotation to retrieve the attributes for
   * @param classValuesAsString whether to convert Class references into Strings (for
   * compatibility with {@link cn.taketoday.core.type.AnnotationMetadata})
   * or to preserve them as Class references
   * @param nestedAnnotationsAsMap whether to convert nested annotations into
   * {@link AnnotationAttributes} maps (for compatibility with
   * {@link cn.taketoday.core.type.AnnotationMetadata}) or to preserve them as
   * {@code Annotation} instances
   * @return the annotation attributes (a specialized Map) with attribute names as keys
   * and corresponding attribute values as values (never {@code null})
   */
  public static AnnotationAttributes getAnnotationAttributes(
          Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

    return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
  }

  /**
   * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
   * <p>Equivalent to calling {@link #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)}
   * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
   * set to {@code false}.
   *
   * @param annotatedElement the element that is annotated with the supplied annotation;
   * may be {@code null} if unknown
   * @param annotation the annotation to retrieve the attributes for
   * @return the annotation attributes (a specialized Map) with attribute names as keys
   * and corresponding attribute values as values (never {@code null})
   * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
   */
  public static AnnotationAttributes getAnnotationAttributes(
          @Nullable AnnotatedElement annotatedElement, Annotation annotation) {

    return getAnnotationAttributes(annotatedElement, annotation, false, false);
  }

  /**
   * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
   * <p>This method provides fully recursive annotation reading capabilities on par with
   * the reflection-based {@link cn.taketoday.core.type.StandardAnnotationMetadata}.
   *
   * @param annotatedElement the element that is annotated with the supplied annotation;
   * may be {@code null} if unknown
   * @param annotation the annotation to retrieve the attributes for
   * @param classValuesAsString whether to convert Class references into Strings (for
   * compatibility with {@link cn.taketoday.core.type.AnnotationMetadata})
   * or to preserve them as Class references
   * @param nestedAnnotationsAsMap whether to convert nested annotations into
   * {@link AnnotationAttributes} maps (for compatibility with
   * {@link cn.taketoday.core.type.AnnotationMetadata}) or to preserve them as
   * {@code Annotation} instances
   * @return the annotation attributes (a specialized Map) with attribute names as keys
   * and corresponding attribute values as values (never {@code null})
   */
  public static AnnotationAttributes getAnnotationAttributes(
          @Nullable AnnotatedElement annotatedElement, Annotation annotation,
          boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

    Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
    return MergedAnnotation.from(annotatedElement, annotation)
            .withNonMergedAttributes()
            .asMap(mergedAnnotation -> new AnnotationAttributes(mergedAnnotation.getType(), true), adaptations);
  }

  /**
   * Register the annotation-declared default values for the given attributes,
   * if available.
   *
   * @param attributes the annotation attributes to process
   */
  public static void registerDefaultValues(AnnotationAttributes attributes) {
    Class<? extends Annotation> annotationType = attributes.annotationType();
    if (annotationType != null && Modifier.isPublic(annotationType.getModifiers())
            && !AnnotationFilter.PLAIN.matches(annotationType)) {
      Map<String, DefaultValueHolder> defaultValues = getDefaultValues(annotationType);
      defaultValues.forEach(attributes::putIfAbsent);
    }
  }

  private static Map<String, DefaultValueHolder> getDefaultValues(Class<? extends Annotation> annotationType) {
    return defaultValuesCache.computeIfAbsent(annotationType, AnnotationUtils::computeDefaultValues);
  }

  private static Map<String, DefaultValueHolder> computeDefaultValues(
          Class<? extends Annotation> annotationType) {

    AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
    if (!methods.hasDefaultValueMethod()) {
      return Collections.emptyMap();
    }
    Map<String, DefaultValueHolder> result = CollectionUtils.newLinkedHashMap(methods.size());
    if (!methods.hasNestedAnnotation()) {
      // Use simpler method if there are no nested annotations
      for (int i = 0; i < methods.size(); i++) {
        Method method = methods.get(i);
        Object defaultValue = method.getDefaultValue();
        if (defaultValue != null) {
          result.put(method.getName(), new DefaultValueHolder(defaultValue));
        }
      }
    }
    else {
      // If we have nested annotations, we need them as nested maps
      AnnotationAttributes attributes = MergedAnnotation.valueOf(annotationType)
              .asMap(annotation -> new AnnotationAttributes(annotation.getType(), true), Adapt.ANNOTATION_TO_MAP);
      for (Map.Entry<String, Object> element : attributes.entrySet()) {
        result.put(element.getKey(), new DefaultValueHolder(element.getValue()));
      }
    }
    return result;
  }

  /**
   * Post-process the supplied {@link AnnotationAttributes}, preserving nested
   * annotations as {@code Annotation} instances.
   * <p>Specifically, this method enforces <em>attribute alias</em> semantics
   * for annotation attributes that are annotated with {@link AliasFor @AliasFor}
   * and replaces default value placeholders with their original default values.
   *
   * @param annotatedElement the element that is annotated with an annotation or
   * annotation hierarchy from which the supplied attributes were created;
   * may be {@code null} if unknown
   * @param attributes the annotation attributes to post-process
   * @param classValuesAsString whether to convert Class references into Strings (for
   * compatibility with {@link cn.taketoday.core.type.AnnotationMetadata})
   * or to preserve them as Class references
   * @see #getDefaultValue(Class, String)
   */
  public static void postProcessAnnotationAttributes(
          @Nullable Object annotatedElement, @Nullable AnnotationAttributes attributes, boolean classValuesAsString) {

    if (attributes == null) {
      return;
    }
    if (!attributes.validated) {
      Class<? extends Annotation> annotationType = attributes.annotationType();
      if (annotationType == null) {
        return;
      }
      AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
      AnnotationTypeMapping.MirrorSets mirrorSets = mapping.getMirrorSets();
      int size = mirrorSets.size();
      for (int i = 0; i < size; i++) {
        AnnotationTypeMapping.MirrorSets.MirrorSet mirrorSet = mirrorSets.get(i);
        int resolved = mirrorSet.resolve(
                attributes.displayName, attributes, AnnotationUtils::getAttributeValueForMirrorResolution);
        if (resolved != -1) {
          Method attribute = mapping.getAttributes().get(resolved);
          Object value = attributes.get(attribute.getName());
          for (int j = 0; j < mirrorSet.size(); j++) {
            Method mirror = mirrorSet.get(j);
            if (mirror != attribute) {
              attributes.put(
                      mirror.getName(), adaptValue(annotatedElement, value, classValuesAsString));
            }
          }
        }
      }
    }
    for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
      String attributeName = attributeEntry.getKey();
      Object value = attributeEntry.getValue();
      if (value instanceof DefaultValueHolder) {
        value = ((DefaultValueHolder) value).defaultValue;
        attributes.put(
                attributeName, adaptValue(annotatedElement, value, classValuesAsString));
      }
    }
  }

  private static Object getAttributeValueForMirrorResolution(Method attribute, Object attributes) {
    Object result = ((AnnotationAttributes) attributes).get(attribute.getName());
    return (result instanceof DefaultValueHolder ? ((DefaultValueHolder) result).defaultValue : result);
  }

  @Nullable
  private static Object adaptValue(
          @Nullable Object annotatedElement, @Nullable Object value, boolean classValuesAsString) {

    if (classValuesAsString) {
      if (value instanceof Class) {
        return ((Class<?>) value).getName();
      }
      if (value instanceof Class[]) {
        Class<?>[] classes = (Class<?>[]) value;
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
          names[i] = classes[i].getName();
        }
        return names;
      }
    }
    if (value instanceof Annotation annotation) {
      return MergedAnnotation.from(annotatedElement, annotation).synthesize();
    }
    if (value instanceof Annotation[] annotations) {
      Annotation[] synthesized = (Annotation[]) Array.newInstance(
              annotations.getClass().getComponentType(), annotations.length);
      for (int i = 0; i < annotations.length; i++) {
        synthesized[i] = MergedAnnotation.from(annotatedElement, annotations[i]).synthesize();
      }
      return synthesized;
    }
    return value;
  }

  /**
   * Retrieve the <em>value</em> of the {@code value} attribute of a
   * single-element Annotation, given an annotation instance.
   *
   * @param annotation the annotation instance from which to retrieve the value
   * @return the attribute value, or {@code null} if not found unless the attribute
   * value cannot be retrieved due to an {@link AnnotationConfigurationException},
   * in which case such an exception will be rethrown
   * @see #getValue(Annotation, String)
   */
  @Nullable
  public static Object getValue(Annotation annotation) {
    return getValue(annotation, VALUE);
  }

  /**
   * Retrieve the <em>value</em> of a named attribute, given an annotation instance.
   *
   * @param annotation the annotation instance from which to retrieve the value
   * @param attributeName the name of the attribute value to retrieve
   * @return the attribute value, or {@code null} if not found unless the attribute
   * value cannot be retrieved due to an {@link AnnotationConfigurationException},
   * in which case such an exception will be rethrown
   * @see #getValue(Annotation)
   */
  @Nullable
  public static Object getValue(@Nullable Annotation annotation, @Nullable String attributeName) {
    if (annotation == null || !StringUtils.hasText(attributeName)) {
      return null;
    }
    try {
      Method method = annotation.annotationType().getDeclaredMethod(attributeName);
      ReflectionUtils.makeAccessible(method);
      return method.invoke(annotation);
    }
    catch (NoSuchMethodException ex) {
      return null;
    }
    catch (InvocationTargetException ex) {
      rethrowAnnotationConfigurationException(ex.getTargetException());
      throw new IllegalStateException(
              "Could not obtain value for annotation attribute '"
                      + attributeName + "' in " + annotation, ex);
    }
    catch (Throwable ex) {
      handleIntrospectionFailure(annotation.getClass(), ex);
      return null;
    }
  }

  /**
   * Retrieve the <em>default value</em> of the {@code value} attribute
   * of a single-element Annotation, given an annotation instance.
   *
   * @param annotation the annotation instance from which to retrieve the default value
   * @return the default value, or {@code null} if not found
   * @see #getDefaultValue(Annotation, String)
   */
  @Nullable
  public static Object getDefaultValue(Annotation annotation) {
    return getDefaultValue(annotation, VALUE);
  }

  /**
   * Retrieve the <em>default value</em> of a named attribute, given an annotation instance.
   *
   * @param annotation the annotation instance from which to retrieve the default value
   * @param attributeName the name of the attribute value to retrieve
   * @return the default value of the named attribute, or {@code null} if not found
   * @see #getDefaultValue(Class, String)
   */
  @Nullable
  public static Object getDefaultValue(@Nullable Annotation annotation, @Nullable String attributeName) {
    return (annotation != null ? getDefaultValue(annotation.annotationType(), attributeName) : null);
  }

  /**
   * Retrieve the <em>default value</em> of the {@code value} attribute
   * of a single-element Annotation, given the {@link Class annotation type}.
   *
   * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
   * @return the default value, or {@code null} if not found
   * @see #getDefaultValue(Class, String)
   */
  @Nullable
  public static Object getDefaultValue(Class<? extends Annotation> annotationType) {
    return getDefaultValue(annotationType, VALUE);
  }

  /**
   * Retrieve the <em>default value</em> of a named attribute, given the
   * {@link Class annotation type}.
   *
   * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
   * @param attributeName the name of the attribute value to retrieve.
   * @return the default value of the named attribute, or {@code null} if not found
   * @see #getDefaultValue(Annotation, String)
   */
  @Nullable
  public static Object getDefaultValue(
          @Nullable Class<? extends Annotation> annotationType, @Nullable String attributeName) {

    if (annotationType == null || !StringUtils.hasText(attributeName)) {
      return null;
    }
    return MergedAnnotation.valueOf(annotationType).getDefaultValue(attributeName).orElse(null);
  }

  /**
   * <em>Synthesize</em> an annotation from the supplied {@code annotation}
   * by wrapping it in a dynamic proxy that transparently enforces
   * <em>attribute alias</em> semantics for annotation attributes that are
   * annotated with {@link AliasFor @AliasFor}.
   *
   * @param annotation the annotation to synthesize
   * @param annotatedElement the element that is annotated with the supplied
   * annotation; may be {@code null} if unknown
   * @return the synthesized annotation if the supplied annotation is
   * <em>synthesizable</em>; {@code null} if the supplied annotation is
   * {@code null}; otherwise the supplied annotation unmodified
   * @throws AnnotationConfigurationException if invalid configuration of
   * {@code @AliasFor} is detected
   * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
   * @see #synthesizeAnnotation(Class)
   */
  public static <A extends Annotation> A synthesizeAnnotation(
          A annotation, @Nullable AnnotatedElement annotatedElement) {
    if (annotation instanceof SynthesizedAnnotation || AnnotationFilter.PLAIN.matches(annotation)) {
      return annotation;
    }
    return MergedAnnotation.from(annotatedElement, annotation).synthesize();
  }

  /**
   * <em>Synthesize</em> an annotation from its default attributes values.
   * <p>This method simply delegates to
   * {@link #synthesizeAnnotation(Map, Class, AnnotatedElement)},
   * supplying an empty map for the source attribute values and {@code null}
   * for the {@link AnnotatedElement}.
   *
   * @param annotationType the type of annotation to synthesize
   * @return the synthesized annotation
   * @throws IllegalArgumentException if a required attribute is missing
   * @throws AnnotationConfigurationException if invalid configuration of
   * {@code @AliasFor} is detected
   * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
   * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
   */
  public static <A extends Annotation> A synthesizeAnnotation(Class<A> annotationType) {
    return synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
  }

  /**
   * <em>Synthesize</em> an annotation from the supplied map of annotation
   * attributes by wrapping the map in a dynamic proxy that implements an
   * annotation of the specified {@code annotationType} and transparently
   * enforces <em>attribute alias</em> semantics for annotation attributes
   * that are annotated with {@link AliasFor @AliasFor}.
   * <p>The supplied map must contain a key-value pair for every attribute
   * defined in the supplied {@code annotationType} that is not aliased or
   * does not have a default value. Nested maps and nested arrays of maps
   * will be recursively synthesized into nested annotations or nested
   * arrays of annotations, respectively.
   * <p>Note that {@link AnnotationAttributes} is a specialized type of
   * {@link Map} that is an ideal candidate for this method's
   * {@code attributes} argument.
   *
   * @param attributes the map of annotation attributes to synthesize
   * @param annotationType the type of annotation to synthesize
   * @param annotatedElement the element that is annotated with the annotation
   * corresponding to the supplied attributes; may be {@code null} if unknown
   * @return the synthesized annotation
   * @throws IllegalArgumentException if a required attribute is missing or if an
   * attribute is not of the correct type
   * @throws AnnotationConfigurationException if invalid configuration of
   * {@code @AliasFor} is detected
   * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
   * @see #synthesizeAnnotation(Class)
   * @see #getAnnotationAttributes(AnnotatedElement, Annotation)
   * @see #getAnnotationAttributes(AnnotatedElement, Annotation, boolean, boolean)
   */
  public static <A extends Annotation> A synthesizeAnnotation(
          Map<String, Object> attributes, Class<A> annotationType,
          @Nullable AnnotatedElement annotatedElement) {

    try {
      return MergedAnnotation.valueOf(annotatedElement, annotationType, attributes).synthesize();
    }
    catch (NoSuchElementException | IllegalStateException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  /**
   * <em>Synthesize</em> an array of annotations from the supplied array
   * of {@code annotations} by creating a new array of the same size and
   * type and populating it with {@linkplain #synthesizeAnnotation(Annotation,
   * AnnotatedElement) synthesized} versions of the annotations from the input
   * array.
   *
   * @param annotations the array of annotations to synthesize
   * @param annotatedElement the element that is annotated with the supplied
   * array of annotations; may be {@code null} if unknown
   * @return a new array of synthesized annotations, or {@code null} if
   * the supplied array is {@code null}
   * @throws AnnotationConfigurationException if invalid configuration of
   * {@code @AliasFor} is detected
   * @see #synthesizeAnnotation(Annotation, AnnotatedElement)
   * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
   */
  static Annotation[] synthesizeAnnotationArray(Annotation[] annotations, AnnotatedElement annotatedElement) {
    if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
      return annotations;
    }
    Annotation[] synthesized = (Annotation[]) Array.newInstance(
            annotations.getClass().getComponentType(), annotations.length);
    for (int i = 0; i < annotations.length; i++) {
      synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
    }
    return synthesized;
  }

  /**
   * Clear the internal annotation metadata cache.
   */
  public static void clearCache() {
    AnnotationTypeMappings.clearCache();
    AnnotationsScanner.clearCache();
  }

  /**
   * Internal holder used to wrap default values.
   */
  private record DefaultValueHolder(Object defaultValue) {

    @Override
    public String toString() {
      return "*" + this.defaultValue;
    }
  }

}
