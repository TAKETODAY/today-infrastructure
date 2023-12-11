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

package cn.taketoday.aot.hint;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Register the necessary reflection hints so that the specified type can be
 * bound at runtime. Fields, constructors, properties and record components
 * are registered, except for a set of types like those in the {@code java.}
 * package where just the type is registered. Types are discovered transitively
 * on properties and record components, and generic types are registered as well.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BindingReflectionHintsRegistrar {

  private static final String JACKSON_ANNOTATION = "com.fasterxml.jackson.annotation.JacksonAnnotation";

  private static final boolean jacksonAnnotationPresent = ClassUtils.isPresent(JACKSON_ANNOTATION,
          BindingReflectionHintsRegistrar.class.getClassLoader());

  /**
   * Register the necessary reflection hints to bind the specified types.
   *
   * @param hints the hints instance to use
   * @param types the types to register
   */
  public void registerReflectionHints(ReflectionHints hints, Type... types) {
    Set<Type> seen = new LinkedHashSet<>();
    for (Type type : types) {
      registerReflectionHints(hints, seen, type);
    }
  }

  private boolean shouldSkipType(Class<?> type) {
    return type.isPrimitive() || type == Object.class;
  }

  private boolean shouldSkipMembers(Class<?> type) {
    return type.getCanonicalName().startsWith("java.") || type.isArray();
  }

  private void registerReflectionHints(ReflectionHints hints, Set<Type> seen, Type type) {
    if (seen.contains(type)) {
      return;
    }
    seen.add(type);
    if (type instanceof Class<?> clazz) {
      if (shouldSkipType(clazz)) {
        return;
      }
      hints.registerType(clazz, typeHint -> {
        if (!shouldSkipMembers(clazz)) {
          if (clazz.isRecord()) {
            typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
            for (RecordComponent recordComponent : clazz.getRecordComponents()) {
              registerRecordHints(hints, seen, recordComponent.getAccessor());
            }
          }
          if (clazz.isEnum()) {
            typeHint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
          }
          typeHint.withMembers(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
          for (Method method : clazz.getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("set") && method.getParameterCount() == 1) {
              registerPropertyHints(hints, seen, method, 0);
            }
            else if ((methodName.startsWith("get") && method.getParameterCount() == 0 && method.getReturnType() != Void.TYPE)
                    || (methodName.startsWith("is") && method.getParameterCount() == 0 && method.getReturnType() == boolean.class)) {
              registerPropertyHints(hints, seen, method, -1);
            }
          }
          if (jacksonAnnotationPresent) {
            registerJacksonHints(hints, clazz);
          }
        }
      });
    }
    Set<Class<?>> referencedTypes = new LinkedHashSet<>();
    collectReferencedTypes(referencedTypes, ResolvableType.forType(type));
    referencedTypes.forEach(referencedType -> registerReflectionHints(hints, seen, referencedType));
  }

  private void registerRecordHints(ReflectionHints hints, Set<Type> seen, Method method) {
    hints.registerMethod(method, ExecutableMode.INVOKE);
    MethodParameter methodParameter = MethodParameter.forExecutable(method, -1);
    Type methodParameterType = methodParameter.getGenericParameterType();
    registerReflectionHints(hints, seen, methodParameterType);
  }

  private void registerPropertyHints(ReflectionHints hints, Set<Type> seen, @Nullable Method method, int parameterIndex) {
    if (method != null && method.getDeclaringClass() != Object.class &&
            method.getDeclaringClass() != Enum.class) {
      hints.registerMethod(method, ExecutableMode.INVOKE);
      MethodParameter methodParameter = MethodParameter.forExecutable(method, parameterIndex);
      Type methodParameterType = methodParameter.getGenericParameterType();
      registerReflectionHints(hints, seen, methodParameterType);
    }
  }

  private void collectReferencedTypes(Set<Class<?>> types, ResolvableType resolvableType) {
    Class<?> clazz = resolvableType.resolve();
    if (clazz != null && !types.contains(clazz)) {
      types.add(clazz);
      for (ResolvableType genericResolvableType : resolvableType.getGenerics()) {
        collectReferencedTypes(types, genericResolvableType);
      }
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null && superClass != Object.class
              && superClass != Record.class && superClass != Enum.class) {
        types.add(superClass);
      }
    }
  }

  private void registerJacksonHints(ReflectionHints hints, Class<?> clazz) {
    ReflectionUtils.doWithFields(clazz, field ->
            forEachJacksonAnnotation(field, annotation -> {
              Field sourceField = (Field) annotation.getSource();
              if (sourceField != null) {
                hints.registerField(sourceField);
              }
              registerHintsForClassAttributes(hints, annotation);
            }));
    ReflectionUtils.doWithMethods(clazz, method ->
            forEachJacksonAnnotation(method, annotation -> {
              Method sourceMethod = (Method) annotation.getSource();
              if (sourceMethod != null) {
                hints.registerMethod(sourceMethod, ExecutableMode.INVOKE);
              }
              registerHintsForClassAttributes(hints, annotation);
            }));
    forEachJacksonAnnotation(clazz, annotation -> registerHintsForClassAttributes(hints, annotation));
  }

  private void forEachJacksonAnnotation(AnnotatedElement element, Consumer<MergedAnnotation<Annotation>> action) {
    MergedAnnotations
            .from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
            .stream(JACKSON_ANNOTATION)
            .filter(MergedAnnotation::isMetaPresent)
            .forEach(action);
  }

  private void registerHintsForClassAttributes(ReflectionHints hints, MergedAnnotation<Annotation> annotation) {
    annotation.getRoot().asMap().values().forEach(value -> {
      if (value instanceof Class<?> classValue && value != Void.class) {
        hints.registerType(classValue, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      }
    });
  }

}
