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

package cn.taketoday.core.conversion.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;

/**
 * Generic converter that uses conventions to convert a source object to a
 * {@code targetType} by delegating to a method on the source object or to
 * a static factory method or constructor on the {@code targetType}.
 *
 * <h3>Conversion Algorithm</h3>
 * <ol>
 * <li>Invoke a non-static {@code to[targetType.simpleName]()} method on the
 * source object that has a return type equal to {@code targetType}, if such
 * a method exists. For example, {@code org.example.Bar Foo#toBar()} is a
 * method that follows this convention.
 * <li>Otherwise invoke a <em>static</em> {@code valueOf(sourceType)} or Java
 * 8 style <em>static</em> {@code of(sourceType)} or {@code from(sourceType)}
 * method on the {@code targetType}, if such a method exists.
 * <li>Otherwise invoke a constructor on the {@code targetType} that accepts
 * a single {@code sourceType} argument, if such a constructor exists.
 * <li>Otherwise throw a {@link ConversionFailedException}.
 * </ol>
 *
 * <p><strong>Warning</strong>: this converter does <em>not</em> support the
 * {@link Object#toString()} method for converting from a {@code sourceType}
 * to {@code java.lang.String}. For {@code toString()} support, use
 * {@link FallbackObjectToStringConverter} instead.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see FallbackObjectToStringConverter
 * @since 3.0
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {

  // Cache for the latest to-method resolved on a given Class
  private static final Map<Class<?>, Member> conversionMemberCache =
          new ConcurrentReferenceHashMap<>(32);

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return (sourceType.getType() != targetType.getType() &&
            hasConversionMethodOrConstructor(targetType.getType(), sourceType.getType()));
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    Class<?> sourceClass = sourceType.getType();
    Class<?> targetClass = targetType.getType();
    Member member = getValidatedMember(targetClass, sourceClass);

    try {
      if (member instanceof Method method) {
        ReflectionUtils.makeAccessible(method);
        if (!Modifier.isStatic(method.getModifiers())) {
          return method.invoke(source);
        }
        else {
          return method.invoke(null, source);
        }
      }
      else if (member instanceof Constructor<?> ctor) {
        ReflectionUtils.makeAccessible(ctor);
        return ctor.newInstance(source);
      }
    }
    catch (InvocationTargetException ex) {
      throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
    }
    catch (Throwable ex) {
      throw new ConversionFailedException(sourceType, targetType, source, ex);
    }

    // If sourceClass is Number and targetClass is Integer, the following message should expand to:
    // No toInteger() method exists on java.lang.Number, and no static valueOf/of/from(java.lang.Number)
    // method or Integer(java.lang.Number) constructor exists on java.lang.Integer.
    throw new IllegalStateException(String.format("No to%3$s() method exists on %1$s, " +
                    "and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
            sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName()));
  }

  static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
    return (getValidatedMember(targetClass, sourceClass) != null);
  }

  @Nullable
  private static Member getValidatedMember(Class<?> targetClass, Class<?> sourceClass) {
    Member member = conversionMemberCache.get(targetClass);
    if (isApplicable(member, sourceClass)) {
      return member;
    }

    member = determineToMethod(targetClass, sourceClass);
    if (member == null) {
      member = determineFactoryMethod(targetClass, sourceClass);
      if (member == null) {
        member = determineFactoryConstructor(targetClass, sourceClass);
        if (member == null) {
          return null;
        }
      }
    }

    conversionMemberCache.put(targetClass, member);
    return member;
  }

  private static boolean isApplicable(Member member, Class<?> sourceClass) {
    if (member instanceof Method method) {
      return (!Modifier.isStatic(method.getModifiers()) ?
              ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) :
              method.getParameterTypes()[0] == sourceClass);
    }
    else if (member instanceof Constructor) {
      Constructor<?> ctor = (Constructor<?>) member;
      return (ctor.getParameterTypes()[0] == sourceClass);
    }
    else {
      return false;
    }
  }

  @Nullable
  private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
    if (String.class == targetClass || String.class == sourceClass) {
      // Do not accept a toString() method or any to methods on String itself
      return null;
    }

    Method method = ReflectionUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
    return (method != null && !Modifier.isStatic(method.getModifiers()) &&
                    ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null);
  }

  @Nullable
  private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
    if (String.class == targetClass) {
      // Do not accept the String.valueOf(Object) method
      return null;
    }

    Method method = ReflectionUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
    if (method == null) {
      method = ReflectionUtils.getStaticMethod(targetClass, "of", sourceClass);
      if (method == null) {
        method = ReflectionUtils.getStaticMethod(targetClass, "from", sourceClass);
      }
    }
    return method;
  }

  @Nullable
  private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
    return ReflectionUtils.getConstructorIfAvailable(targetClass, sourceClass);
  }

}
