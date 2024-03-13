/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Helper class for resolving generic types against type variables.
 *
 * <p>Mainly intended for usage within the framework, resolving method
 * parameter types even when they are declared generically.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/3/8 18:22
 */
public abstract class GenericTypeResolver {

  /** Cache from Class to TypeVariable Map. */
  @SuppressWarnings("rawtypes")
  private static final ConcurrentHashMap<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentHashMap<>();

  /**
   * Determine the target type for the generic return type of the given method,
   * where formal type variables are declared on the given class.
   *
   * @param method the method to introspect
   * @param clazz the class to resolve type variables against
   * @return the corresponding generic parameter or return type
   */
  public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
    Assert.notNull(method, "Method is required");
    Assert.notNull(clazz, "Class is required");
    return ResolvableType.forReturnType(method, clazz).resolve(method.getReturnType());
  }

  /**
   * Resolve the single type argument of the given generic interface against the given
   * target method which is assumed to return the given interface or an implementation
   * of it.
   *
   * @param method the target method to check the return type of
   * @param genericIfc the generic interface or superclass to resolve the type argument from
   * @return the resolved parameter type of the method return type, or {@code null}
   * if not resolvable or if the single argument is of type {@link WildcardType}.
   */
  @Nullable
  public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
    Assert.notNull(method, "Method is required");
    ResolvableType resolvableType = ResolvableType.forReturnType(method).as(genericIfc);
    if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
      return null;
    }
    return getSingleGeneric(resolvableType);
  }

  /**
   * Resolve the single type argument of the given generic interface against
   * the given target class which is assumed to implement the generic interface
   * and possibly declare a concrete type for its type variable.
   *
   * @param clazz the target class to check against
   * @param genericIfc the generic interface or superclass to resolve the type argument from
   * @return the resolved type of the argument, or {@code null} if not resolvable
   */
  @Nullable
  public static <T> Class<T> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
    ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
    if (!resolvableType.hasGenerics()) {
      return null;
    }
    return getSingleGeneric(resolvableType);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static <T> Class<T> getSingleGeneric(ResolvableType resolvableType) {
    Assert.isTrue(resolvableType.getGenerics().length == 1,
            () -> "Expected 1 type argument on generic interface [" + resolvableType +
                    "] but found " + resolvableType.getGenerics().length);
    return (Class<T>) resolvableType.getGeneric().resolve();
  }

  /**
   * Resolve the type arguments of the given generic interface against the given
   * target class which is assumed to implement the generic interface and possibly
   * declare concrete types for its type variables.
   *
   * @param clazz the target class to check against
   * @param genericIfc the generic interface or superclass to resolve the type argument from
   * @return the resolved type of each argument, with the array size matching the
   * number of actual type arguments, or {@code null} if not resolvable
   */
  @Nullable
  public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
    ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
    if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
      return null;
    }
    return type.resolveGenerics(Object.class);
  }

  /**
   * Resolve the given generic type against the given context class,
   * substituting type variables as far as possible.
   *
   * @param genericType the (potentially) generic type
   * @param contextClass a context class for the target type, for example a class
   * in which the target type appears in a method signature (can be {@code null})
   * @return the resolved type (possibly the given generic type as-is)
   */
  public static Type resolveType(Type genericType, @Nullable Class<?> contextClass) {
    if (contextClass != null) {
      if (genericType instanceof TypeVariable<?> typeVariable) {
        ResolvableType resolvedTypeVariable = resolveVariable(
                typeVariable, ResolvableType.forClass(contextClass));
        if (resolvedTypeVariable != ResolvableType.NONE) {
          Class<?> resolved = resolvedTypeVariable.resolve();
          if (resolved != null) {
            return resolved;
          }
        }
      }
      else if (genericType instanceof ParameterizedType parameterizedType) {
        ResolvableType resolvedType = ResolvableType.forType(genericType);
        if (resolvedType.hasUnresolvableGenerics()) {
          ResolvableType[] generics = new ResolvableType[parameterizedType.getActualTypeArguments().length];
          Type[] typeArguments = parameterizedType.getActualTypeArguments();
          ResolvableType contextType = ResolvableType.forClass(contextClass);
          for (int i = 0; i < typeArguments.length; i++) {
            Type typeArgument = typeArguments[i];
            if (typeArgument instanceof TypeVariable<?> typeVariable) {
              ResolvableType resolvedTypeArgument = resolveVariable(typeVariable, contextType);
              if (resolvedTypeArgument != ResolvableType.NONE) {
                generics[i] = resolvedTypeArgument;
              }
              else {
                generics[i] = ResolvableType.forType(typeArgument);
              }
            }
            else if (typeArgument instanceof ParameterizedType) {
              generics[i] = ResolvableType.forType(resolveType(typeArgument, contextClass));
            }
            else {
              generics[i] = ResolvableType.forType(typeArgument);
            }
          }
          Class<?> rawClass = resolvedType.getRawClass();
          if (rawClass != null) {
            return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
          }
        }
      }
    }
    return genericType;
  }

  private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
    ResolvableType resolvedType;
    if (contextType.hasGenerics()) {
      ResolvableType.VariableResolver variableResolver = contextType.asVariableResolver();
      if (variableResolver == null) {
        return ResolvableType.NONE;
      }
      resolvedType = variableResolver.resolveVariable(typeVariable);
      if (resolvedType != null) {
        return resolvedType;
      }
    }

    ResolvableType superType = contextType.getSuperType();
    if (superType != ResolvableType.NONE) {
      resolvedType = resolveVariable(typeVariable, superType);
      if (resolvedType != ResolvableType.NONE) {
        return resolvedType;
      }
    }
    for (ResolvableType ifc : contextType.getInterfaces()) {
      resolvedType = resolveVariable(typeVariable, ifc);
      if (resolvedType != ResolvableType.NONE) {
        return resolvedType;
      }
    }
    return ResolvableType.NONE;
  }

  /**
   * Resolve the specified generic type against the given TypeVariable map.
   *
   * @param genericType the generic type to resolve
   * @param map the TypeVariable Map to resolved against
   * @return the type if it resolves to a Class, or {@code Object.class} otherwise
   */
  @SuppressWarnings("rawtypes")
  public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
    return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
  }

  /**
   * Build a mapping of {@link TypeVariable#getName TypeVariable names} to
   * {@link Class concrete classes} for the specified {@link Class}.
   * Searches all super types, enclosing types and interfaces.
   *
   * @see #resolveType(Type, Map)
   */
  @SuppressWarnings("rawtypes")
  public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
    Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
    if (typeVariableMap == null) {
      typeVariableMap = new HashMap<>();
      buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
      typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
    }
    return typeVariableMap;
  }

  @SuppressWarnings("rawtypes")
  private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
    if (type != ResolvableType.NONE) {
      Class<?> resolved = type.resolve();
      if (resolved != null && type.getType() instanceof ParameterizedType) {
        TypeVariable<?>[] variables = resolved.getTypeParameters();
        for (int i = 0; i < variables.length; i++) {
          ResolvableType generic = type.getGeneric(i);
          while (generic.getType() instanceof TypeVariable<?>) {
            generic = generic.resolveType();
          }
          if (generic != ResolvableType.NONE) {
            typeVariableMap.put(variables[i], generic.getType());
          }
        }
      }
      buildTypeVariableMap(type.getSuperType(), typeVariableMap);
      for (ResolvableType interfaceType : type.getInterfaces()) {
        buildTypeVariableMap(interfaceType, typeVariableMap);
      }
      if (resolved != null && resolved.isMemberClass()) {
        buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
      }
    }
  }

  @SuppressWarnings({ "rawtypes" })
  private record TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap)
          implements ResolvableType.VariableResolver {

    @Override
    public ResolvableType resolveVariable(TypeVariable<?> variable) {
      Type type = this.typeVariableMap.get(variable);
      return (type != null ? ResolvableType.forType(type) : null);
    }

    @Override
    public Object getSource() {
      return this.typeVariableMap;
    }
  }

}
