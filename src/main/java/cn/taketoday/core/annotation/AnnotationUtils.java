/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import java.util.Collection;
import java.util.List;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2021/7/28 21:15
 * @since 4.0
 */
public abstract class AnnotationUtils {
  private static final AnnotationMetaReader reader =
          TodayStrategies.getDetector().getFirst(
                  AnnotationMetaReader.class, ReflectiveAnnotationMetaReader::new);

  /**
   * Get the array of {@link Annotation} instance
   *
   * @param element
   *         annotated element
   * @param annotationClass
   *         target annotation class
   * @param implClass
   *         impl class
   *
   * @return the array of {@link Annotation} instance
   *
   * @since 2.1.1
   */
  public static <T extends Annotation> T[] getAnnotationArray(
          final AnnotatedElement element,
          final Class<T> annotationClass,
          final Class<? extends T> implClass
  ) {
    return reader.getAnnotationArray(element, annotationClass, implClass);
  }

  /**
   * Get the array of {@link Annotation} instance
   *
   * @param element
   *         annotated element
   * @param targetClass
   *         target annotation class
   *
   * @return the array of {@link Annotation} instance. If returns null
   * it indicates that no targetClass Annotations
   *
   * @since 2.1.1
   */
  @Nullable
  public static <T extends Annotation> T[] getAnnotationArray(
          final AnnotatedElement element, @Nullable final Class<T> targetClass
  ) {
    return reader.getAnnotationArray(element, targetClass);
  }

  /**
   * Get Annotation by reflect
   *
   * @param element
   *         The annotated element
   * @param annotationClass
   *         The annotation class
   * @param implClass
   *         The implementation class
   *
   * @return the {@link Collection} of {@link Annotation} instance
   *
   * @since 2.0.x
   */
  public static <A extends Annotation> List<A> getAnnotation(
          final AnnotatedElement element,
          final Class<A> annotationClass,
          final Class<? extends A> implClass
  ) {
    return reader.getAnnotation(element, annotationClass, implClass);
  }

  /**
   * Inject {@link AnnotationAttributes} by reflect
   *
   * @param source
   *         Element attributes
   * @param annotationClass
   *         Annotated class
   * @param instance
   *         target instance
   *
   * @return target instance
   *
   * @throws ReflectionException
   *         if BeanProperty not found
   * @since 2.1.5
   */
  public static <A> A injectAttributes(final AnnotationAttributes source,
                                       final Class<?> annotationClass, final A instance) {
    return reader.injectAttributes(source, annotationClass, instance);
  }

  /**
   * Get Annotation Attributes from an annotation instance
   *
   * @param annotation
   *         annotation instance
   *
   * @return {@link AnnotationAttributes}
   *
   * @since 2.1.1
   */
  public static AnnotationAttributes getAttributes(final Annotation annotation) {
    return getAttributes(annotation.annotationType(), annotation);
  }

  /**
   * Get Annotation Attributes from an annotation instance
   *
   * @param annotationType
   *         Input annotation type
   * @param annotation
   *         Input annotation
   *
   * @return {@link AnnotationAttributes} key-value
   *
   * @since 2.1.7
   */
  public static AnnotationAttributes getAttributes(
          final Class<? extends Annotation> annotationType, final Object annotation) {
    return reader.getAttributes(annotationType, annotation);
  }

  /**
   * Get Annotation by proxy
   *
   * @param annotatedElement
   *         The annotated element
   * @param annotationClass
   *         The annotation class
   *
   * @return the {@link Collection} of {@link Annotation} instance
   *
   * @since 2.1.1
   */
  public static <T extends Annotation> List<T> getAnnotation(
          final AnnotatedElement annotatedElement,
          final Class<T> annotationClass
  ) {
    return reader.getAnnotation(annotatedElement, annotationClass);
  }

  /**
   * Get First Annotation
   *
   * @param element
   *         The annotated element
   * @param annotationClass
   *         The annotation class
   * @param implClass
   *         the annotation' subclass
   *
   * @return the {@link Collection} of {@link Annotation} instance
   *
   * @since 2.1.7
   */
  public static <T extends Annotation> T getAnnotation(
          final Class<T> annotationClass,
          final Class<? extends T> implClass,
          final AnnotatedElement element
  ) {
    return reader.getAnnotation(annotationClass, implClass, element);
  }

