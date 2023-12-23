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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A hint that describes the need for reflection on a {@link Method} or
 * {@link Constructor}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ExecutableHint extends MemberHint implements Comparable<ExecutableHint> {

  private final List<TypeReference> parameterTypes;

  private final ExecutableMode mode;

  private ExecutableHint(Builder builder) {
    super(builder.name);
    this.parameterTypes = List.copyOf(builder.parameterTypes);
    this.mode = (builder.mode != null ? builder.mode : ExecutableMode.INVOKE);
  }

  /**
   * Initialize a builder with the parameter types of a constructor.
   *
   * @param parameterTypes the parameter types of the constructor
   * @return a builder
   */
  static Builder ofConstructor(List<TypeReference> parameterTypes) {
    return new Builder("<init>", parameterTypes);
  }

  /**
   * Initialize a builder with the name and parameter types of a method.
   *
   * @param name the name of the method
   * @param parameterTypes the parameter types of the method
   * @return a builder
   */
  static Builder ofMethod(String name, List<TypeReference> parameterTypes) {
    return new Builder(name, parameterTypes);
  }

  /**
   * Return the parameter types of the executable.
   *
   * @return the parameter types
   * @see Executable#getParameterTypes()
   */
  public List<TypeReference> getParameterTypes() {
    return this.parameterTypes;
  }

  /**
   * Return the {@linkplain ExecutableMode mode} that applies to this hint.
   *
   * @return the mode
   */
  public ExecutableMode getMode() {
    return this.mode;
  }

  /**
   * Return a {@link Consumer} that applies the given {@link ExecutableMode}
   * to the accepted {@link Builder}.
   *
   * @param mode the mode to apply
   * @return a consumer to apply the mode
   */
  public static Consumer<Builder> builtWith(ExecutableMode mode) {
    return builder -> builder.withMode(mode);
  }

  @Override
  public int compareTo(ExecutableHint other) {
    return Comparator.comparing(ExecutableHint::getName, String::compareToIgnoreCase)
            .thenComparing(ExecutableHint::getParameterTypes, Comparator.comparingInt(List::size))
            .thenComparing(ExecutableHint::getParameterTypes, (params1, params2) -> {
              String left = params1.stream().map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
              String right = params2.stream().map(TypeReference::getCanonicalName).collect(Collectors.joining(","));
              return left.compareTo(right);
            }).compare(this, other);
  }

  /**
   * Builder for {@link ExecutableHint}.
   */
  public static class Builder {

    private final String name;

    private final List<TypeReference> parameterTypes;

    @Nullable
    private ExecutableMode mode;

    Builder(String name, List<TypeReference> parameterTypes) {
      this.name = name;
      this.parameterTypes = parameterTypes;
    }

    /**
     * Specify that the {@linkplain ExecutableMode mode} is required.
     *
     * @param mode the required mode
     * @return {@code this}, to facilitate method chaining
     */
    public Builder withMode(ExecutableMode mode) {
      Assert.notNull(mode, "'mode' is required");
      if ((this.mode == null) || !this.mode.includes(mode)) {
        this.mode = mode;
      }
      return this;
    }

    /**
     * Create an {@link ExecutableHint} based on the state of this builder.
     *
     * @return an executable hint
     */
    ExecutableHint build() {
      return new ExecutableHint(this);
    }

  }

}
