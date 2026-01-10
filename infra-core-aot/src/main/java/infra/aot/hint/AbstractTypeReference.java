/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aot.hint;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
