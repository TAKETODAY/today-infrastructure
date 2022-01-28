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
package cn.taketoday.core.conversion.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.MatchingConverter;
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
 * {@link FallbackConverter} instead.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see FallbackConverter
 * @since 3.0
 */
final class ObjectToObjectConverter implements MatchingConverter {
  // Cache for the latest to-method resolved on a given Class
  private static final Map<Class<?>, Member> conversionMemberCache = new ConcurrentHashMap<>(32);

  // Object.class, Object.class

  @Override
  public boolean supports(final TypeDescriptor targetType, final Class<?> sourceType) {
    return !targetType.is(sourceType)
            && hasConversionMethodOrConstructor(targetType.getType(), sourceType);
  }

  @Override
  public Object convert(TypeDescriptor targetType, Object source) {
    Class<?> sourceClass = source.getClass();
    Member member = getValidatedMember(targetType.getType(), sourceClass);

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
      throw new ConversionFailedException(ex.getTargetException(), source, targetType);
    }
    catch (Throwable ex) {
      throw new ConversionFailedException(ex, source, targetType);
    }

    // If sourceClass is Number and targetClass is Integer, the following message should expand to:
    // No toInteger() method exists on java.lang.Number, and no static valueOf/of/from(java.lang.Number)
    // method or Integer(java.lang.Number) constructor exists on java.lang.Integer.
    throw new IllegalStateException(
            String.format("No to%3$s() method exists on %1$s, and no static valueOf/of/from(%1$s)" +
                                  " method or %3$s(%1$s) constructor exists on %2$s.",
                          sourceClass.getName(), targetType.getName(), targetType.getSimpleName()));
  }

  static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
    return (getValidatedMember(targetClass, sourceClass) != null);
  }

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
      return !Modifier.isStatic(method.getModifiers())
             ? isAssignable(method.getDeclaringClass(), sourceClass)
             : method.getParameterTypes()[0] == sourceClass;
    }
    else if (member instanceof Constructor<?> ctor) {
      return ctor.getParameterTypes()[0] == sourceClass;
    }
    else {
      return false;
    }
  }

  /**
   * Check if the right-hand side type may be assigned to the left-hand side
   * type, assuming setting by reflection. Considers primitive wrapper
   * classes as assignable to the corresponding primitive types.
   *
   * @param lhsType the target type
   * @param rhsType the value type that should be assigned to the target type
   * @return if the target type is assignable from the value type
   */
  private static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
    return lhsType.isAssignableFrom(rhsType);
  }

  private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
    if (String.class == targetClass || String.class == sourceClass) {
      // Do not accept a toString() method or any to methods on String itself
      return null;
    }

    final Method method = ReflectionUtils.findMethod(sourceClass, "to" + targetClass.getSimpleName());
    return method != null
                   && !Modifier.isStatic(method.getModifiers())
                   && isAssignable(targetClass, method.getReturnType()) ? method : null;
  }

  private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
    if (String.class == targetClass) {
      // Do not accept the String.valueOf(Object) method
      return null;
    }

    Method method = ReflectionUtils.findMethod(targetClass, "valueOf", sourceClass);
    if (method == null || !Modifier.isStatic(method.getModifiers())) {
      method = ReflectionUtils.findMethod(targetClass, "of", sourceClass);
      if (method == null || !Modifier.isStatic(method.getModifiers())) {
        method = ReflectionUtils.findMethod(targetClass, "from", sourceClass);
      }
    }
    return method;
  }

  private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
    try {
      return targetClass.getDeclaredConstructor(sourceClass);
    }
    catch (NoSuchMethodException e) {
      return null;
    }
  }

}
