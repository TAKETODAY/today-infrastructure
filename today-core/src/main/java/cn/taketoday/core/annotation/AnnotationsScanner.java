/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Predicate;

import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotations.Search;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Scanner to search for relevant annotations in the annotation hierarchy of an
 * {@link AnnotatedElement}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationsProcessor
 * @since 4.0
 */
abstract class AnnotationsScanner {

  private static final ConcurrentReferenceHashMap<AnnotatedElement, Annotation[]>
          declaredAnnotationCache = new ConcurrentReferenceHashMap<>(256);

  private static final ConcurrentReferenceHashMap<Class<?>, Method[]>
          baseTypeMethodsCache = new ConcurrentReferenceHashMap<>(256);

  private AnnotationsScanner() { }

  /**
   * Scan the hierarchy of the specified element for relevant annotations and
   * call the processor as required.
   *
   * @param context an optional context object that will be passed back to the
   * processor
   * @param source the source element to scan
   * @param searchStrategy the search strategy to use
   * @param searchEnclosingClass a predicate which evaluates to {@code true}
   * if a search should be performed on the enclosing class of the class
   * supplied to the predicate
   * @param processor the processor that receives the annotations
   * @return the result of {@link AnnotationsProcessor#finish(Object)}
   */
  @Nullable
  static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy,
          Predicate<Class<?>> searchEnclosingClass, AnnotationsProcessor<C, R> processor) {

    R result = process(context, source, searchStrategy, searchEnclosingClass, processor);
    return processor.finish(result);
  }

  @Nullable
  private static <C, R> R process(C context, AnnotatedElement source, SearchStrategy searchStrategy,
          Predicate<Class<?>> searchEnclosingClass, AnnotationsProcessor<C, R> processor) {

    if (source instanceof Class<?> clazz) {
      return processClass(context, clazz, searchStrategy, searchEnclosingClass, processor);
    }
    if (source instanceof Method method) {
      return processMethod(context, method, searchStrategy, processor);
    }
    return processElement(context, source, processor);
  }

  @Nullable
  private static <C, R> R processClass(C context, Class<?> source, SearchStrategy searchStrategy,
          Predicate<Class<?>> searchEnclosingClass, AnnotationsProcessor<C, R> processor) {

    return switch (searchStrategy) {
      case DIRECT -> processElement(context, source, processor);
      case INHERITED_ANNOTATIONS -> processClassInheritedAnnotations(context, source, processor);
      case SUPERCLASS -> processClassHierarchy(context, source, processor, false, Search.never);
      case TYPE_HIERARCHY -> processClassHierarchy(context, source, processor, true, searchEnclosingClass);
    };
  }

  @Nullable
  private static <C, R> R processClassInheritedAnnotations(C context, Class<?> source, AnnotationsProcessor<C, R> processor) {
    try {
      if (isWithoutHierarchy(source, Search.never)) {
        return processElement(context, source, processor);
      }
      Annotation[] relevant = null;
      int remaining = Integer.MAX_VALUE;
      int aggregateIndex = 0;
      Class<?> root = source;
      while (source != null && source != Object.class && remaining > 0 && !hasPlainJavaAnnotationsOnly(source)) {
        R result = processor.doWithAggregate(context, aggregateIndex);
        if (result != null) {
          return result;
        }
        Annotation[] declaredAnnotations = getDeclaredAnnotations(source, true);
        if (relevant == null && declaredAnnotations.length > 0) {
          relevant = root.getAnnotations();
          remaining = relevant.length;
        }
        for (int i = 0; i < declaredAnnotations.length; i++) {
          if (declaredAnnotations[i] != null) {
            boolean isRelevant = false;
            for (int relevantIndex = 0; relevantIndex < relevant.length; relevantIndex++) {
              if (relevant[relevantIndex] != null
                      && declaredAnnotations[i].annotationType() == relevant[relevantIndex].annotationType()) {
                isRelevant = true;
                relevant[relevantIndex] = null;
                remaining--;
                break;
              }
            }
            if (!isRelevant) {
              declaredAnnotations[i] = null;
            }
          }
        }
        result = processor.doWithAnnotations(context, aggregateIndex, source, declaredAnnotations);
        if (result != null) {
          return result;
        }
        source = source.getSuperclass();
        aggregateIndex++;
      }
    }
    catch (Throwable ex) {
      AnnotationUtils.handleIntrospectionFailure(source, ex);
    }
    return null;
  }

  @Nullable
  private static <C, R> R processClassHierarchy(C context, Class<?> source,
          AnnotationsProcessor<C, R> processor, boolean includeInterfaces, Predicate<Class<?>> searchEnclosingClass) {

    return processClassHierarchy(context, new int[] { 0 }, source, processor, includeInterfaces, searchEnclosingClass);
  }

  @Nullable
  private static <C, R> R processClassHierarchy(C context, int[] aggregateIndex, Class<?> source,
          AnnotationsProcessor<C, R> processor, boolean includeInterfaces, Predicate<Class<?>> searchEnclosingClass) {

    try {
      R result = processor.doWithAggregate(context, aggregateIndex[0]);
      if (result != null) {
        return result;
      }
      if (hasPlainJavaAnnotationsOnly(source)) {
        return null;
      }
      Annotation[] annotations = getDeclaredAnnotations(source, false);
      result = processor.doWithAnnotations(context, aggregateIndex[0], source, annotations);
      if (result != null) {
        return result;
      }
      aggregateIndex[0]++;
      if (includeInterfaces) {
        for (Class<?> interfaceType : source.getInterfaces()) {
          R interfacesResult = processClassHierarchy(context, aggregateIndex,
                  interfaceType, processor, true, searchEnclosingClass);
          if (interfacesResult != null) {
            return interfacesResult;
          }
        }
      }
      Class<?> superclass = source.getSuperclass();
      if (superclass != Object.class && superclass != null) {
        R superclassResult = processClassHierarchy(context, aggregateIndex,
                superclass, processor, includeInterfaces, searchEnclosingClass);
        if (superclassResult != null) {
          return superclassResult;
        }
      }
      if (searchEnclosingClass.test(source)) {
        // Since merely attempting to load the enclosing class may result in
        // automatic loading of sibling nested classes that in turn results
        // in an exception such as NoClassDefFoundError, we wrap the following
        // in its own dedicated try-catch block in order not to preemptively
        // halt the annotation scanning process.
        try {
          Class<?> enclosingClass = source.getEnclosingClass();
          if (enclosingClass != null) {
            R enclosingResult = processClassHierarchy(context, aggregateIndex,
                    enclosingClass, processor, includeInterfaces, searchEnclosingClass);
            if (enclosingResult != null) {
              return enclosingResult;
            }
          }
        }
        catch (Throwable ex) {
          AnnotationUtils.handleIntrospectionFailure(source, ex);
        }
      }
    }
    catch (Throwable ex) {
      AnnotationUtils.handleIntrospectionFailure(source, ex);
    }
    return null;
  }

  @Nullable
  private static <C, R> R processMethod(C context, Method source,
          SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor) {

    return switch (searchStrategy) {
      case DIRECT, INHERITED_ANNOTATIONS -> processMethodInheritedAnnotations(context, source, processor);
      case SUPERCLASS -> processMethodHierarchy(context, new int[] { 0 }, source.getDeclaringClass(),
              processor, source, false);
      case TYPE_HIERARCHY -> processMethodHierarchy(context, new int[] { 0 }, source.getDeclaringClass(),
              processor, source, true);
    };
  }

  @Nullable
  private static <C, R> R processMethodInheritedAnnotations(C context, Method source, AnnotationsProcessor<C, R> processor) {

    try {
      R result = processor.doWithAggregate(context, 0);
      return (result != null ? result :
              processMethodAnnotations(context, 0, source, processor));
    }
    catch (Throwable ex) {
      AnnotationUtils.handleIntrospectionFailure(source, ex);
    }
    return null;
  }

  @Nullable
  private static <C, R> R processMethodHierarchy(C context, int[] aggregateIndex,
          Class<?> sourceClass, AnnotationsProcessor<C, R> processor, Method rootMethod, boolean includeInterfaces) {

    try {
      R result = processor.doWithAggregate(context, aggregateIndex[0]);
      if (result != null) {
        return result;
      }
      if (hasPlainJavaAnnotationsOnly(sourceClass)) {
        return null;
      }
      boolean calledProcessor = false;
      if (sourceClass == rootMethod.getDeclaringClass()) {
        result = processMethodAnnotations(context, aggregateIndex[0], rootMethod, processor);
        calledProcessor = true;
        if (result != null) {
          return result;
        }
      }
      else {
        for (Method candidateMethod : getBaseTypeMethods(sourceClass)) {
          if (candidateMethod != null && isOverride(rootMethod, candidateMethod)) {
            result = processMethodAnnotations(context, aggregateIndex[0], candidateMethod, processor);
            calledProcessor = true;
            if (result != null) {
              return result;
            }
          }
        }
      }
      if (Modifier.isPrivate(rootMethod.getModifiers())) {
        return null;
      }
      if (calledProcessor) {
        aggregateIndex[0]++;
      }
      if (includeInterfaces) {
        for (Class<?> interfaceType : sourceClass.getInterfaces()) {
          R interfacesResult = processMethodHierarchy(context, aggregateIndex,
                  interfaceType, processor, rootMethod, true);
          if (interfacesResult != null) {
            return interfacesResult;
          }
        }
      }
      Class<?> superclass = sourceClass.getSuperclass();
      if (superclass != Object.class && superclass != null) {
        R superclassResult = processMethodHierarchy(context, aggregateIndex,
                superclass, processor, rootMethod, includeInterfaces);
        if (superclassResult != null) {
          return superclassResult;
        }
      }
    }
    catch (Throwable ex) {
      AnnotationUtils.handleIntrospectionFailure(rootMethod, ex);
    }
    return null;
  }

  private static Method[] getBaseTypeMethods(Class<?> baseType) {
    if (baseType == Object.class || hasPlainJavaAnnotationsOnly(baseType)) {
      return Constant.EMPTY_METHODS;
    }

    Method[] methods = baseTypeMethodsCache.get(baseType);
    if (methods == null) {
      methods = ReflectionUtils.getDeclaredMethods(baseType);
      int cleared = 0;
      for (int i = 0; i < methods.length; i++) {
        if (Modifier.isPrivate(methods[i].getModifiers())
                || hasPlainJavaAnnotationsOnly(methods[i])
                || getDeclaredAnnotations(methods[i], false).length == 0) {
          methods[i] = null;
          cleared++;
        }
      }
      if (cleared == methods.length) {
        methods = Constant.EMPTY_METHODS;
      }
      baseTypeMethodsCache.put(baseType, methods);
    }
    return methods;
  }

  private static boolean isOverride(Method rootMethod, Method candidateMethod) {
    return !Modifier.isPrivate(candidateMethod.getModifiers())
            && candidateMethod.getName().equals(rootMethod.getName())
            && hasSameParameterTypes(rootMethod, candidateMethod);
  }

  private static boolean hasSameParameterTypes(Method rootMethod, Method candidateMethod) {
    if (candidateMethod.getParameterCount() != rootMethod.getParameterCount()) {
      return false;
    }
    Class<?>[] rootParameterTypes = rootMethod.getParameterTypes();
    Class<?>[] candidateParameterTypes = candidateMethod.getParameterTypes();
    if (Arrays.equals(candidateParameterTypes, rootParameterTypes)) {
      return true;
    }
    return hasSameGenericTypeParameters(rootMethod, candidateMethod, rootParameterTypes);
  }

  private static boolean hasSameGenericTypeParameters(
          Method rootMethod, Method candidateMethod, Class<?>[] rootParameterTypes) {

    Class<?> sourceDeclaringClass = rootMethod.getDeclaringClass();
    Class<?> candidateDeclaringClass = candidateMethod.getDeclaringClass();
    if (!candidateDeclaringClass.isAssignableFrom(sourceDeclaringClass)) {
      return false;
    }
    for (int i = 0; i < rootParameterTypes.length; i++) {
      Class<?> resolvedParameterType = ResolvableType.forMethodParameter(candidateMethod, i, sourceDeclaringClass).resolve();
      if (rootParameterTypes[i] != resolvedParameterType) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  private static <C, R> R processMethodAnnotations(C context, int aggregateIndex, Method source, AnnotationsProcessor<C, R> processor) {

    Annotation[] annotations = getDeclaredAnnotations(source, false);
    R result = processor.doWithAnnotations(context, aggregateIndex, source, annotations);
    if (result != null) {
      return result;
    }
    Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(source);
    if (bridgedMethod != source) {
      Annotation[] bridgedAnnotations = getDeclaredAnnotations(bridgedMethod, true);
      for (int i = 0; i < bridgedAnnotations.length; i++) {
        if (ObjectUtils.containsElement(annotations, bridgedAnnotations[i])) {
          bridgedAnnotations[i] = null;
        }
      }
      return processor.doWithAnnotations(context, aggregateIndex, source, bridgedAnnotations);
    }
    return null;
  }

  @Nullable
  private static <C, R> R processElement(C context, AnnotatedElement source, AnnotationsProcessor<C, R> processor) {

    try {
      R result = processor.doWithAggregate(context, 0);
      return (result != null ? result : processor.doWithAnnotations(
              context, 0, source, getDeclaredAnnotations(source, false)));
    }
    catch (Throwable ex) {
      AnnotationUtils.handleIntrospectionFailure(source, ex);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement source, Class<A> annotationType) {
    Annotation[] annotations = getDeclaredAnnotations(source, false);
    for (Annotation annotation : annotations) {
      if (annotation != null && annotationType == annotation.annotationType()) {
        return (A) annotation;
      }
    }
    return null;
  }

  static Annotation[] getDeclaredAnnotations(AnnotatedElement source, boolean defensive) {
    boolean cached = false;
    Annotation[] annotations = declaredAnnotationCache.get(source);
    if (annotations != null) {
      cached = true;
    }
    else {
      annotations = source.getDeclaredAnnotations();
      if (annotations.length != 0) {
        boolean allIgnored = true;
        for (int i = 0; i < annotations.length; i++) {
          Annotation annotation = annotations[i];
          if (isIgnorable(annotation.annotationType()) ||
                  !AttributeMethods.forAnnotationType(annotation.annotationType()).isValid(annotation)) {
            annotations[i] = null;
          }
          else {
            allIgnored = false;
          }
        }
        annotations = allIgnored ? Constant.EMPTY_ANNOTATIONS : annotations;
        if (source instanceof Class || source instanceof Member) {
          declaredAnnotationCache.put(source, annotations);
          cached = true;
        }
      }
    }
    if (!defensive || annotations.length == 0 || !cached) {
      return annotations;
    }
    return annotations.clone();
  }

  private static boolean isIgnorable(Class<?> annotationType) {
    return AnnotationFilter.PLAIN.matches(annotationType);
  }

  static boolean isKnownEmpty(AnnotatedElement source, SearchStrategy searchStrategy, Predicate<Class<?>> searchEnclosingClass) {
    if (hasPlainJavaAnnotationsOnly(source)) {
      return true;
    }
    if (searchStrategy == SearchStrategy.DIRECT || isWithoutHierarchy(source, searchEnclosingClass)) {
      if (source instanceof Method method && method.isBridge()) {
        return false;
      }
      return getDeclaredAnnotations(source, false).length == 0;
    }
    return false;
  }

  static boolean hasPlainJavaAnnotationsOnly(@Nullable Object annotatedElement) {
    if (annotatedElement instanceof Class<?> clazz) {
      return hasPlainJavaAnnotationsOnly(clazz);
    }
    else if (annotatedElement instanceof Member member) {
      return hasPlainJavaAnnotationsOnly(member.getDeclaringClass());
    }
    else {
      return false;
    }
  }

  static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
    return type.getName().startsWith("java.") || type == Ordered.class;
  }

  private static boolean isWithoutHierarchy(AnnotatedElement source, Predicate<Class<?>> searchEnclosingClass) {
    if (source == Object.class) {
      return true;
    }

    if (source instanceof Class<?> sourceClass) {
      boolean noSuperTypes = sourceClass.getSuperclass() == Object.class && sourceClass.getInterfaces().length == 0;
      return searchEnclosingClass.test(sourceClass)
             ? noSuperTypes && sourceClass.getEnclosingClass() == null
             : noSuperTypes;
    }
    if (source instanceof Method sourceMethod) {
      return Modifier.isPrivate(sourceMethod.getModifiers())
              || isWithoutHierarchy(sourceMethod.getDeclaringClass(), searchEnclosingClass);
    }
    return true;
  }

  static void clearCache() {
    declaredAnnotationCache.clear();
    baseTypeMethodsCache.clear();
  }

}
