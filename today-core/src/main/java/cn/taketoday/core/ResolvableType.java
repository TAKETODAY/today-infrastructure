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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import cn.taketoday.core.SerializableTypeWrapper.MethodParameterTypeProvider;
import cn.taketoday.core.SerializableTypeWrapper.ParameterTypeProvider;
import cn.taketoday.core.SerializableTypeWrapper.TypeProvider;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Encapsulates a Java {@link Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link Class}.
 *
 * <p>{@code ResolvableTypes} may be obtained from {@link #forField(Field) fields},
 * {@link #forParameter(Executable, int) executable parameters},
 * {@link #forReturnType(Method) method returns} or
 * {@link #forClass(Class) classes}. Most methods on this class will themselves return
 * {@link ResolvableType ResolvableTypes}, allowing easy navigation. For example:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.fromField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #forField(Field)
 * @see #forParameter(Executable, int)
 * @see #forReturnType(Method)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @see #forInstance(Object)
 * @see ResolvableTypeProvider
 * @since 3.0
 */
public class ResolvableType implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * {@code ResolvableType} returned when no value is available. {@code NONE} is used
   * in preference to {@code null} so that multiple method calls can be safely chained.
   */
  public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

  public static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

  private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
          new ConcurrentReferenceHashMap<>(256);

  /**
   * The underlying Java type being managed.
   */
  private final Type type;

  /**
   * Optional provider for the type.
   */
  @Nullable
  private final TypeProvider typeProvider;

  /**
   * The {@code VariableResolver} to use or {@code null} if no resolver is available.
   */
  @Nullable
  private final VariableResolver variableResolver;

  /**
   * The component type for an array or {@code null} if the type should be deduced.
   */
  @Nullable
  private final ResolvableType componentType;

  @Nullable
  private final Integer hash;

  @Nullable
  private Class<?> resolved;

  @Nullable
  private ResolvableType superType;

  @Nullable
  private ResolvableType[] interfaces;

  @Nullable
  private ResolvableType[] generics;

  @Nullable
  private volatile Boolean unresolvableGenerics;

  /**
   * Private constructor used to create a new {@link ResolvableType} for cache key purposes,
   * with no upfront resolution.
   */
  private ResolvableType(
          Type type, @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {
    this.type = type;
    this.typeProvider = typeProvider;
    this.variableResolver = variableResolver;
    this.componentType = null;
    this.hash = calculateHashCode();
    this.resolved = null;
  }

  /**
   * Private constructor used to create a new {@link ResolvableType} for cache value purposes,
   * with upfront resolution and a pre-calculated hash.
   */
  private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
          @Nullable VariableResolver variableResolver, @Nullable Integer hash) {
    this.type = type;
    this.typeProvider = typeProvider;
    this.variableResolver = variableResolver;
    this.componentType = null;
    this.hash = hash;
    this.resolved = resolveClass();
  }

  /**
   * Private constructor used to create a new {@link ResolvableType} for uncached purposes,
   * with upfront resolution but lazily calculated hash.
   */
  private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
          @Nullable VariableResolver variableResolver, @Nullable ResolvableType componentType) {

    this.type = type;
    this.typeProvider = typeProvider;
    this.variableResolver = variableResolver;
    this.componentType = componentType;
    this.hash = null;
    this.resolved = resolveClass();
  }

  /**
   * Private constructor used to create a new {@link ResolvableType} on a {@link Class} basis.
   * Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
   */
  private ResolvableType(@Nullable Class<?> clazz) {
    this.resolved = (clazz != null ? clazz : Object.class);
    this.type = this.resolved;
    this.typeProvider = null;
    this.variableResolver = null;
    this.componentType = null;
    this.hash = null;
  }

  /**
   * Return the underling Java {@link Type} being managed.
   */
  public Type getType() {
    return SerializableTypeWrapper.unwrap(this.type);
  }

  /**
   * Return the underlying Java {@link Class} being managed, if available;
   * otherwise {@code null}.
   */
  @Nullable
  public Class<?> getRawClass() {
    if (this.type == this.resolved) {
      return this.resolved;
    }
    Type rawType = this.type;
    if (rawType instanceof ParameterizedType) {
      rawType = ((ParameterizedType) rawType).getRawType();
    }

    if (rawType instanceof Class) {
      return (Class<?>) rawType;
    }
    return null;
  }

  /**
   * Return the underlying source of the resolvable type. Will return a {@link Field},
   * {@link MethodParameter} or {@link Type} depending on how the {@link ResolvableType}
   * was constructed. This method is primarily to provide access to additional type
   * information or meta-data that alternative JVM languages may provide.
   */
  public Object getSource() {
    if (typeProvider != null) {
      Object source = typeProvider.getSource();
      if (source != null) {
        return source;
      }
    }
    // fallback
    return type;
  }

  /**
   * Return this type as a resolved {@code Class}, falling back to
   * {@link Object} if no specific class can be resolved.
   *
   * @return the resolved {@link Class} or the {@code Object} fallback
   * @see #getRawClass()
   * @see #resolve(Class)
   */
  public Class<?> toClass() {
    return resolve(Object.class);
  }

  /**
   * Determine whether the given object is an instance of this {@code ResolvableType}.
   *
   * @param obj the object to check
   * @see #isAssignableFrom(Class)
   */
  public boolean isInstance(@Nullable Object obj) {
    return obj != null && isAssignableFrom(obj.getClass());
  }

  /**
   * Determine whether this {@code ResolvableType} is assignable from the
   * specified other type.
   *
   * @param other the type to be checked against (as a {@code Class})
   * @see #isAssignableFrom(ResolvableType)
   */
  public boolean isAssignableFrom(Class<?> other) {
    return isAssignableFrom(forClass(other), null);
  }

  /**
   * Determine whether this {@code ResolvableType} is assignable from the
   * specified other type.
   * <p>Attempts to follow the same rules as the Java compiler, considering
   * whether both the {@link #resolve() resolved} {@code Class} is
   * {@link Class#isAssignableFrom(Class) assignable from} the given type
   * as well as whether all {@link #getGenerics() generics} are assignable.
   *
   * @param other the type to be checked against (as a {@code ResolvableType})
   * @return {@code true} if the specified other type can be assigned to this
   * {@code ResolvableType}; {@code false} otherwise
   */
  public boolean isAssignableFrom(ResolvableType other) {
    return isAssignableFrom(other, null);
  }

  private boolean isAssignableFrom(ResolvableType other, @Nullable Map<Type, Type> matchedBefore) {
    Assert.notNull(other, "ResolvableType is required");

    // If we cannot resolve types, we are not assignable
    if (this == NONE || other == NONE) {
      return false;
    }

    // Deal with array by delegating to the component type
    if (isArray()) {
      return other.isArray() && getComponentType().isAssignableFrom(other.getComponentType());
    }

    if (matchedBefore != null && matchedBefore.get(this.type) == other.type) {
      return true;
    }

    // Deal with wildcard bounds
    WildcardBounds ourBounds = WildcardBounds.get(this);
    WildcardBounds typeBounds = WildcardBounds.get(other);

    // In the form X is assignable to <? extends Number>
    if (typeBounds != null) {
      return ourBounds != null
              && ourBounds.isSameKind(typeBounds)
              && ourBounds.isAssignableFrom(typeBounds.getBounds());
    }

    // In the form <? extends Number> is assignable to X...
    if (ourBounds != null) {
      return ourBounds.isAssignableFrom(other);
    }

    // Main assignability check about to follow
    boolean exactMatch = matchedBefore != null;  // We're checking nested generic variables now...
    boolean checkGenerics = true;
    Class<?> ourResolved = null;
    if (type instanceof TypeVariable<?> variable) {
      // Try default variable resolution
      if (variableResolver != null) {
        ResolvableType resolved = variableResolver.resolveVariable(variable);
        if (resolved != null) {
          ourResolved = resolved.resolve();
        }
      }
      if (ourResolved == null && other.variableResolver != null) {
        // Try variable resolution against target type
        ResolvableType resolved = other.variableResolver.resolveVariable(variable);
        if (resolved != null) {
          ourResolved = resolved.resolve();
          checkGenerics = false;
        }
      }
      if (ourResolved == null) {
        // Unresolved type variable, potentially nested -> never insist on exact match
        exactMatch = false;
      }
    }
    if (ourResolved == null) {
      ourResolved = resolve(Object.class);
    }
    Class<?> otherResolved = other.toClass();

    // We need an exact type match for generics
    // List<CharSequence> is not assignable from List<String>
    if (exactMatch ? !ourResolved.equals(otherResolved) : !ClassUtils.isAssignable(ourResolved, otherResolved)) {
      return false;
    }

    if (checkGenerics) {
      // Recursively check each generic
      ResolvableType[] ourGenerics = getGenerics();
      ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
      if (ourGenerics.length != typeGenerics.length) {
        return false;
      }
      if (matchedBefore == null) {
        matchedBefore = new IdentityHashMap<>(1);
      }
      matchedBefore.put(type, other.type);
      for (int i = 0; i < ourGenerics.length; i++) {
        if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBefore)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Return {@code true} if this type resolves to a Class that represents an array.
   *
   * @see #getComponentType()
   */
  public boolean isArray() {
    if (this == NONE) {
      return false;
    }
    Type type = this.type;
    return (type instanceof Class && ((Class<?>) type).isArray())
            || type instanceof GenericArrayType
            || resolveType(type).isArray();
  }

  /**
   * Return the ResolvableType representing the component type of the array or
   * {@link #NONE} if this type does not represent an array.
   *
   * @see #isArray()
   */
  public ResolvableType getComponentType() {
    if (this == NONE) {
      return NONE;
    }
    if (this.componentType != null) {
      return this.componentType;
    }
    Type type = this.type;
    if (type instanceof Class) {
      Class<?> componentType = ((Class<?>) type).getComponentType();
      return forType(componentType, this.variableResolver);
    }
    if (type instanceof GenericArrayType) {
      return forType(((GenericArrayType) type).getGenericComponentType(), this.variableResolver);
    }
    return resolveType(type).getComponentType();
  }

  /**
   * Convenience method to return this type as a resolvable {@link Collection} type.
   * Returns {@link #NONE} if this type does not implement or extend
   * {@link Collection}.
   *
   * @see #as(Class)
   * @see #asMap()
   */
  public ResolvableType asCollection() {
    return as(Collection.class);
  }

  /**
   * Convenience method to return this type as a resolvable {@link Map} type.
   * Returns {@link #NONE} if this type does not implement or extend
   * {@link Map}.
   *
   * @see #as(Class)
   * @see #asCollection()
   */
  public ResolvableType asMap() {
    return as(Map.class);
  }

  /**
   * Return this type as a {@link ResolvableType} of the specified class. Searches
   * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
   * hierarchies to find a match, returning {@link #NONE} if this type does not
   * implement or extend the specified class.
   *
   * @param type the required type (typically narrowed)
   * @return a {@link ResolvableType} representing this object as the specified
   * type, or {@link #NONE} if not resolvable as that type
   * @see #asCollection()
   * @see #asMap()
   * @see #getSuperType()
   * @see #getInterfaces()
   */
  public ResolvableType as(Class<?> type) {
    if (this == NONE) {
      return NONE;
    }
    if (resolved == null || resolved == type) {
      return this;
    }
    for (ResolvableType interfaceType : getInterfaces()) {
      ResolvableType interfaceAsType = interfaceType.as(type);
      if (interfaceAsType != NONE) {
        return interfaceAsType;
      }
    }
    return getSuperType().as(type);
  }

  /**
   * Return a {@link ResolvableType} representing the direct supertype of this type.
   * If no supertype is available this method returns {@link #NONE}.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @see #getInterfaces()
   */
  public ResolvableType getSuperType() {
    Class<?> resolved = resolve();
    if (resolved == null) {
      return NONE;
    }
    try {
      Type superclass = resolved.getGenericSuperclass();
      if (superclass == null) {
        return NONE;
      }
      ResolvableType superType = this.superType;
      if (superType == null) {
        superType = forType(superclass, this);
        this.superType = superType;
      }
      return superType;
    }
    catch (TypeNotPresentException ex) {
      // Ignore non-present types in generic signature
      return NONE;
    }
  }

  /**
   * Return a {@link ResolvableType} array representing the direct interfaces
   * implemented by this type. If this type does not implement any interfaces an
   * empty array is returned.
   * <p>Note: The resulting {@link ResolvableType} instances may not be {@link Serializable}.
   *
   * @see #getSuperType()
   */
  public ResolvableType[] getInterfaces() {
    Class<?> resolved = resolve();
    if (resolved == null) {
      return EMPTY_TYPES_ARRAY;
    }
    ResolvableType[] interfaces = this.interfaces;
    if (interfaces == null) {
      Type[] genericIfcs = resolved.getGenericInterfaces();
      interfaces = new ResolvableType[genericIfcs.length];
      for (int i = 0; i < genericIfcs.length; i++) {
        interfaces[i] = forType(genericIfcs[i], this);
      }
      this.interfaces = interfaces;
    }
    return interfaces;
  }

  /**
   * Return {@code true} if this type contains generic parameters.
   *
   * @see #getGeneric(int...)
   * @see #getGenerics()
   */
  public boolean hasGenerics() {
    return getGenerics().length > 0;
  }

  /**
   * Return {@code true} if this type contains unresolvable generics only,
   * that is, no substitute for any of its declared type variables.
   */
  boolean isEntirelyUnresolvable() {
    if (this == NONE) {
      return false;
    }
    ResolvableType[] generics = getGenerics();
    for (ResolvableType generic : generics) {
      if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine whether the underlying type has any unresolvable generics:
   * either through an unresolvable type variable on the type itself
   * or through implementing a generic interface in a raw fashion,
   * i.e. without substituting that interface's type variables.
   * The result will be {@code true} only in those two scenarios.
   */
  public boolean hasUnresolvableGenerics() {
    if (this == NONE) {
      return false;
    }
    Boolean unresolvableGenerics = this.unresolvableGenerics;
    if (unresolvableGenerics == null) {
      unresolvableGenerics = determineUnresolvableGenerics();
      this.unresolvableGenerics = unresolvableGenerics;
    }
    return unresolvableGenerics;
  }

  private boolean determineUnresolvableGenerics() {
    ResolvableType[] generics = getGenerics();
    for (ResolvableType generic : generics) {
      if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()) {
        return true;
      }
    }
    Class<?> resolved = resolve();
    if (resolved != null) {
      try {
        for (Type genericInterface : resolved.getGenericInterfaces()) {
          if (genericInterface instanceof Class<?> clazz) {
            if (clazz.getTypeParameters().length > 0) {
              return true;
            }
          }
        }
      }
      catch (TypeNotPresentException ex) {
        // Ignore non-present types in generic signature
      }
      Class<?> superclass = resolved.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getSuperType().hasUnresolvableGenerics();
      }
    }
    return false;
  }

  /**
   * Determine whether the underlying type is a type variable that
   * cannot be resolved through the associated variable resolver.
   */
  private boolean isUnresolvableTypeVariable() {
    if (this.type instanceof TypeVariable<?> variable) {
      if (this.variableResolver == null) {
        return true;
      }
      ResolvableType resolved = this.variableResolver.resolveVariable(variable);
      return resolved == null || resolved.isUnresolvableTypeVariable();
    }
    return false;
  }

  /**
   * Determine whether the underlying type represents a wildcard
   * without specific bounds (i.e., equal to {@code ? extends Object}).
   */
  private boolean isWildcardWithoutBounds() {
    if (this.type instanceof WildcardType wt && wt.getLowerBounds().length == 0) {
      Type[] upperBounds = wt.getUpperBounds();
      return upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0]);
    }
    return false;
  }

  /**
   * Return a {@link ResolvableType} for the specified nesting level.
   * See {@link #getNested(int, Map)} for details.
   *
   * @param nestingLevel the nesting level
   * @return the {@link ResolvableType} type, or {@code #NONE}
   */
  public ResolvableType getNested(int nestingLevel) {
    return getNested(nestingLevel, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified nesting level.
   * <p>The nesting level refers to the specific generic parameter that should be returned.
   * A nesting level of 1 indicates this type; 2 indicates the first nested generic;
   * 3 the second; and so on. For example, given {@code List<Set<Integer>>} level 1 refers
   * to the {@code List}, level 2 the {@code Set}, and level 3 the {@code Integer}.
   * <p>The {@code typeIndexesPerLevel} map can be used to reference a specific generic
   * for the given level. For example, an index of 0 would refer to a {@code Map} key;
   * whereas, 1 would refer to the value. If the map does not contain a value for a
   * specific level the last generic will be used (e.g. a {@code Map} value).
   * <p>Nesting levels may also apply to array types; for example given
   * {@code String[]}, a nesting level of 2 refers to {@code String}.
   * <p>If a type does not {@link #hasGenerics() contain} generics the
   * {@link #getSuperType() supertype} hierarchy will be considered.
   *
   * @param nestingLevel the required nesting level, indexed from 1 for the
   * current type, 2 for the first nested generic, 3 for the second and so on
   * @param typeIndexesPerLevel a map containing the generic index for a given
   * nesting level (may be {@code null})
   * @return a {@link ResolvableType} for the nested level, or {@link #NONE}
   */
  public ResolvableType getNested(int nestingLevel, @Nullable Map<Integer, Integer> typeIndexesPerLevel) {
    ResolvableType result = this;
    for (int i = 2; i <= nestingLevel; i++) {
      if (result.isArray()) {
        result = result.getComponentType();
      }
      else {
        // Handle derived types
        while (result != ResolvableType.NONE && !result.hasGenerics()) {
          result = result.getSuperType();
        }
        Integer index = null;
        if (typeIndexesPerLevel != null) {
          index = typeIndexesPerLevel.get(i);
        }

        if (index == null) {
          index = result.getGenerics().length - 1;
        }
        result = result.getGeneric(index);
      }
    }
    return result;
  }

  /**
   * Return a {@link ResolvableType} representing the generic parameter for the
   * given indexes. Indexes are zero based; for example given the type
   * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
   * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
   * for example {@code getGeneric(1, 0)} will access the {@code String} from the
   * nested {@code List}. For convenience, if no indexes are specified the first
   * generic is returned.
   * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
   *
   * @param indexes the indexes that refer to the generic parameter
   * (may be omitted to return the first generic)
   * @return a {@link ResolvableType} for the specified generic, or {@link #NONE}
   * @see #hasGenerics()
   * @see #getGenerics()
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  public ResolvableType getGeneric(@Nullable int... indexes) {
    ResolvableType[] generics = getGenerics();
    if (indexes == null || indexes.length == 0) {
      return (generics.length == 0 ? NONE : generics[0]);
    }
    ResolvableType generic = this;
    for (int index : indexes) {
      generics = generic.getGenerics();
      if (index < 0 || index >= generics.length) {
        return NONE;
      }
      generic = generics[index];
    }
    return generic;
  }

  /**
   * Return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters of
   * this type. If no generics are available an empty array is returned. If you need to
   * access a specific generic consider using the {@link #getGeneric(int...)} method as
   * it allows access to nested generics and protects against
   * {@code IndexOutOfBoundsExceptions}.
   *
   * @return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters
   * (never {@code null})
   * @see #hasGenerics()
   * @see #getGeneric(int...)
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  public ResolvableType[] getGenerics() {
    if (this == NONE) {
      return EMPTY_TYPES_ARRAY;
    }
    ResolvableType[] generics = this.generics;
    if (generics == null) {
      if (type instanceof Class<?> type) {
        Type[] typeParams = type.getTypeParameters();
        generics = new ResolvableType[typeParams.length];
        for (int i = 0; i < generics.length; i++) {
          generics[i] = ResolvableType.forType(typeParams[i], this);
        }
      }
      else if (type instanceof ParameterizedType type) {
        Type[] actualTypeArguments = type.getActualTypeArguments();
        generics = new ResolvableType[actualTypeArguments.length];
        for (int i = 0; i < actualTypeArguments.length; i++) {
          generics[i] = forType(actualTypeArguments[i], this.variableResolver);
        }
      }
      else {
        generics = resolveType(type).getGenerics();
      }
      this.generics = generics;
    }
    return generics;
  }

  /**
   * Convenience method that will {@link #getGenerics() get} and
   * {@link #resolve() resolve} generic parameters.
   *
   * @return an array of resolved generic parameters (the resulting array
   * will never be {@code null}, but it may contain {@code null} elements})
   * @see #getGenerics()
   * @see #resolve()
   */
  public Class<?>[] resolveGenerics() {
    ResolvableType[] generics = getGenerics();
    Class<?>[] resolvedGenerics = new Class<?>[generics.length];
    for (int i = 0; i < generics.length; i++) {
      resolvedGenerics[i] = generics[i].resolve();
    }
    return resolvedGenerics;
  }

  /**
   * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
   * resolve} generic parameters, using the specified {@code fallback} if any type
   * cannot be resolved.
   *
   * @param fallback the fallback class to use if resolution fails
   * @return an array of resolved generic parameters
   * @see #getGenerics()
   * @see #resolve()
   */
  public Class<?>[] resolveGenerics(Class<?> fallback) {
    ResolvableType[] generics = getGenerics();
    Class<?>[] resolvedGenerics = new Class<?>[generics.length];
    for (int i = 0; i < generics.length; i++) {
      resolvedGenerics[i] = generics[i].resolve(fallback);
    }
    return resolvedGenerics;
  }

  /**
   * Convenience method that will {@link #getGeneric(int...) get} and
   * {@link #resolve() resolve} a specific generic parameters.
   *
   * @param indexes the indexes that refer to the generic parameter
   * (maybe omitted to return the first generic)
   * @return a resolved {@link Class} or {@code null}
   * @see #getGeneric(int...)
   * @see #resolve()
   */
  @Nullable
  public Class<?> resolveGeneric(int... indexes) {
    return getGeneric(indexes).resolve();
  }

  /**
   * Resolve this type to a {@link Class}, returning {@code null}
   * if the type cannot be resolved. This method will consider bounds of
   * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
   * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
   * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
   * returns {@code false}, the given type effectively wraps a plain {@code Class},
   * allowing for plain {@code Class} processing if desirable.
   *
   * @return the resolved {@link Class}, or {@code null} if not resolvable
   * @see #resolve(Class)
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  @Nullable
  public Class<?> resolve() {
    return this.resolved;
  }

  /**
   * @since 4.0
   */
  public boolean is(Class<?> clazz) {
    return resolved == clazz;
  }

  /**
   * @since 4.0
   */
  public boolean isCollection() {
    return CollectionUtils.isCollection(resolved);
  }

  /**
   * Is this type a {@link Map} type?
   *
   * @since 4.0
   */
  public boolean isMap() {
    return resolved != null && Map.class.isAssignableFrom(resolved);
  }

  /**
   * Resolve this type to a {@link Class}, returning the specified
   * {@code fallback} if the type cannot be resolved. This method will consider bounds
   * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
   * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
   *
   * @param fallback the fallback class to use if resolution fails
   * @return the resolved {@link Class} or the {@code fallback}
   * @see #resolve()
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  public Class<?> resolve(Class<?> fallback) {
    return this.resolved != null ? this.resolved : fallback;
  }

  @Nullable
  private Class<?> resolveClass() {
    Type type = this.type;
    if (type == EmptyType.INSTANCE) {
      return null;
    }
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof GenericArrayType) {
      Class<?> resolvedComponent = getComponentType().resolve();
      return resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null;
    }
    return resolveType(type).resolve();
  }

  /**
   * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
   * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
   * as it cannot be serialized.
   */
  ResolvableType resolveType() {
    return resolveType(type);
  }

  /**
   * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
   * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
   * as it cannot be serialized.
   */
  private ResolvableType resolveType(Type type) {
    if (type instanceof ParameterizedType) {
      return forType(((ParameterizedType) type).getRawType(), variableResolver);
    }
    if (type instanceof WildcardType wildcardType) {
      Type resolved = resolveBounds(wildcardType.getUpperBounds());
      if (resolved == null) {
        resolved = resolveBounds(wildcardType.getLowerBounds());
      }
      return forType(resolved, variableResolver);
    }
    if (type instanceof TypeVariable<?> variable) {
      VariableResolver variableResolver = this.variableResolver;
      // Try default variable resolution
      if (variableResolver != null) {
        ResolvableType resolved = variableResolver.resolveVariable(variable);
        if (resolved != null) {
          return resolved;
        }
      }
      // Fallback to bounds
      return forType(resolveBounds(variable.getBounds()), variableResolver);
    }
    return NONE;
  }

  @Nullable
  private Type resolveBounds(Type[] bounds) {
    if (bounds.length == 0 || bounds[0] == Object.class) {
      return null;
    }
    return bounds[0];
  }

  @Nullable
  private ResolvableType resolveVariable(TypeVariable<?> variable) {
    Type type = this.type;
    if (type instanceof TypeVariable) {
      return resolveType(type).resolveVariable(variable);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      Class<?> resolved = resolve();
      if (resolved == null) {
        return null;
      }
      TypeVariable<?>[] variables = resolved.getTypeParameters();
      for (int i = 0; i < variables.length; i++) {
        if (Objects.equals(variables[i].getName(), variable.getName())) {
          Type actualType = parameterizedType.getActualTypeArguments()[i];
          return forType(actualType, this.variableResolver);
        }
      }
      Type ownerType = parameterizedType.getOwnerType();
      if (ownerType != null) {
        return forType(ownerType, this.variableResolver).resolveVariable(variable);
      }
    }
    if (type instanceof WildcardType) {
      ResolvableType resolved = resolveType(type).resolveVariable(variable);
      if (resolved != null) {
        return resolved;
      }
    }
    if (this.variableResolver != null) {
      return this.variableResolver.resolveVariable(variable);
    }
    return null;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass()) {
      return false;
    }
    ResolvableType otherType = (ResolvableType) other;
    if (!equalsType(otherType)) {
      return false;
    }
    if (typeProvider != otherType.typeProvider
            && (
            typeProvider == null
                    || otherType.typeProvider == null
                    || !Objects.equals(this.typeProvider.getType(), otherType.typeProvider.getType())
    )) {
      return false;
    }
    return variableResolver == otherType.variableResolver
            || (variableResolver != null
            && otherType.variableResolver != null
            && ObjectUtils.nullSafeEquals(variableResolver.getSource(), otherType.variableResolver.getSource())
    );
  }

  /**
   * Check for type-level equality with another {@code ResolvableType}.
   * <p>In contrast to {@link #equals(Object)} or {@link #isAssignableFrom(ResolvableType)},
   * this works between different sources as well, e.g. method parameters and return types.
   *
   * @param otherType the {@code ResolvableType} to match against
   * @return whether the declared type and type variables match
   * @since 4.0
   */
  public boolean equalsType(ResolvableType otherType) {
    return Objects.equals(this.type, otherType.type)
            && Objects.equals(this.componentType, otherType.componentType);
  }

  @Override
  public int hashCode() {
    return this.hash != null ? this.hash : calculateHashCode();
  }

  private int calculateHashCode() {
    int hashCode = ObjectUtils.nullSafeHashCode(this.type);
    if (this.componentType != null) {
      hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
    }
    if (this.typeProvider != null) {
      hashCode = 31 * hashCode + Objects.hashCode(this.typeProvider.getType());
    }
    if (this.variableResolver != null) {
      hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
    }

    return hashCode;
  }

  /**
   * Adapts this {@link ResolvableType} to a {@link VariableResolver}.
   */
  @Nullable
  VariableResolver asVariableResolver() {
    if (this == NONE) {
      return null;
    }
    return new DefaultVariableResolver(this);
  }

  /**
   * Custom serialization support for {@link #NONE}.
   */
  @Serial
  private Object readResolve() {
    return this.type == EmptyType.INSTANCE ? NONE : this;
  }

  /**
   * Return a String representation of this type in its fully resolved form
   * (including any generic parameters).
   */
  @Override
  public String toString() {
    if (isArray()) {
      return getComponentType() + "[]";
    }
    if (this.resolved == null) {
      return "?";
    }
    if (this.type instanceof TypeVariable<?> variable) {
      if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
        // Don't bother with variable boundaries for toString()...
        // Can cause infinite recursions in case of self-references
        return "?";
      }
    }
    if (hasGenerics()) {
      return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
    }
    return this.resolved.getName();
  }

  // Factory methods

  /**
   * Return a {@link ResolvableType} for the specified {@link Constructor} parameter.
   *
   * @param constructor the source constructor (must not be {@code null})
   * @param parameterIndex the parameter index
   * @return a {@link ResolvableType} for the specified constructor parameter
   * @see #forConstructorParameter(Constructor, int, Class)
   */
  public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex) {
    Assert.notNull(constructor, "Constructor is required");
    return forMethodParameter(new MethodParameter(constructor, parameterIndex));
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Constructor} parameter
   * with a given implementation. Use this variant when the class that declares the
   * constructor includes generic parameter variables that are satisfied by the
   * implementation class.
   *
   * @param constructor the source constructor (must not be {@code null})
   * @param parameterIndex the parameter index
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified constructor parameter
   * @see #forConstructorParameter(Constructor, int)
   */
  public static ResolvableType forConstructorParameter(Constructor<?> constructor, int parameterIndex,
          Class<?> implementationClass) {

    Assert.notNull(constructor, "Constructor is required");
    MethodParameter methodParameter = new MethodParameter(constructor, parameterIndex, implementationClass);
    return forMethodParameter(methodParameter);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Method} parameter.
   *
   * @param method the source method (must not be {@code null})
   * @param parameterIndex the parameter index
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forMethodParameter(Method, int, Class)
   * @see #forMethodParameter(MethodParameter)
   */
  public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
    Assert.notNull(method, "Method is required");
    return forMethodParameter(new MethodParameter(method, parameterIndex));
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
   * given implementation. Use this variant when the class that declares the method
   * includes generic parameter variables that are satisfied by the implementation class.
   *
   * @param method the source method (must not be {@code null})
   * @param parameterIndex the parameter index
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forMethodParameter(Method, int, Class)
   * @see #forMethodParameter(MethodParameter)
   */
  public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
    Assert.notNull(method, "Method is required");
    MethodParameter methodParameter = new MethodParameter(method, parameterIndex, implementationClass);
    return forMethodParameter(methodParameter);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link MethodParameter}.
   *
   * @param methodParameter the source method parameter (must not be {@code null})
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   * @since 4.0
   */
  public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
    return forMethodParameter(methodParameter, (Type) null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link MethodParameter} with a
   * given implementation type. Use this variant when the class that declares the method
   * includes generic parameter variables that are satisfied by the implementation type.
   *
   * @param methodParameter the source method parameter (must not be {@code null})
   * @param implementationType the implementation type
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forMethodParameter(MethodParameter)
   * @since 4.0
   */
  public static ResolvableType forMethodParameter(
          MethodParameter methodParameter,
          @Nullable ResolvableType implementationType) {

    Assert.notNull(methodParameter, "MethodParameter is required");
    implementationType = implementationType != null
                         ? implementationType
                         : forType(methodParameter.getContainingClass());
    ResolvableType owner = implementationType.as(methodParameter.getDeclaringClass());
    return forType(null, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
            getNested(methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link MethodParameter},
   * overriding the target type to resolve with a specific given type.
   *
   * @param methodParameter the source method parameter (must not be {@code null})
   * @param targetType the type to resolve (a part of the method parameter's type)
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   * @since 4.0
   */
  public static ResolvableType forMethodParameter(MethodParameter methodParameter, @Nullable Type targetType) {
    Assert.notNull(methodParameter, "MethodParameter is required");
    return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link MethodParameter} at
   * a specific nesting level, overriding the target type to resolve with a specific
   * given type.
   *
   * @param methodParameter the source method parameter (must not be {@code null})
   * @param targetType the type to resolve (a part of the method parameter's type)
   * @param nestingLevel the nesting level to use
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   * @since 4.0
   */
  public static ResolvableType forMethodParameter(
          MethodParameter methodParameter, @Nullable Type targetType, int nestingLevel) {
    ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
    return forType(targetType, new MethodParameterTypeProvider(methodParameter), owner.asVariableResolver()).
            getNested(nestingLevel, methodParameter.typeIndexesPerLevel);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Executable} parameter.
   *
   * @param executable the source Executable (must not be {@code null})
   * @param parameterIndex the parameter index
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int, Class)
   * @see #forParameter(Parameter)
   */
  public static ResolvableType forParameter(Executable executable, int parameterIndex) {
    return forParameter(executable, parameterIndex, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Method} return type.
   *
   * @param method the source for the method return type
   * @return a {@link ResolvableType} for the specified method return
   * @see #forReturnType(Method, Class)
   */
  public static ResolvableType forReturnType(Method method) {
    Assert.notNull(method, "Method is required");
    return forMethodParameter(new MethodParameter(method, -1));
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Method} return type.
   * Use this variant when the class that declares the method includes generic
   * parameter variables that are satisfied by the implementation class.
   *
   * @param method the source for the method return type
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified method return
   * @see #forReturnType(Method)
   */
  public static ResolvableType forReturnType(Method method, @Nullable Class<?> implementationClass) {
    Assert.notNull(method, "Method must not be null");
    MethodParameter methodParameter = new MethodParameter(method, -1, implementationClass);
    return forMethodParameter(methodParameter);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
   * given implementation. Use this variant when the class that declares the method
   * includes generic parameter variables that are satisfied by the implementation class.
   *
   * @param executable the source method or constructor (must not be {@code null})
   * @param parameterIndex the parameter index
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Parameter)
   */
  public static ResolvableType forParameter(
          Executable executable, int parameterIndex, @Nullable Class<?> implementationClass) {
    Parameter parameter = ReflectionUtils.getParameter(executable, parameterIndex);
    Class<?> declaringClass = executable.getDeclaringClass();
    ResolvableType owner = implementationClass == null
                           ? forType(declaringClass)
                           : forType(implementationClass).as(declaringClass);
    return forType(null, new ParameterTypeProvider(parameter, parameterIndex), owner.asVariableResolver());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Parameter}.
   *
   * @param parameter the source method parameter (must not be {@code null})
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   */
  public static ResolvableType forParameter(Parameter parameter) {
    return forParameter(parameter, (Type) null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Parameter} with a
   * given implementation type. Use this variant when the class that declares the method
   * includes generic parameter variables that are satisfied by the implementation type.
   *
   * @param parameter the source method parameter (must not be {@code null})
   * @param implementationType the implementation type
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Parameter)
   */
  public static ResolvableType forParameter(Parameter parameter, @Nullable ResolvableType implementationType) {
    Assert.notNull(parameter, "Parameter is required");
    Executable executable = parameter.getDeclaringExecutable();
    Class<?> declaringClass = executable.getDeclaringClass();

    ResolvableType owner;
    if (implementationType != null) {
      owner = implementationType.as(declaringClass);
    }
    else {
      owner = forType(declaringClass);
    }

    return forType(null, new ParameterTypeProvider(parameter), owner.asVariableResolver());
  }

  public static ResolvableType forParameter(Parameter parameter, Class<?> implementationType) {
    return forParameter(parameter, forType(implementationType));
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Parameter},
   * overriding the target type to resolve with a specific given type.
   *
   * @param parameter the source method parameter (must not be {@code null})
   * @param targetType the type to resolve (a part of the method parameter's type)
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   */
  public static ResolvableType forParameter(Parameter parameter, @Nullable Type targetType) {
    Assert.notNull(parameter, "Parameter is required");
    Executable executable = parameter.getDeclaringExecutable();
    Class<?> declaringClass = executable.getDeclaringClass();
    ResolvableType owner = forType(declaringClass);
    return forType(targetType, new ParameterTypeProvider(parameter), owner.asVariableResolver());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Class},
   * using the full generic type information for assignability checks.
   * For example: {@code ResolvableType.fromClass(MyArrayList.class)}.
   *
   * @param clazz the class to introspect ({@code null} is semantically
   * equivalent to {@code Object.class} for typical use cases here)
   * @return a {@link ResolvableType} for the specified class
   * @see #forClass(Class, Class)
   * @see #forClassWithGenerics(Class, Class...)
   */
  public static ResolvableType forClass(@Nullable Class<?> clazz) {
    return new ResolvableType(clazz);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Class},
   * doing assignability checks against the raw class only (analogous to
   * {@link Class#isAssignableFrom}, which this serves as a wrapper for.
   * For example: {@code ResolvableType.fromRawClass(List.class)}.
   *
   * @param clazz the class to introspect ({@code null} is semantically
   * equivalent to {@code Object.class} for typical use cases here)
   * @return a {@link ResolvableType} for the specified class
   * @see #forClass(Class)
   * @see #getRawClass()
   */
  public static ResolvableType forRawClass(@Nullable Class<?> clazz) {
    return new ResolvableType(clazz) {
      @Override
      public ResolvableType[] getGenerics() {
        return EMPTY_TYPES_ARRAY;
      }

      @Override
      public boolean isAssignableFrom(Class<?> other) {
        return clazz == null || ClassUtils.isAssignable(clazz, other);
      }

      @Override
      public boolean isAssignableFrom(ResolvableType other) {
        Class<?> otherClass = other.resolve();
        return otherClass != null
                && (clazz == null || ClassUtils.isAssignable(clazz, otherClass));
      }
    };
  }

  /**
   * Return a {@link ResolvableType} for the specified base type
   * (interface or base class) with a given implementation class.
   * For example: {@code ResolvableType.fromClass(List.class, MyArrayList.class)}.
   *
   * @param baseType the base type (must not be {@code null})
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified base type backed by the
   * given implementation class
   * @see #forClass(Class)
   * @see #forClassWithGenerics(Class, Class...)
   */
  public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
    Assert.notNull(baseType, "Base type is required");
    ResolvableType asType = forType(implementationClass).as(baseType);
    return (asType == NONE ? forType(baseType) : asType);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
   *
   * @param clazz the class (or interface) to introspect
   * @param generics the generics of the class
   * @return a {@link ResolvableType} for the specific class and generics
   * @see #forClassWithGenerics(Class, ResolvableType...)
   */
  public static ResolvableType forClassWithGenerics(Class<?> clazz, Class<?>... generics) {
    Assert.notNull(clazz, "Class is required");
    Assert.notNull(generics, "Generics array is required");
    ResolvableType[] resolvableGenerics = new ResolvableType[generics.length];
    for (int i = 0; i < generics.length; i++) {
      resolvableGenerics[i] = forClass(generics[i]);
    }
    return forClassWithGenerics(clazz, resolvableGenerics);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Class} with pre-declared generics.
   *
   * @param clazz the class (or interface) to introspect
   * @param generics the generics of the class
   * @return a {@link ResolvableType} for the specific class and generics
   * @see #forClassWithGenerics(Class, Class...)
   */
  public static ResolvableType forClassWithGenerics(Class<?> clazz, ResolvableType... generics) {
    Assert.notNull(clazz, "Class is required");
    Assert.notNull(generics, "Generics array is required");
    TypeVariable<?>[] variables = clazz.getTypeParameters();
    if (variables.length != generics.length) {
      throw new IllegalArgumentException("Mismatched number of generics specified for " + clazz.toGenericString());
    }

    Type[] arguments = new Type[generics.length];
    for (int i = 0; i < generics.length; i++) {
      ResolvableType generic = generics[i];
      Type argument = (generic != null ? generic.getType() : null);
      arguments[i] = (argument != null && !(argument instanceof TypeVariable) ? argument : variables[i]);
    }

    ParameterizedType syntheticType = new SyntheticParameterizedType(clazz, arguments);
    return forType(syntheticType, new TypeVariablesVariableResolver(variables, generics));
  }

  /**
   * Return a {@link ResolvableType} for the specified instance. The instance does not
   * convey generic information but if it implements {@link ResolvableTypeProvider} a
   * more precise {@link ResolvableType} can be used than the simple one based on
   * the {@link #forClass(Class) Class instance}.
   *
   * @param instance the instance (possibly {@code null})
   * @return a {@link ResolvableType} for the specified instance,
   * or {@code NONE} for {@code null}
   * @see ResolvableTypeProvider
   */
  public static ResolvableType forInstance(@Nullable Object instance) {
    if (instance == null) {
      return NONE;
    }
    if (instance instanceof ResolvableTypeProvider provider) {
      ResolvableType type = provider.getResolvableType();
      if (type != null) {
        return type;
      }
    }
    return ResolvableType.forClass(instance.getClass());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Field}.
   *
   * @param field the source field
   * @return a {@link ResolvableType} for the specified field
   * @see #forField(Field, Class)
   */
  public static ResolvableType forField(Field field) {
    Assert.notNull(field, "Field is required");
    return forType(null, new SerializableTypeWrapper.FieldTypeProvider(field), null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Field} with a given
   * implementation.
   * <p>Use this variant when the class that declares the field includes generic
   * parameter variables that are satisfied by the implementation class.
   *
   * @param field the source field
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified field
   * @see #forField(Field)
   */
  public static ResolvableType forField(Field field, Class<?> implementationClass) {
    Assert.notNull(field, "Field is required");
    ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
    return forType(null, new SerializableTypeWrapper.FieldTypeProvider(field), owner.asVariableResolver());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Field} with a given
   * implementation.
   * <p>Use this variant when the class that declares the field includes generic
   * parameter variables that are satisfied by the implementation type.
   *
   * @param field the source field
   * @param implementationType the implementation type
   * @return a {@link ResolvableType} for the specified field
   * @see #forField(Field)
   */
  public static ResolvableType forField(Field field, @Nullable ResolvableType implementationType) {
    Assert.notNull(field, "Field is required");
    ResolvableType owner = (implementationType != null ? implementationType : NONE);
    owner = owner.as(field.getDeclaringClass());
    return forType(null, new SerializableTypeWrapper.FieldTypeProvider(field), owner.asVariableResolver());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Field} with the
   * given nesting level.
   *
   * @param field the source field
   * @param nestingLevel the nesting level (1 for the outer level; 2 for a nested
   * generic type; etc)
   * @see #forField(Field)
   */
  public static ResolvableType forField(Field field, int nestingLevel) {
    Assert.notNull(field, "Field is required");
    return forType(null, new SerializableTypeWrapper.FieldTypeProvider(field), null).getNested(nestingLevel);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Field} with a given
   * implementation and the given nesting level.
   * <p>Use this variant when the class that declares the field includes generic
   * parameter variables that are satisfied by the implementation class.
   *
   * @param field the source field
   * @param nestingLevel the nesting level (1 for the outer level; 2 for a nested
   * generic type; etc)
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified field
   * @see #forField(Field)
   */
  public static ResolvableType forField(Field field, int nestingLevel, @Nullable Class<?> implementationClass) {
    Assert.notNull(field, "Field is required");
    ResolvableType owner = forType(implementationClass).as(field.getDeclaringClass());
    return forType(null, new SerializableTypeWrapper.FieldTypeProvider(field), owner.asVariableResolver()).getNested(nestingLevel);
  }

  /**
   * Return a {@link ResolvableType} as a array of the specified {@code componentType}.
   *
   * @param componentType the component type
   * @return a {@link ResolvableType} as an array of the specified component type
   */
  public static ResolvableType forArrayComponent(ResolvableType componentType) {
    Assert.notNull(componentType, "Component type is required");
    Class<?> arrayClass = Array.newInstance(componentType.resolve(), 0).getClass();
    return new ResolvableType(arrayClass, null, null, componentType);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type}.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @param type the source type (potentially {@code null})
   * @return a {@link ResolvableType} for the specified {@link Type}
   * @see #forType(Type, ResolvableType)
   */
  public static ResolvableType forType(@Nullable Type type) {
    return forType(type, null, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
   * owner type.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @param type the source type or {@code null}
   * @param owner the owner type used to resolve variables
   * @return a {@link ResolvableType} for the specified {@link Type} and owner
   * @see #forType(Type)
   */
  public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
    VariableResolver variableResolver = null;
    if (owner != null) {
      variableResolver = owner.asVariableResolver();
    }
    return forType(type, variableResolver);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @param typeReference the reference to obtain the source type from
   * @return a {@link ResolvableType} for the specified {@link ParameterizedTypeReference}
   * @see #forType(Type)
   */
  public static ResolvableType forType(ParameterizedTypeReference<?> typeReference) {
    return forType(typeReference.getType(), null, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
   * {@link VariableResolver}.
   *
   * @param type the source type or {@code null}
   * @param variableResolver the variable resolver or {@code null}
   * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
   */
  public static ResolvableType forType(@Nullable Type type, @Nullable VariableResolver variableResolver) {
    return forType(type, null, variableResolver);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
   * {@link VariableResolver}.
   *
   * @param type the source type or {@code null}
   * @param typeProvider the type provider or {@code null}
   * @param variableResolver the variable resolver or {@code null}
   * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
   */
  public static ResolvableType forType(@Nullable Type type,
          @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

    if (type == null && typeProvider != null) {
      type = SerializableTypeWrapper.forTypeProvider(typeProvider);
    }
    if (type == null) {
      return NONE;
    }

    // For simple Class references, build the wrapper right away -
    // no expensive resolution necessary, so not worth caching...
    if (type instanceof Class) {
      return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
    }

    // Purge empty entries on access since we don't have a clean-up thread or the like.
    cache.purgeUnreferencedEntries();

    // Check the cache - we may have a ResolvableType which has been resolved before...
    ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
    ResolvableType cachedType = cache.get(resultType);
    if (cachedType == null) {
      cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
      cache.put(cachedType, cachedType);
    }
    resultType.resolved = cachedType.resolved;
    return resultType;
  }

  /**
   * Clear the internal {@code ResolvableType}/{@code SerializableTypeWrapper} cache.
   */
  public static void clearCache() {
    cache.clear();
    SerializableTypeWrapper.cache.clear();
  }

  /**
   * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
   */
  public interface VariableResolver extends Serializable {

    /**
     * Return the source of the resolver (used for hashCode and equals).
     */
    Object getSource();

    /**
     * Resolve the specified variable.
     *
     * @param variable the variable to resolve
     * @return the resolved variable, or {@code null} if not found
     */
    @Nullable
    ResolvableType resolveVariable(TypeVariable<?> variable);
  }

  static class DefaultVariableResolver implements VariableResolver {
    @Serial
    private static final long serialVersionUID = 1L;

    final ResolvableType source;

    DefaultVariableResolver(ResolvableType resolvableType) {
      this.source = resolvableType;
    }

    @Nullable
    @Override
    public ResolvableType resolveVariable(TypeVariable<?> variable) {
      return this.source.resolveVariable(variable);
    }

    @Override
    public Object getSource() {
      return this.source;
    }
  }

  record TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics)
          implements VariableResolver {

    @Override
    public ResolvableType resolveVariable(TypeVariable<?> variable) {
      TypeVariable<?> variableToCompare = SerializableTypeWrapper.unwrap(variable);
      TypeVariable<?>[] variables = this.variables;
      for (int i = 0; i < variables.length; i++) {
        TypeVariable<?> resolvedVariable = SerializableTypeWrapper.unwrap(variables[i]);
        if (Objects.equals(resolvedVariable, variableToCompare)) {
          return this.generics[i];
        }
      }
      return null;
    }

    @Override
    public Object getSource() {
      return this.generics;
    }
  }

  record SyntheticParameterizedType(Type rawType, Type[] typeArguments)
          implements ParameterizedType, Serializable {

    @Override
    public String getTypeName() {
      String typeName = this.rawType.getTypeName();
      if (this.typeArguments.length > 0) {
        StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
        for (Type argument : this.typeArguments) {
          stringJoiner.add(argument.getTypeName());
        }
        return typeName + stringJoiner;
      }
      return typeName;
    }

    @Nullable
    @Override
    public Type getOwnerType() {
      return null;
    }

    @Override
    public Type getRawType() {
      return this.rawType;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return this.typeArguments;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ParameterizedType otherType)) {
        return false;
      }
      return otherType.getOwnerType() == null
              && this.rawType.equals(otherType.getRawType())
              && Arrays.equals(this.typeArguments, otherType.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
      return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
    }

    @Override
    public String toString() {
      return getTypeName();
    }
  }

  /**
   * Internal helper to handle bounds from {@link WildcardType WildcardTypes}.
   */
  record WildcardBounds(ResolvableType.WildcardBounds.Kind kind, ResolvableType[] bounds) {

    /**
     * Internal constructor to create a new {@link WildcardBounds} instance.
     *
     * @param kind the kind of bounds
     * @param bounds the bounds
     * @see #get(ResolvableType)
     */
    WildcardBounds { }

    /**
     * Return {@code true} if this bounds is the same kind as the specified bounds.
     */
    public boolean isSameKind(WildcardBounds bounds) {
      return this.kind == bounds.kind;
    }

    /**
     * Return {@code true} if this bounds is assignable to all the specified types.
     *
     * @param types the types to test against
     * @return {@code true} if this bounds is assignable to all types
     */
    public boolean isAssignableFrom(ResolvableType... types) {
      for (ResolvableType bound : this.bounds) {
        for (ResolvableType type : types) {
          if (!isAssignable(bound, type)) {
            return false;
          }
        }
      }
      return true;
    }

    private boolean isAssignable(ResolvableType source, ResolvableType from) {
      return (this.kind == Kind.UPPER ? source.isAssignableFrom(from) : from.isAssignableFrom(source));
    }

    /**
     * Return the underlying bounds.
     */
    public ResolvableType[] getBounds() {
      return this.bounds;
    }

    /**
     * Get a {@link WildcardBounds} instance for the specified type, returning
     * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
     *
     * @param type the source type
     * @return a {@link WildcardBounds} instance or {@code null}
     */
    @Nullable
    public static WildcardBounds get(ResolvableType type) {
      ResolvableType resolveToWildcard = type;
      while (!(resolveToWildcard.getType() instanceof WildcardType)) {
        if (resolveToWildcard == NONE) {
          return null;
        }
        resolveToWildcard = resolveToWildcard.resolveType();
      }
      WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
      Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
      Type[] bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
      ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
      for (int i = 0; i < bounds.length; i++) {
        resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
      }
      return new WildcardBounds(boundsType, resolvableBounds);
    }

    /**
     * The various kinds of bounds.
     */
    enum Kind {UPPER, LOWER}
  }

  /**
   * Internal {@link Type} used to represent an empty value.
   */
  static class EmptyType implements Type, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    static final Type INSTANCE = new EmptyType();

    @Serial
    Object readResolve() {
      return INSTANCE;
    }
  }

}
