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

import java.util.Objects;
import java.util.ResourceBundle;

import cn.taketoday.lang.Nullable;

/**
 * A hint that describes the need to access a {@link ResourceBundle}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 4.0
 */
public final class ResourceBundleHint implements ConditionalHint {

  private final String baseName;

  @Nullable
  private final TypeReference reachableType;

  ResourceBundleHint(Builder builder) {
    this.baseName = builder.baseName;
    this.reachableType = builder.reachableType;
  }

  /**
   * Return the {@code baseName} of the resource bundle.
   *
   * @return the base name
   */
  public String getBaseName() {
    return this.baseName;
  }

  @Nullable
  @Override
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
    ResourceBundleHint that = (ResourceBundleHint) o;
    return this.baseName.equals(that.baseName)
            && Objects.equals(this.reachableType, that.reachableType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.baseName, this.reachableType);
  }

  /**
   * Builder for {@link ResourceBundleHint}.
   */
  public static class Builder {

    private String baseName;

    @Nullable
    private TypeReference reachableType;

    Builder(String baseName) {
      this.baseName = baseName;
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
     * Use the {@code baseName} of the resource bundle.
     *
     * @return {@code this}, to facilitate method chaining
     */
    public Builder baseName(String baseName) {
      this.baseName = baseName;
      return this;
    }

    /**
     * Creates a {@link ResourceBundleHint} based on the state of this
     * builder.
     *
     * @return a resource bundle hint
     */
    ResourceBundleHint build() {
      return new ResourceBundleHint(this);
    }

  }

}
