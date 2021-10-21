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

import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.EmptyObject;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * @author TODAY 2021/8/15 22:36
 * @since 4.0
 */
public abstract class AnnotationMetaReader {
  public static final AnnotationAttributes[] EMPTY_ANNOTATION_ATTRIBUTES = { };

  /** @since 2.1.1 */
  static final HashSet<Class<? extends Annotation>> IGNORE_ANNOTATION_CLASS = new HashSet<>();

  static final ConcurrentReferenceHashMap<AnnotationKey<?>, Object>
          ANNOTATIONS = new ConcurrentReferenceHashMap<>(128);

  static final ConcurrentReferenceHashMap<AnnotationKey<?>, AnnotationAttributes[]>
          ANNOTATION_ATTRIBUTES = new ConcurrentReferenceHashMap<>(128);

  static {
    // Add ignore annotation
    addIgnoreAnnotationClass(Target.class);
    addIgnoreAnnotationClass(Inherited.class);
    addIgnoreAnnotationClass(Retention.class);
    addIgnoreAnnotationClass(Repeatable.class);
    addIgnoreAnnotationClass(Documented.class);
  }

  public static void addIgnoreAnnotationClass(Class<? extends Annotation> annotationClass) {
    IGNORE_ANNOTATION_CLASS.add(annotationClass);
  }

  /**
   * Get the array of {@link Annotation} instance
   *
   * @param element annotated element
   * @param annotationClass target annotation class
   * @param implClass impl class
   * @return the array of {@link Annotation} instance
   * @since 2.1.1
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T[] getAnnotationArray(
          final AnnotatedElement element,
          final Class<T> annotationClass,
          final Class<? extends T> implClass
  ) {
    if (annotationClass == null) {
      return null;
    }
    final AnnotationKey<T> key = new AnnotationKey<>(element, annotationClass);
    Object ret = ANNOTATIONS.get(key);
    if (ret == null) {
      final AnnotationAttributes[] annAttributes = getAttributesArray(key);
      if (ObjectUtils.isEmpty(annAttributes)) {
        ret = EmptyObject.INSTANCE;
      }
      else {
        int i = 0;
        Assert.notNull(implClass, "Implementation class can't be null");
        ret = Array.newInstance(annotationClass, annAttributes.length);
        for (AnnotationAttributes attributes : annAttributes) {
          Array.set(ret, i++, injectAttributes(attributes, BeanUtils.newInstance(implClass)));
        }
      }
      ANNOTATIONS.put(key, ret);
    }
    return ret == EmptyObject.INSTANCE ? null : (T[]) ret;
  }

  /**
   * Get the array of {@link Annotation} instance
   *
   * @param element annotated element
   * @param targetClass target annotation class
   * @return the array of {@link Annotation} instance. If returns null
   * it indicates that no targetClass Annotations
   * @since 2.1.1
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T[] getAnnotationArray(
          final AnnotatedElement element, @Nullable final Class<T> targetClass
  ) {
    if (targetClass == null) {
      return null;
    }
    final AnnotationKey<T> key = new AnnotationKey<>(element, targetClass);
    Object ret = ANNOTATIONS.get(key);
    if (ret == null) {
      final AnnotationAttributes[] annAttributes = getAttributesArray(key);
      if (ObjectUtils.isEmpty(annAttributes)) {
        ret = EmptyObject.INSTANCE;
      }
      else {
        int i = 0;
        ret = Array.newInstance(targetClass, annAttributes.length);
        for (final AnnotationAttributes attributes : annAttributes) {
          Array.set(ret, i++, getAnnotationProxy(targetClass, attributes));
        }
      }
      ANNOTATIONS.put(key, ret);
    }
    return ret == EmptyObject.INSTANCE ? null : (T[]) ret;
  }

  /**
   * Get Annotation by reflect
   *
   * @param element The annotated element
   * @param annotationClass The annotation class
   * @param implClass The implementation class
   * @return the {@link Collection} of {@link Annotation} instance
   * @since 2.0.x
   */
  public <A extends Annotation> List<A> getAnnotation(
          final AnnotatedElement element,
          final Class<A> annotationClass,
          final Class<? extends A> implClass
  ) {
    return CollectionUtils.newArrayList(getAnnotationArray(element, annotationClass, implClass));
  }

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
  public <A> A injectAttributes(final AnnotationAttributes source,
                                final Class<?> annotationClass, final A instance) {
    final Class<?> implClass = instance.getClass();
    final BeanMetadata metadata = BeanMetadata.ofClass(implClass);
    for (final Method method : annotationClass.getDeclaredMethods()) {
      // method name must == field name
      final String name = method.getName();
      final BeanProperty beanProperty = metadata.getBeanProperty(name);
      if (beanProperty == null) {
        throw new ReflectionException(
                "You must specify a field: [" + name + "] in class: [" + implClass.getName() + "]");
      }
      beanProperty.setValue(instance, source.get(name));
    }
    return instance;
  }

