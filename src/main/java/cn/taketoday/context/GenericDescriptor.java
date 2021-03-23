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
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import cn.taketoday.context.factory.BeanProperty;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;

/**
 * @author TODAY 2021/3/22 20:37
 * @since 3.0
 */
public class GenericDescriptor {
  private final Class<?> type;
  private final Type genericType;
  private final Type[] genericTypes;

  public GenericDescriptor(Class<?> type, Type genericType, Type[] genericTypes) {
    Assert.notNull(type, "type must not be null");
    this.type = type;
    this.genericType = genericType;
    this.genericTypes = genericTypes;
  }

  public Class<?> getType() {
    return type;
  }

  public Type getGenericType() {
    return genericType;
  }

  public Type[] getGenericTypes() {
    return genericTypes;
  }

  private GenericDescriptor[] generics;

//  public GenericDescriptor[] getGenerics() {
//    GenericDescriptor[] generics = this.generics;
//    if (generics == null) {
//      if (genericType instanceof Class) {
//        final TypeVariable<? extends Class<?>>[] typeParameters = ((Class<?>) genericType).getTypeParameters();
//        GenericDescriptor[] ret = new GenericDescriptor[typeParameters.length];
//        for (int i = 0; i < typeParameters.length; i++) {
//          ret[i] = new GenericDescriptor(type, typeParameters[i], );
//        }
//        this.generics = ret;
//      }
//      else if (this.genericType instanceof ParameterizedType) {
//        Type[] actualTypeArguments = ((ParameterizedType) this.genericType).getActualTypeArguments();
//
//        for (final Type actualTypeArgument : actualTypeArguments) {
//
//        }
//
//        this.generics = generics;
//      }
//    }
//    return generics;
//  }
//
//  public Class<?>[] getGenerics() {
//    Class<?>[] generics = this.generics;
//    if (generics == null) {
//      if (genericType instanceof Class) {
//
//      }
//      else if (this.genericType instanceof ParameterizedType) {
//        Type[] actualTypeArguments = ((ParameterizedType) this.genericType).getActualTypeArguments();
//        generics = GenericTypeResolver.extractClasses(type, actualTypeArguments);
//        this.generics = generics;
//      }
//    }
//    return generics;
//  }

  public Class<?>[] getGenerics(Class<?> genericIfc) {
    if (genericType != type) {
      final TypeVariable<? extends Class<?>>[] typeParameters = genericIfc.getTypeParameters();

      if (genericIfc.isAssignableFrom(type)) {
        final Type[] genericInterfaces = type.getGenericInterfaces();
        Type[] actualTypeArguments = ((ParameterizedType) this.genericType).getActualTypeArguments();
        if (genericInterfaces.length == 0) {
          return GenericTypeResolver.extractClasses(type, actualTypeArguments);
        }
        else {

        }
      }

      Class<?>[] ret = new Class<?>[typeParameters.length];
      final Map<TypeVariable<?>, Type> typeVariableMaps = GenericTypeResolver.getTypeVariableMap(type);
      final Set<Map.Entry<TypeVariable<?>, Type>> entries = typeVariableMaps.entrySet();
      int idx = 0;
      for (final Map.Entry<TypeVariable<?>, Type> entry : entries) {
        final TypeVariable<?> key = entry.getKey();
        final Type value = entry.getValue();
        if (genericIfc == key.getGenericDeclaration()) {
          ret[idx++] = (Class<?>) value;
        }
      }

      if (!Arrays.asList(ret).contains(null)) {
        return ret;
      }
    }
    return GenericTypeResolver.resolveTypeArguments(type, genericIfc);
  }

  @SuppressWarnings("unchecked")
  public <T> Class<T> getGeneric(final Class<?> genericIfc) {
    if (genericType != type) {
      final Map<TypeVariable<?>, Type> typeVariableMaps = GenericTypeResolver.getTypeVariableMap(type);
      final Set<Map.Entry<TypeVariable<?>, Type>> entries = typeVariableMaps.entrySet();
      for (final Map.Entry<TypeVariable<?>, Type> entry : entries) {
        final TypeVariable<?> key = entry.getKey();
        final Type value = entry.getValue();
        if (genericIfc == key.getGenericDeclaration()) {
          return (Class<T>) value;
        }
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

  /**
   * Is this type a primitive type?
   */
  public boolean isPrimitive() {
    return type.isPrimitive();
  }

  // static factory methods

  public static GenericDescriptor map(Class<?> mapClass, Class<?> keyType, Class<?> valueType) {
    return new GenericDescriptor(mapClass, null, new Type[] { keyType, valueType });
  }

  public static GenericDescriptor collection(Class<?> collectionClass, Class<?> elementType) {
    return new GenericDescriptor(collectionClass, null, new Type[] { elementType });
  }

  public static GenericDescriptor ofProperty(final Field field) {
    final Class<?> type = field.getType();
    final Type genericType = field.getGenericType();
    final Type[] genericTypes = ClassUtils.getGenericTypes(genericType);
    return new GenericDescriptor(type, genericType, genericTypes);
  }

  public static GenericDescriptor ofProperty(final BeanProperty beanProperty) {
    return ofProperty(beanProperty.getField());
  }

  public static GenericDescriptor ofParameter(final Parameter parameter) {
    final Class<?> type = parameter.getType();
    final Type parameterizedType = parameter.getParameterizedType();
    final Type[] genericTypes = ClassUtils.getGenericTypes(parameterizedType);
    return new GenericDescriptor(type, parameterizedType, genericTypes);
  }

  public static GenericDescriptor ofClass(Class<?> type) {
    final Type[] generics = ClassUtils.getGenerics(type);
    return new GenericDescriptor(type, type, generics);
  }

  public static GenericDescriptor ofParameter(final Executable executable, int index) {
    Assert.notNull(executable, "Executable must not be null");
    final Class<?>[] parameterTypes = executable.getParameterTypes();
    if (index < 0 || index >= parameterTypes.length) {
      throw new IllegalArgumentException("parameter index is illegal");
    }

    final Type genericType = executable.getGenericParameterTypes()[index];
    final Type[] genericTypes = ClassUtils.getGenericTypes(genericType);
    return new GenericDescriptor(parameterTypes[index], genericType, genericTypes);
  }

  public static GenericDescriptor ofReturnType(final Method method) {
    Assert.notNull(method, "method must not be null");
    final Type genericType = method.getGenericReturnType();
    final Type[] genericTypes = ClassUtils.getGenericTypes(genericType);
    return new GenericDescriptor(method.getReturnType(), genericType, genericTypes);
  }

  public static GenericDescriptor of(Class<?> type, Type genericType) {
    final Type[] genericTypes = ClassUtils.getGenericTypes(genericType);
    return new GenericDescriptor(type, genericType, genericTypes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GenericDescriptor)) return false;
    final GenericDescriptor that = (GenericDescriptor) o;
    return type == that.type
            && Arrays.equals(genericTypes, that.genericTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, genericTypes);
  }

  @Override
  public String toString() {
    if (isArray()) {
      return getComponentType() + "[]";
    }
    final String typeName = this.type.getName();
    if (genericTypes != null) {
      StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
      for (Type argument : genericTypes) {
        stringJoiner.add(argument.getTypeName());
      }
      return typeName + stringJoiner;
    }
    return typeName;
  }
}
