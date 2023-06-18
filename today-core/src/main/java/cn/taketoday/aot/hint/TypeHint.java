/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A hint that describes the need for reflection on a type.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public final class TypeHint implements ConditionalHint {

  private final TypeReference type;

  @Nullable
  private final TypeReference reachableType;

  private final Set<FieldHint> fields;

  private final Set<ExecutableHint> constructors;

  private final Set<ExecutableHint> methods;

  private final Set<MemberCategory> memberCategories;

  private TypeHint(Builder builder) {
    this.type = builder.type;
    this.reachableType = builder.reachableType;
    this.memberCategories = Set.copyOf(builder.memberCategories);
    this.fields = builder.fields.stream().map(FieldHint::new).collect(Collectors.toSet());
    this.constructors = builder.constructors.values().stream().map(ExecutableHint.Builder::build).collect(Collectors.toSet());
    this.methods = builder.methods.values().stream().map(ExecutableHint.Builder::build).collect(Collectors.toSet());
  }

  /**
   * Initialize a builder for the type defined by the specified
   * {@link TypeReference}.
   *
   * @param type the type to use
   * @return a builder
   */
  static Builder of(TypeReference type) {
    Assert.notNull(type, "'type' must not be null");
    return new Builder(type);
  }

  /**
   * Return the type that this hint handles.
   *
   * @return the type
   */
  public TypeReference getType() {
    return this.type;
  }

  @Nullable
  @Override
  public TypeReference getReachableType() {
    return this.reachableType;
  }

  /**
   * Return the fields that require reflection.
   *
   * @return a stream of {@link FieldHint}
   */
  public Stream<FieldHint> fields() {
    return this.fields.stream();
  }

  /**
   * Return the constructors that require reflection.
   *
   * @return a stream of {@link ExecutableHint}
   */
  public Stream<ExecutableHint> constructors() {
    return this.constructors.stream();
  }

  /**
   * Return the methods that require reflection.
   *
   * @return a stream of {@link ExecutableHint}
   */
  public Stream<ExecutableHint> methods() {
    return this.methods.stream();
  }

  /**
   * Return the member categories that apply.
   *
   * @return the member categories to enable
   */
  public Set<MemberCategory> getMemberCategories() {
    return this.memberCategories;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TypeHint.class.getSimpleName() + "[", "]")
            .add("type=" + this.type)
            .toString();
  }

  /**
   * Return a {@link Consumer} that applies the given {@link MemberCategory
   * MemberCategories} to the accepted {@link Builder}.
   *
   * @param memberCategories the memberCategories to apply
   * @return a consumer to apply the member categories
   */
  public static Consumer<Builder> builtWith(MemberCategory... memberCategories) {
    return builder -> builder.withMembers(memberCategories);
  }

  /**
   * Builder for {@link TypeHint}.
   */
  public static class Builder {

    private final TypeReference type;

    @Nullable
    private TypeReference reachableType;

    private final Set<String> fields = new HashSet<>();

    private final Map<ExecutableKey, ExecutableHint.Builder> constructors = new HashMap<>();

    private final Map<ExecutableKey, ExecutableHint.Builder> methods = new HashMap<>();

    private final Set<MemberCategory> memberCategories = new HashSet<>();

    Builder(TypeReference type) {
      this.type = type;
    }

    /**
     * Make this hint conditional on the fact that the specified type
     * is in a reachable code path from a static analysis point of view.
     *
     * @param reachableType the type that should be reachable for this
     * hint to apply
     * @return {@code this}, to facilitate method chaining
     */
    public Builder onReachableType(TypeReference reachableType) {
      this.reachableType = reachableType;
      return this;
    }

    /**
     * Make this hint conditional on the fact that the specified type
     * is in a reachable code path from a static analysis point of view.
     *
     * @param reachableType the type that should be reachable for this
     * hint to apply
     * @return {@code this}, to facilitate method chaining
     */
    public Builder onReachableType(Class<?> reachableType) {
      this.reachableType = TypeReference.of(reachableType);
      return this;
    }

    /**
     * Register the need for reflection on the field with the specified name.
     *
     * @param name the name of the field
     * @return {@code this}, to facilitate method chaining
     */
    public Builder withField(String name) {
      this.fields.add(name);
      return this;
    }

    /**
     * Register the need for reflection on the constructor with the specified
     * parameter types, using the specified {@link ExecutableMode}.
     *
     * @param parameterTypes the parameter types of the constructor
     * @param mode the requested mode
     * @return {@code this}, to facilitate method chaining
     */
    public Builder withConstructor(List<TypeReference> parameterTypes, ExecutableMode mode) {
      return withConstructor(parameterTypes, ExecutableHint.builtWith(mode));
    }

    /**
     * Register the need for reflection on the constructor with the specified
     * parameter types.
     *
     * @param parameterTypes the parameter types of the constructor
     * @param constructorHint a builder to further customize the hints of this
     * constructor
     * @return {@code this}, to facilitate method chaining
     */
    private Builder withConstructor(List<TypeReference> parameterTypes,
            Consumer<ExecutableHint.Builder> constructorHint) {
      ExecutableKey key = new ExecutableKey("<init>", parameterTypes);
      ExecutableHint.Builder builder = this.constructors.computeIfAbsent(key,
              k -> ExecutableHint.ofConstructor(parameterTypes));
      constructorHint.accept(builder);
      return this;
    }

    /**
     * Register the need for reflection on the method with the specified name
     * and parameter types, using the specified {@link ExecutableMode}.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter types of the constructor
     * @param mode the requested mode
     * @return {@code this}, to facilitate method chaining
     */
    public Builder withMethod(String name, List<TypeReference> parameterTypes, ExecutableMode mode) {
      return withMethod(name, parameterTypes, ExecutableHint.builtWith(mode));
    }

    /**
     * Register the need for reflection on the method with the specified name
     * and parameter types.
     *
     * @param name the name of the method
     * @param parameterTypes the parameter types of the constructor
     * @param methodHint a builder to further customize the hints of this method
     * @return {@code this}, to facilitate method chaining
     */
    private Builder withMethod(String name, List<TypeReference> parameterTypes,
            Consumer<ExecutableHint.Builder> methodHint) {
      ExecutableKey key = new ExecutableKey(name, parameterTypes);
      ExecutableHint.Builder builder = this.methods.computeIfAbsent(key,
              k -> ExecutableHint.ofMethod(name, parameterTypes));
      methodHint.accept(builder);
      return this;
    }

    /**
     * Adds the specified {@linkplain MemberCategory member categories}.
     *
     * @param memberCategories the categories to apply
     * @return {@code this}, to facilitate method chaining
     * @see TypeHint#builtWith(MemberCategory...)
     */
    public Builder withMembers(MemberCategory... memberCategories) {
      this.memberCategories.addAll(Arrays.asList(memberCategories));
      return this;
    }

    /**
     * Create a {@link TypeHint} based on the state of this builder.
     *
     * @return a type hint
     */
    TypeHint build() {
      return new TypeHint(this);
    }

  }

  private static final class ExecutableKey {

    private final String name;

    private final List<String> parameterTypes;

    private ExecutableKey(String name, List<TypeReference> parameterTypes) {
      this.name = name;
      this.parameterTypes = parameterTypes.stream().map(TypeReference::getCanonicalName).toList();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ExecutableKey that = (ExecutableKey) o;
      return this.name.equals(that.name) && this.parameterTypes.equals(that.parameterTypes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.parameterTypes);
    }

  }

}