  public <A> A injectAttributes(final AnnotationAttributes source, final A instance) {
    final Class<?> implClass = instance.getClass();
    final BeanMetadata metadata = BeanMetadata.ofClass(implClass);
    for (BeanProperty property : metadata) {
      // method name must == field name
      String name = property.getPropertyName();
      property.setValue(instance, source.getAttribute(name, property.getType()));
    }
    return instance;
  }

  /**
   * Get Annotation Attributes from an annotation instance
   *
   * @param annotation annotation instance
   * @return {@link AnnotationAttributes}
   * @since 2.1.1
   */
  public AnnotationAttributes getAttributes(final Annotation annotation) {
    return getAttributes(annotation.annotationType(), annotation);
  }

  /**
   * Get Annotation Attributes from an annotation instance
   *
   * @param annotationType Input annotation type
   * @param annotation Input annotation
   * @return {@link AnnotationAttributes} key-value
   * @since 2.1.7
   */
  @NonNull
  public AnnotationAttributes getAttributes(
          final Class<? extends Annotation> annotationType, final Object annotation) {
    final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(annotationType);
    final AnnotationAttributes attributes = new AnnotationAttributes(annotationType, declaredMethods.length);
    for (final Method method : declaredMethods) {
      attributes.put(method.getName(), ReflectionUtils.invokeMethod(method, annotation));
    }
    return attributes;
  }

  /**
   * Get Annotation by proxy
   *
   * @param annotatedElement The annotated element
   * @param annotationClass The annotation class
   * @return the {@link Collection} of {@link Annotation} instance
   * @since 2.1.1
   */
  @NonNull
  public <T extends Annotation> List<T> getAnnotation(
          final AnnotatedElement annotatedElement,
          final Class<T> annotationClass
  ) {
    return CollectionUtils.newArrayList(getAnnotationArray(annotatedElement, annotationClass));
  }

  /**
   * Get First Annotation
   *
   * @param element The annotated element
   * @param annotationClass The annotation class
   * @param implClass the annotation' subclass
   * @return the {@link Collection} of {@link Annotation} instance
   * @since 2.1.7
   */
  @Nullable
  public <T extends Annotation> T getAnnotation(
          final Class<T> annotationClass,
          final Class<? extends T> implClass,
          final AnnotatedElement element
  ) {
    final T[] array = getAnnotationArray(element, annotationClass, implClass);
    return CollectionUtils.firstElement(array);
  }

  /**
   * Get First Annotation
   *
   * @param annotated The annotated element object
   * @param annotationClass The annotation class
   * @return The target {@link Annotation} instance
   * @since 2.1.7
   */
  @Nullable
  public <T extends Annotation> T getAnnotation(
          final Object annotated, final Class<T> annotationClass
  ) {
    return annotated == null ? null : getAnnotation(annotationClass, annotated.getClass());
  }

  /**
   * Get First Annotation
   *
   * @param annotatedElement The annotated element
   * @param annotationClass The annotation class
   * @return The target {@link Annotation} instance. If annotatedElement is null returns null
   * @since 2.1.7
   */
  @Nullable
  public <T extends Annotation> T getAnnotation(
          final Class<T> annotationClass, final AnnotatedElement annotatedElement) {
    final T[] annotationArray = getAnnotationArray(annotatedElement, annotationClass);
    return CollectionUtils.firstElement(annotationArray);
  }

