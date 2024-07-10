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

import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * A hint that describes the need for Java serialization at runtime.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JavaSerializationHint implements ConditionalHint {

  private final TypeReference type;

  @Nullable
  private final TypeReference reachableType;

  JavaSerializationHint(Builder builder) {
    this.type = builder.type;
    this.reachableType = builder.reachableType;
  }

  /**
   * Return the {@link TypeReference type} that needs to be serialized using
   * Java serialization at runtime.
   *
   * @return a {@link Serializable} type
   */
  public TypeReference getType() {
    return this.type;
  }

  @Override
  @Nullable
  public TypeReference getReachableType() {
    return this.reachableType;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JavaSerializationHint that = (JavaSerializationHint) o;
    return this.type.equals(that.type)
            && Objects.equals(this.reachableType, that.reachableType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.type, this.reachableType);
  }

  /**
   * Builder for {@link JavaSerializationHint}.
   */
  public static class Builder {

    private final TypeReference type;

    @Nullable
    private TypeReference reachableType;

    Builder(TypeReference type) {
      this.type = type;
    }

    /**
     * Make this hint conditional on the fact that the specified type
     * can be resolved.
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
     * Create a {@link JavaSerializationHint} based on the state of this builder.
     *
     * @return a java serialization hint
     */
    JavaSerializationHint build() {
      return new JavaSerializationHint(this);
    }

  }
}
