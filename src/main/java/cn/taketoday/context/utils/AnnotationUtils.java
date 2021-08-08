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

package cn.taketoday.context.utils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.Constant;
import cn.taketoday.context.EmptyObject;
import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.BeanProperty;
import cn.taketoday.context.support.AnnotationDescriptor;
import cn.taketoday.context.support.ClassMetaReader;

/**
 * @author TODAY 2021/7/28 21:15
 * @since 4.0
 */
public abstract class AnnotationUtils {
  /** @since 2.1.1 */
  static final HashSet<Class<? extends Annotation>> IGNORE_ANNOTATION_CLASS = new HashSet<>();

  static final WeakHashMap<AnnotationKey<?>, Object> ANNOTATIONS = new WeakHashMap<>(128);
  static final WeakHashMap<AnnotationKey<?>, AnnotationAttributes[]> ANNOTATION_ATTRIBUTES = new WeakHashMap<>(128);

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
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T[] getAnnotationArray(
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
        for (final AnnotationAttributes attributes : annAttributes) {
          Array.set(ret, i++, injectAttributes(attributes, annotationClass, ClassUtils.newInstance(implClass)));
        }
      }
      ANNOTATIONS.put(key, ret);
    }
    return ret == EmptyObject.INSTANCE ? null : (T[]) ret;
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
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T[] getAnnotationArray(
          final AnnotatedElement element, final Class<T> targetClass
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
    return Arrays.asList(getAnnotationArray(element, annotationClass, implClass));
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
   * @throws ApplicationContextException
   *         if any {@link Exception} occurred
   * @since 2.1.5
   */
  public static <A> A injectAttributes(final AnnotationAttributes source,
                                       final Class<?> annotationClass, final A instance) {
    final Class<?> implClass = instance.getClass();
    final BeanMetadata metadata = BeanMetadata.ofClass(implClass);
    for (final Method method : annotationClass.getDeclaredMethods()) {
      // method name must == field name
      final String name = method.getName();
      final BeanProperty beanProperty = metadata.getBeanProperty(name);
      if (beanProperty == null) {
        throw new ApplicationContextException(
                "You must specify a field: [" + name + "] in class: [" + implClass.getName() + "]");
      }
      beanProperty.setValue(instance, source.get(name));
    }
    return instance;
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
          final Class<? extends Annotation> annotationType, final Object annotation
  ) {
    try {
      final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(annotationType);
      final AnnotationAttributes attributes = new AnnotationAttributes(annotationType, declaredMethods.length);
      for (final Method method : declaredMethods) {
        attributes.put(method.getName(), method.invoke(annotation));
      }
      return attributes;
    }
    catch (Throwable ex) {
      ex = ExceptionUtils.unwrapThrowable(ex);
      throw new ApplicationContextException("An Exception Occurred When Getting Annotation Attributes", ex);
    }
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
    return Arrays.asList(getAnnotationArray(annotatedElement, annotationClass));
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
    final T[] array = getAnnotationArray(element, annotationClass, implClass);
    return ObjectUtils.isEmpty(array) ? null : array[0];
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
    return annotated == null ? null : getAnnotation(annotationClass, annotated.getClass());
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
    final T[] annotationArray = getAnnotationArray(annotatedElement, annotationClass);
    return ObjectUtils.isEmpty(annotationArray) ? null : annotationArray[0];
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
    return ClassMetaReader.getAnnotation(annotationClass, attributes);
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
    return Arrays.asList(getAttributesArray(element, annotationClass));
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
  public static <T extends Annotation> AnnotationAttributes getAttributes(
          final Class<T> annotationClass, final AnnotatedElement element
  ) {
    final AnnotationAttributes[] array = getAttributesArray(element, annotationClass);
    return ObjectUtils.isEmpty(array) ? null : array[0];
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
  public static <T extends Annotation> AnnotationAttributes[] getAttributesArray(
          final AnnotatedElement element, final Class<T> targetClass
  ) {
    if (targetClass == null) {
      return Constant.EMPTY_ANNOTATION_ATTRIBUTES;
    }
    return getAttributesArray(new AnnotationKey<>(element, targetClass));
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
    AnnotationAttributes[] ret = ANNOTATION_ATTRIBUTES.get(key);
    if (ret == null) {
      AnnotationAttributes[] annotations = ClassMetaReader.readAnnotations(key.element);
      if (ObjectUtils.isEmpty(annotations)) {
        ret = Constant.EMPTY_ANNOTATION_ATTRIBUTES;
      }
      else {
        final Class<T> annotationClass = key.annotationClass;
        final ArrayList<AnnotationAttributes> result = new ArrayList<>(); // for the order
        for (final AnnotationAttributes annotation : annotations) {
          final List<AnnotationAttributes> attr = getAttributes(annotation, annotationClass);
          if (!attr.isEmpty()) {
            result.addAll(attr);
          }
        }
        ret = result.isEmpty()
              ? Constant.EMPTY_ANNOTATION_ATTRIBUTES
              : result.toArray(new AnnotationAttributes[result.size()]);
      }
      ANNOTATION_ATTRIBUTES.putIfAbsent(key, ret);
    }
    return ret;
  }

  public static class AnnotationKey<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int hash;
    private final Class<T> annotationClass;
    private final AnnotatedElement element;

    public AnnotationKey(AnnotatedElement element, Class<T> annotationClass) {
      Assert.notNull(element, "AnnotatedElement can't be null");
      this.element = element;
      this.annotationClass = annotationClass;
      this.hash = Objects.hash(element, annotationClass);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof AnnotationKey) {
        final AnnotationKey<?> other = (AnnotationKey<?>) obj;
        return Objects.equals(element, other.element) //
                && Objects.equals(annotationClass, other.annotationClass);
      }
      return false;
    }
  }

  public static <T extends Annotation> List<AnnotationAttributes> getAttributes(
          final AnnotationAttributes annotation, final Class<T> target
  ) {
    if (annotation == null) {
      return Collections.emptyList();
    }
    final Class<? extends Annotation> source = annotation.annotationType();
    if (source == target) {
      // return
      return Collections.singletonList(annotation);
    }
    // filter some annotation classes
    // -----------------------------------------
    if (IGNORE_ANNOTATION_CLASS.contains(source)) {
      return Collections.emptyList();
    }
    // find the default value of annotation
    // -----------------------------------------
    final ArrayList<AnnotationAttributes> ret = new ArrayList<>();

    findTargetAttributes(source, target, annotation, ret, IGNORE_ANNOTATION_CLASS);
    return ret;
  }

  static <T extends Annotation> void findTargetAttributes(
          final Class<?> source,
          final Class<T> targetType,
          final AnnotationAttributes annotation,
          final ArrayList<AnnotationAttributes> attributes,
          final Set<Class<? extends Annotation>> ignoreAnnotation
  ) {
    AnnotationAttributes[] sourceAttributes = ClassMetaReader.readAnnotations(source);
    for (final AnnotationAttributes current : sourceAttributes) {
      final Class<? extends Annotation> candidateType = current.annotationType();
      if (candidateType == source || ignoreAnnotation.contains(candidateType)) {
        continue;
      }
      if (candidateType == targetType) {// TODO 优化 匹配方式
        // found target annotation
//        AnnotationAttributes found = new AnnotationAttributes(current, candidateType);
//        found.putAll(annotation);
        AnnotationAttributes found = getAttributes(current, annotation, candidateType);
        attributes.add(found); // found it
      }
      else {
        findTargetAttributes(candidateType, targetType, annotation, attributes, ignoreAnnotation);
      }
    }
  }

  static AnnotationAttributes getAttributes(
          final AnnotationAttributes current,
          final AnnotationAttributes annotation,
          final Class<? extends Annotation> candidateType
  ) {
    AnnotationDescriptor descriptor = ClassMetaReader.readDefault(candidateType);
    Map<String, String> annotationTypes = descriptor.annotationTypes; // method-name to return-type string
    AnnotationAttributes found = new AnnotationAttributes(descriptor.defaultAttributes, candidateType);

    AnnotationDescriptor candidateDescriptor = ClassMetaReader.readDefault(current.annotationName());
    Map<String, String> candidateAnnotationTypes = candidateDescriptor.annotationTypes;
    for (final Map.Entry<String, String> entry : annotationTypes.entrySet()) {
      String method = entry.getKey(); // attribute-name
      String returnType = entry.getValue();// return-type
      Object value;
      if (Objects.equals(returnType, candidateAnnotationTypes.get(method))) {
        value = current.getAttribute(method, ClassUtils.loadClass(returnType));
      }
      else {
        value = annotation.getAttribute(method, ClassUtils.loadClass(returnType));
      }
      if (value != null) {
        found.put(method, value);
      }
    }
    return found;
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
    // find the default value of annotation
    // -----------------------------------------
    final ArrayList<AnnotationAttributes> ret = new ArrayList<>();

    findTargetAttributes(annotationType, target, ret,
                         new TransformTarget(annotation, annotationType), IGNORE_ANNOTATION_CLASS);
    return ret;
  }

  interface AnnotationAttributesTransformer {

    Object get(Method method);

    void transform(AnnotationAttributes attributes);
  }

  static final class TransformTarget implements AnnotationAttributesTransformer {

    private Method[] declaredMethods;
    private final Annotation annotation;
    private final Class<?> annotationType;

    public TransformTarget(Annotation annotation, Class<?> annotationType) {
      this.annotation = annotation;
      this.annotationType = annotationType;
    }

    @Override
    public void transform(final AnnotationAttributes target) {
      // found it and override same properties
      // -------------------------------------
      final Annotation annotation = this.annotation;
      for (final Method method : getDeclaredMethods()) {
        final Object value = target.get(method.getName());
        if (value == null || eq(method.getReturnType(), value.getClass())) {
          target.put(method.getName(), ReflectionUtils.invokeMethod(method, annotation)); // override
        }
      }
    }

    protected final Method[] getDeclaredMethods() {
      Method[] ret = this.declaredMethods;
      if (ret == null) {
        ret = ReflectionUtils.getDeclaredMethods(annotationType);
        this.declaredMethods = ret;
      }
      return ret;
    }

    @Override
    public Object get(final Method targetMethod) {
      final String name = targetMethod.getName();
      final Annotation annotation = this.annotation;
      // In general there isn't lots of Annotation Attributes
      for (final Method method : getDeclaredMethods()) {
        if (method.getName().equals(name)
                && eq(method.getReturnType(), targetMethod.getReturnType())) {
          return ReflectionUtils.invokeMethod(method, annotation);
        }
      }
      return null;
    }

    private static boolean eq(Class<?> returnType, Class<?> clazz) {
      if (returnType == clazz) {
        return true;
      }
      if (returnType.isPrimitive()) {
        switch (returnType.getName()) {//@off
          case "int" :    return clazz == Integer.class;
          case "long" :   return clazz == Long.class;
          case "byte" :   return clazz == Byte.class;
          case "char" :   return clazz == Character.class;
          case "float" :  return clazz == Float.class;
          case "double" : return clazz == Double.class;
          case "short" :  return clazz == Short.class;
          case "boolean" :return clazz == Boolean.class;
          default:        return false;
        } //@on
      }
      return false;
    }
  }

  /**
   * Use recursive to find the All target {@link AnnotationAttributes} instance
   *
   * @param targetType
   *         Target {@link Annotation} class to find
   * @param source
   *         {@link Annotation} source
   * @param attributes
   *         All suitable {@link AnnotationAttributes}
   * @param ignoreAnnotation
   *         Ignore {@link Annotation}s
   *
   * @since 2.1.7
   */
  static <T extends Annotation> void findTargetAttributes(
          final Class<?> source,
          final Class<T> targetType,
          final ArrayList<AnnotationAttributes> attributes,
          final AnnotationAttributesTransformer transformer,
          final Set<Class<? extends Annotation>> ignoreAnnotation
  ) {
    for (final Annotation current : source.getAnnotations()) {
      final Class<? extends Annotation> candidateType = current.annotationType();
      if (candidateType == source || ignoreAnnotation.contains(candidateType)) {
        continue;
      }
      if (candidateType == targetType) {
        // found target annotation
        attributes.add(getAttributes(current, candidateType, transformer)); // found it
      }
      else {
        findTargetAttributes(candidateType, targetType, attributes, transformer, ignoreAnnotation);
      }
    }
  }

  public static AnnotationAttributes getAttributes(
          final Annotation current,
          final Class<? extends Annotation> candidateType,
          final AnnotationAttributesTransformer transformer
  ) {
    final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(candidateType);
    final AnnotationAttributes target = new AnnotationAttributes(candidateType, declaredMethods.length);
    for (final Method method : declaredMethods) {
      Object value = transformer.get(method);
      if (value == null) {
        value = ReflectionUtils.invokeMethod(method, current);
      }
      target.put(method.getName(), value);
    }
    return target;
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
  public static <A extends Annotation> boolean isPresent(final AnnotatedElement element, final Class<A> annType) {
    return annType != null && element != null
            && (element.isAnnotationPresent(annType)
            || ObjectUtils.isNotEmpty(getAttributesArray(element, annType)));
  }

  /**
   * clear cache
   */
  public static void clearCache() {
    ANNOTATIONS.clear();
    ANNOTATION_ATTRIBUTES.clear();
  }

}
