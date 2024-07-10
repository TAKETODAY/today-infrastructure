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

import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * Base {@link TypeReference} implementation that ensures consistent behaviour
 * for {@code equals()}, {@code hashCode()}, and {@code toString()} based on
 * the {@linkplain #getCanonicalName() canonical name}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractTypeReference implements TypeReference {

  private final String packageName;

  private final String simpleName;

  @Nullable
  private final TypeReference enclosingType;

  protected AbstractTypeReference(String packageName, String simpleName, @Nullable TypeReference enclosingType) {
    this.packageName = packageName;
    this.simpleName = simpleName;
    this.enclosingType = enclosingType;
  }

  @Override
  public String getName() {
    TypeReference enclosingType = getEnclosingType();
    String simpleName = getSimpleName();
    return (enclosingType != null ? (enclosingType.getName() + '$' + simpleName) :
            addPackageIfNecessary(simpleName));
  }

  @Override
  public String getPackageName() {
    return this.packageName;
  }

  @Override
  public String getSimpleName() {
    return this.simpleName;
  }

  @Nullable
  @Override
  public TypeReference getEnclosingType() {
    return this.enclosingType;
  }

  protected abstract boolean isPrimitive();

  @Override
  public int compareTo(TypeReference other) {
    return getCanonicalName().compareToIgnoreCase(other.getCanonicalName());
  }

  protected String addPackageIfNecessary(String part) {
    if (this.packageName.isEmpty() ||
            this.packageName.equals("java.lang") && isPrimitive()) {
      return part;
    }
    return this.packageName + '.' + part;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCanonicalName());
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TypeReference otherReference)) {
      return false;
    }
    return getCanonicalName().equals(otherReference.getCanonicalName());
  }

  @Override
  public String toString() {
    return getCanonicalName();
  }

}