  /**
   * Get First Annotation
   *
   * @param annotated
   *         The annotated element object
   * @param annotationClass
   *         The annotation class
   *
   * @return The target {@link Annotation} instance
   *
   * @since 2.1.7
   */
  public static <T extends Annotation> T getAnnotation(
          final Object annotated, final Class<T> annotationClass
  ) {
    return reader.getAnnotation(annotated, annotationClass);
  }

  /**
   * Get First Annotation
   *
   * @param annotatedElement
   *         The annotated element
   * @param annotationClass
   *         The annotation class
   *
   * @return The target {@link Annotation} instance. If annotatedElement is null returns null
   *
   * @since 2.1.7
   */
  public static <T extends Annotation> T getAnnotation(
          final Class<T> annotationClass, final AnnotatedElement annotatedElement) {
    return reader.getAnnotation(annotationClass, annotatedElement);
  }

  /**
   * Get Annotation by proxy
   *
   * @param annotationClass
   *         The annotation class
   * @param attributes
   *         The annotation attributes key-value
   *
   * @return the target {@link Annotation} instance
   *
   * @since 2.1.1
   */
  public static <T extends Annotation> T getAnnotationProxy(
          final Class<T> annotationClass, final AnnotationAttributes attributes) {
    return reader.getAnnotationProxy(annotationClass, attributes);
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @param element
   *         The annotated element
   * @param annotationClass
   *         The annotation class
   *
   * @return a set of {@link AnnotationAttributes}
   *
   * @since 2.1.1
   */
  public static <T extends Annotation> List<AnnotationAttributes> getAttributes(
          final AnnotatedElement element, final Class<T> annotationClass
  ) {
    return reader.getAttributes(element, annotationClass);
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @param element
   *         The annotated element
   * @param annotationClass
   *         The annotation class
   *
   * @return First of the {@link AnnotationAttributes} on the element
   *
   * @since 2.1.7
   */
  @Nullable
  public static <T extends Annotation> AnnotationAttributes getAttributes(
          final Class<T> annotationClass, final AnnotatedElement element
  ) {
    return reader.getAttributes(annotationClass, element);
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @param element
   *         The annotated element
   * @param targetClass
   *         The annotation class
   *
   * @return a set of {@link AnnotationAttributes} never be null
   *
   * @since 2.1.1
   */
  @NonNull
  public static <T extends Annotation> AnnotationAttributes[] getAttributesArray(
          final AnnotatedElement element, final Class<T> targetClass
  ) {
    return reader.getAttributesArray(element, targetClass);
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @return a set of {@link AnnotationAttributes} never be null
   *
   * @since 2.1.7
   */
  public static <T extends Annotation> AnnotationAttributes[] getAttributesArray(
          final AnnotationKey<T> key
  ) {
    return reader.getAttributesArray(key);
  }

  public static <T extends Annotation> List<AnnotationAttributes> getAttributes(
          final AnnotationAttributes annotation, final Class<T> target
  ) {
    return reader.searchAttributes(annotation, target);
  }

  /**
   * Get target {@link AnnotationAttributes} on input annotation
   *
   * @param target
   *         The annotation class
   * @param annotation
   *         The annotation instance
   *
   * @return {@link AnnotationAttributes} list never be null.
   *
   * @since 2.1.7
   */
  public static <T extends Annotation> List<AnnotationAttributes> getAttributes(
          final Annotation annotation, final Class<T> target
  ) {
    return reader.searchAttributes(annotation, target);
  }

  /**
   * Whether a {@link Annotation} present on {@link AnnotatedElement}
   *
   * @param <A>
   *         {@link Annotation} type
   * @param element
   *         Target {@link AnnotatedElement}
   * @param annType
   *         Target annotation type
   *
   * @return Whether it's present
   */
  public static <A extends Annotation> boolean isPresent(
          @Nullable final AnnotatedElement element, @Nullable final Class<A> annType) {
    return annType != null && element != null
            && (element.isAnnotationPresent(annType)
            || ObjectUtils.isNotEmpty(getAttributesArray(element, annType)));
  }

  /**
   * clear cache
   */
  public static void clearCache() {
    reader.clearCache();
  }

}