  /**
   * Get Annotation by proxy
   *
   * @param annotationClass The annotation class
   * @param attributes The annotation attributes key-value
   * @return the target {@link Annotation} instance
   * @since 2.1.1
   */
  @NonNull
  public <T extends Annotation> T getAnnotationProxy(
          final Class<T> annotationClass, final AnnotationAttributes attributes) {
    return ClassMetaReader.getAnnotation(annotationClass, attributes);
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @param element The annotated element
   * @param annotationClass The annotation class
   * @return a set of {@link AnnotationAttributes}
   * @since 2.1.1
   */
  @NonNull
  public <T extends Annotation> List<AnnotationAttributes> getAttributes(
          final AnnotatedElement element, @Nullable final Class<T> annotationClass
  ) {
    return CollectionUtils.newArrayList(getAttributesArray(element, annotationClass));
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @param element The annotated element
   * @param annotationClass The annotation class
   * @return First of the {@link AnnotationAttributes} on the element
   * @since 2.1.7
   */
  @Nullable
  public <T extends Annotation> AnnotationAttributes getAttributes(
          @Nullable final Class<T> annotationClass, final AnnotatedElement element
  ) {
    final AnnotationAttributes[] array = getAttributesArray(element, annotationClass);
    return CollectionUtils.firstElement(array);
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @param element The annotated element
   * @param targetClass The annotation class
   * @return a set of {@link AnnotationAttributes} never be null
   * @since 2.1.1
   */
  @NonNull
  public <T extends Annotation> AnnotationAttributes[] getAttributesArray(
          final AnnotatedElement element, @Nullable final Class<T> targetClass
  ) {
    if (targetClass == null) {
      return EMPTY_ANNOTATION_ATTRIBUTES;
    }
    return getAttributesArray(new AnnotationKey<>(element, targetClass));
  }

  /**
   * Get attributes the 'key-value' of annotations
   *
   * @return an array of {@link AnnotationAttributes} never be null
   * @since 2.1.7
   */
  public <T extends Annotation> AnnotationAttributes[] getAttributesArray(
          @NonNull final AnnotationKey<T> key
  ) {
    AnnotationAttributes[] ret = ANNOTATION_ATTRIBUTES.get(key);
    if (ret == null) {
      ret = createAttributesArray(key);
      ANNOTATION_ATTRIBUTES.putIfAbsent(key, ret);
    }
    return ret;
  }

  protected abstract <T extends Annotation> AnnotationAttributes[] createAttributesArray(
          @NonNull AnnotationKey<T> key);

  public <T extends Annotation> List<AnnotationAttributes> searchAttributes(
          final AnnotationAttributes annotation, final Class<T> target
  ) {
    if (annotation == null) {
      return Collections.emptyList();
    }
    final String nameToFind = annotation.annotationName();
    if (Objects.equals(target.getName(), nameToFind)) {
      // return
      return Collections.singletonList(annotation);
    }
    // filter some annotation classes
    // -----------------------------------------

    if (ignorable(annotation, null)) {
      return Collections.emptyList();
    }
    // searching for the target annotation
    // -----------------------------------------
    return doSearch(annotation, target, nameToFind);
  }

  protected abstract <T extends Annotation> List<AnnotationAttributes> doSearch(
          AnnotationAttributes annotation, Class<T> target, String nameToFind);

  public <T extends Annotation> List<AnnotationAttributes> searchAttributes(
          final Annotation annotation, final Class<T> target) {
    if (annotation == null) {
      return Collections.emptyList();
    }
    final Class<? extends Annotation> annotationType = annotation.annotationType();
    if (annotationType == target) {
      // 如果等于对象注解就直接添加
      return Collections.singletonList(getAttributes(annotationType, annotation));
    }
    // filter some annotation classes
    // -----------------------------------------
    if (IGNORE_ANNOTATION_CLASS.contains(annotationType)) {
      return Collections.emptyList();
    }

    // searching for the target annotation
    return doSearch(annotation, target, annotationType);
  }

  protected abstract <T extends Annotation> List<AnnotationAttributes> doSearch(
          Annotation annotation, Class<T> target, Class<? extends Annotation> annotationType);

  protected boolean ignorable(AnnotationAttributes current, String source) {
    final String annotationName = current.annotationName();
    if (annotationName != null) {
      if (source != null && Objects.equals(source, annotationName)) {
        return true;
      }
      for (final Class<?> aClass : IGNORE_ANNOTATION_CLASS) {
        if (annotationName.equals(aClass.getName())) {
          return true;
        }
      }
      return false;
    }
    else {
      final Class<? extends Annotation> annotationType = current.annotationType();
      if (annotationType != null) {
        return IGNORE_ANNOTATION_CLASS.contains(annotationType);
      }
    }
    return false;
  }

  /**
   * Whether a {@link Annotation} present on {@link AnnotatedElement}
   *
   * @param <A> {@link Annotation} type
   * @param element Target {@link AnnotatedElement}
   * @param annType Target annotation type
   * @return Whether it's present
   */
  public <A extends Annotation> boolean isPresent(final AnnotatedElement element, final Class<A> annType) {
    return annType != null && element != null
            && (element.isAnnotationPresent(annType)
            || ObjectUtils.isNotEmpty(getAttributesArray(element, annType)));
  }

  /**
   * clear cache
   */
  public void clearCache() {
    ANNOTATIONS.clear();
    ANNOTATION_ATTRIBUTES.clear();
  }

}
