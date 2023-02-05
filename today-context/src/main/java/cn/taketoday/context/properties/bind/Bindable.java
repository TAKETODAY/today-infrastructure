/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * Source that can be bound by a {@link Binder}.
 *
 * @param <T> the source type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Bindable#of(Class)
 * @see Bindable#of(ResolvableType)
 * @since 4.0
 */
public final class Bindable<T> {

  private static final EnumSet<BindRestriction> NO_BIND_RESTRICTIONS = EnumSet.noneOf(BindRestriction.class);

  private final ResolvableType type;

  private final ResolvableType boxedType;

  @Nullable
  private final Supplier<T> value;

  private final Annotation[] annotations;

  private final EnumSet<BindRestriction> bindRestrictions;

  private Bindable(
          ResolvableType type,
          ResolvableType boxedType,
          @Nullable Supplier<T> value,
          Annotation[] annotations,
          EnumSet<BindRestriction> bindRestrictions) {
    this.type = type;
    this.boxedType = boxedType;
    this.value = value;
    this.annotations = annotations;
    this.bindRestrictions = bindRestrictions;
  }

  /**
   * Return the type of the item to bind.
   *
   * @return the type being bound
   */
  public ResolvableType getType() {
    return this.type;
  }

  /**
   * Return the boxed type of the item to bind.
   *
   * @return the boxed type for the item being bound
   */
  public ResolvableType getBoxedType() {
    return this.boxedType;
  }

  /**
   * Return a supplier that provides the object value or {@code null}.
   *
   * @return the value or {@code null}
   */
  @Nullable
  public Supplier<T> getValue() {
    return this.value;
  }

  /**
   * Return any associated annotations that could affect binding.
   *
   * @return the associated annotations
   */
  public Annotation[] getAnnotations() {
    return this.annotations;
  }

  /**
   * Return a single associated annotations that could affect binding.
   *
   * @param <A> the annotation type
   * @param type annotation type
   * @return the associated annotation or {@code null}
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <A extends Annotation> A getAnnotation(Class<A> type) {
    for (Annotation annotation : this.annotations) {
      if (type.isInstance(annotation)) {
        return (A) annotation;
      }
    }
    return null;
  }

  /**
   * Returns {@code true} if the specified bind restriction has been added.
   *
   * @param bindRestriction the bind restriction to check
   * @return if the bind restriction has been added
   */
  public boolean hasBindRestriction(BindRestriction bindRestriction) {
    return this.bindRestrictions.contains(bindRestriction);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Bindable<?> other = (Bindable<?>) obj;
    return Objects.equals(type.resolve(), other.type.resolve())
            && Objects.equals(this.bindRestrictions, other.bindRestrictions)
            && ObjectUtils.nullSafeEquals(this.annotations, other.annotations);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
    result = prime * result + ObjectUtils.nullSafeHashCode(this.annotations);
    result = prime * result + ObjectUtils.nullSafeHashCode(this.bindRestrictions);
    return result;
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    creator.append("type", this.type);
    creator.append("value", value != null ? "provided" : "none");
    creator.append("annotations", this.annotations);
    return creator.toString();
  }

  /**
   * Create an updated {@link Bindable} instance with the specified annotations.
   *
   * @param annotations the annotations
   * @return an updated {@link Bindable}
   */
  public Bindable<T> withAnnotations(@Nullable Annotation... annotations) {
    return new Bindable<>(this.type, this.boxedType, this.value,
            annotations != null ? annotations : Constant.EMPTY_ANNOTATIONS, NO_BIND_RESTRICTIONS);
  }

  /**
   * Create an updated {@link Bindable} instance with an existing value.
   *
   * @param existingValue the existing value
   * @return an updated {@link Bindable}
   */
  public Bindable<T> withExistingValue(@Nullable T existingValue) {
    if (!(existingValue == null || this.type.isArray() || boxedType.resolve().isInstance(existingValue))) {
      throw new IllegalArgumentException("ExistingValue must be an instance of " + this.type);
    }
    Supplier<T> value = existingValue != null ? SingletonSupplier.valueOf(existingValue) : null;
    return new Bindable<>(this.type, this.boxedType, value, this.annotations, this.bindRestrictions);
  }

