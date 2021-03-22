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

package cn.taketoday.context;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import cn.taketoday.context.factory.BeanProperty;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;

/**
 * @author TODAY 2021/3/22 20:37
 * @since 3.0
 */
public class GenericDescriptor {
  private final Class<?> type;
  private final Type genericType;

  public GenericDescriptor(Class<?> type, Type genericType) {
    Assert.notNull(type, "type must not be null");
    this.type = type;
    this.genericType = genericType;
  }

  public Class<?> getType() {
    return type;
  }

  public Type getGenericType() {
    return genericType;
  }

  public Class<?>[] getGenerics(Class<?> genericIfc) {
    if (genericIfc.isAssignableFrom(type)) {
      if (genericType instanceof ParameterizedType) {
        ParameterizedType targetType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = targetType.getActualTypeArguments();
        return GenericTypeResolver.extractClasses(type, actualTypeArguments);
      }
      else {
        return null;
      }
    }
    return GenericTypeResolver.resolveTypeArguments(type, genericIfc);
  }

  public <T> Class<T> getGeneric(Class<?> genericIfc) {
    if (genericIfc.isAssignableFrom(type)) {
      if (genericType instanceof ParameterizedType) {
        ParameterizedType targetType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = targetType.getActualTypeArguments();
        Type typeArg = actualTypeArguments[0];
        if (!(typeArg instanceof WildcardType)) {
          return (Class<T>) typeArg;
        }
      }
      else {
        return null;
      }
    }
    return GenericTypeResolver.resolveTypeArgument(type, genericIfc);
  }

  public boolean isArray() {
    return type.isArray();
  }

  public boolean isCollection() {
    return CollectionUtils.isCollection(type);
  }

  public Class<?> getComponentType() {
    return type.getComponentType();
  }

  public boolean isInstance(Object source) {
    return type.isInstance(source);
  }

  public boolean is(Class<?> testClass) {
    return type == testClass;
  }

  public boolean isAssignableFrom(Class<?> subType) {
    return type.isAssignableFrom(subType);
  }

  public boolean isAssignableTo(Class<?> superType) {
    return superType.isAssignableFrom(type);
  }

  public boolean isEnum() {
    return type.isEnum();
  }

  public Object getName() {
    return type.getName();
  }

  public String getSimpleName() {
    return type.getSimpleName();
  }

  // static

  public static GenericDescriptor map(Class<?> mapClass, Class<?> keyType, Class<?> valueType) {
    final DefaultParameterizedType parameterizedType =
            new DefaultParameterizedType(mapClass, new Type[] { keyType, valueType });

    return new GenericDescriptor(mapClass, parameterizedType);
  }

  public static GenericDescriptor collection(Class<?> collectionClass, Class<?> elementType) {
    final DefaultParameterizedType parameterizedType =
            new DefaultParameterizedType(collectionClass, new Type[] { elementType });
    return new GenericDescriptor(collectionClass, parameterizedType);
  }

  public static GenericDescriptor ofField(final Field field) {
    final Class<?> type = field.getType();
    final Type genericType = field.getGenericType();
    return new GenericDescriptor(type, genericType);
  }

  public static GenericDescriptor ofParameter(final Parameter parameter) {
    final Class<?> type = parameter.getType();
    final Type genericType = parameter.getParameterizedType();
    return new GenericDescriptor(type, genericType);
  }

  public static GenericDescriptor ofBeanProperty(final BeanProperty beanProperty) {
    return ofField(beanProperty.getField());
  }

  public static GenericDescriptor of(Class<?> type, Type genericType) {
    return new GenericDescriptor(type, genericType);
  }

  public static GenericDescriptor ofClass(Class<?> type) {
    return new GenericDescriptor(type, type);
  }

  public static GenericDescriptor of(final Executable executable, int index) {
    Assert.notNull(executable, "Executable must not be null");
    final Class<?>[] parameterTypes = executable.getParameterTypes();
    if (index < 0 || index >= parameterTypes.length) {
      throw new IllegalArgumentException("parameter index is illegal");
    }

    final Type genericType = executable.getGenericParameterTypes()[index];
    return new GenericDescriptor(parameterTypes[index], genericType);
  }

  public static GenericDescriptor ofReturnType(final Method method) {
    Assert.notNull(method, "method must not be null");
    final Type genericType = method.getGenericReturnType();
    return new GenericDescriptor(method.getReturnType(), genericType);
  }

  static class DefaultParameterizedType implements ParameterizedType {

    Type ownerType;
    Class<?> rawType;
    Type[] actualTypeArguments;

    DefaultParameterizedType(Class<?> rawType, Type[] actualTypeArguments) {
      this.rawType = rawType;
      this.ownerType = rawType.getDeclaringClass();
      this.actualTypeArguments = actualTypeArguments;
    }

    public Type[] getActualTypeArguments() {
      return this.actualTypeArguments;
    }

    public Class<?> getRawType() {
      return this.rawType;
    }

    public Type getOwnerType() {
      return this.ownerType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof DefaultParameterizedType)) return false;
      final DefaultParameterizedType that = (DefaultParameterizedType) o;
      return Objects.equals(ownerType, that.ownerType)
              && Objects.equals(rawType, that.rawType)
              && Arrays.equals(actualTypeArguments, that.actualTypeArguments);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(ownerType, rawType);
      result = 31 * result + Arrays.hashCode(actualTypeArguments);
      return result;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();

      if (ownerType != null) {
        sb.append(ownerType.getTypeName());
        sb.append("$");
        if (ownerType instanceof DefaultParameterizedType) {
          // Find simple name of nested type by removing the
          // shared prefix with owner.
          sb.append(
                  rawType.getName()
                          .replace(((DefaultParameterizedType) ownerType).rawType.getName() + "$", Constant.BLANK)
          );
        }
        else
          sb.append(rawType.getSimpleName());
      }
      else
        sb.append(rawType.getName());

      if (actualTypeArguments != null) {
        StringJoiner sj = new StringJoiner(", ", "<", ">");
        sj.setEmptyValue("");
        for (Type t : actualTypeArguments) {
          sj.add(t.getTypeName());
        }
        sb.append(sj.toString());
      }

      return sb.toString();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GenericDescriptor)) return false;
    final GenericDescriptor that = (GenericDescriptor) o;
    return type == that.type
            && Objects.equals(genericType, that.genericType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, genericType);
  }

  @Override
  public String toString() {
    return "GenericDescriptor{" +
            "type=" + type +
            ", genericType=" + genericType +
            '}';
  }
}