  /**
   * Create an updated {@link Bindable} instance with a value supplier.
   *
   * @param suppliedValue the supplier for the value
   * @return an updated {@link Bindable}
   */
  public Bindable<T> withSuppliedValue(Supplier<T> suppliedValue) {
    return new Bindable<>(this.type, this.boxedType, suppliedValue, this.annotations, this.bindRestrictions);
  }

  /**
   * Create an updated {@link Bindable} instance with additional bind restrictions.
   *
   * @param additionalRestrictions any additional restrictions to apply
   * @return an updated {@link Bindable}
   */
  public Bindable<T> withBindRestrictions(BindRestriction... additionalRestrictions) {
    EnumSet<BindRestriction> bindRestrictions = EnumSet.copyOf(this.bindRestrictions);
    CollectionUtils.addAll(bindRestrictions, additionalRestrictions);
    return new Bindable<>(this.type, this.boxedType, this.value, this.annotations, bindRestrictions);
  }

  /**
   * Create a new {@link Bindable} of the type of the specified instance with an
   * existing value equal to the instance.
   *
   * @param <T> the source type
   * @param instance the instance (must not be {@code null})
   * @return a {@link Bindable} instance
   * @see #of(ResolvableType)
   * @see #withExistingValue(Object)
   */
  @SuppressWarnings("unchecked")
  public static <T> Bindable<T> ofInstance(T instance) {
    Assert.notNull(instance, "Instance must not be null");
    Class<T> type = (Class<T>) instance.getClass();
    return of(type).withExistingValue(instance);
  }

  /**
   * Create a new {@link Bindable} of the specified type.
   *
   * @param <T> the source type
   * @param type the type (must not be {@code null})
   * @return a {@link Bindable} instance
   * @see #of(ResolvableType)
   */
  public static <T> Bindable<T> of(Class<T> type) {
    Assert.notNull(type, "Type must not be null");
    return of(ResolvableType.fromClass(type));
  }

  /**
   * Create a new {@link Bindable} {@link List} of the specified element type.
   *
   * @param <E> the element type
   * @param elementType the list element type
   * @return a {@link Bindable} instance
   */
  public static <E> Bindable<List<E>> listOf(Class<E> elementType) {
    return of(ResolvableType.fromClassWithGenerics(List.class, elementType));
  }

  /**
   * Create a new {@link Bindable} {@link Set} of the specified element type.
   *
   * @param <E> the element type
   * @param elementType the set element type
   * @return a {@link Bindable} instance
   */
  public static <E> Bindable<Set<E>> setOf(Class<E> elementType) {
    return of(ResolvableType.fromClassWithGenerics(Set.class, elementType));
  }

  /**
   * Create a new {@link Bindable} {@link Map} of the specified key and value type.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param keyType the map key type
   * @param valueType the map value type
   * @return a {@link Bindable} instance
   */
  public static <K, V> Bindable<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
    return of(ResolvableType.fromClassWithGenerics(Map.class, keyType, valueType));
  }

  /**
   * Create a new {@link Bindable} of the specified type.
   *
   * @param <T> the source type
   * @param type the type (must not be {@code null})
   * @return a {@link Bindable} instance
   * @see #of(Class)
   */
  public static <T> Bindable<T> of(ResolvableType type) {
    Assert.notNull(type, "Type must not be null");
    ResolvableType boxedType = box(type);
    return new Bindable<>(type, boxedType, null, Constant.EMPTY_ANNOTATIONS, NO_BIND_RESTRICTIONS);
  }

  private static ResolvableType box(ResolvableType type) {
    Class<?> resolved = type.resolve();
    if (resolved != null && resolved.isPrimitive()) {
      Object array = Array.newInstance(resolved, 1);
      Class<?> wrapperType = Array.get(array, 0).getClass();
      return ResolvableType.fromClass(wrapperType);
    }
    if (resolved != null && resolved.isArray()) {
      return ResolvableType.fromArrayComponent(box(type.getComponentType()));
    }
    return type;
  }

  /**
   * Restrictions that can be applied when binding values.
   */
  public enum BindRestriction {

    /**
     * Do not bind direct {@link ConfigurationProperty} matches.
     */
    NO_DIRECT_PROPERTY

  }

}
