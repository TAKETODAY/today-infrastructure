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

package infra.aot.generate;

import infra.aot.hint.AbstractTypeReference;
import infra.aot.hint.TypeReference;
import infra.javapoet.ClassName;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A {@link TypeReference} for a generated {@linkplain ClassName type}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public final class GeneratedTypeReference extends AbstractTypeReference {

  private final ClassName className;

  private GeneratedTypeReference(ClassName className) {
    super(className.packageName(), className.simpleName(), safeCreate(className.enclosingClassName()));
    this.className = className;
  }

  @Nullable
  private static GeneratedTypeReference safeCreate(@Nullable ClassName className) {
    return (className != null ? new GeneratedTypeReference(className) : null);
  }

  public static GeneratedTypeReference of(ClassName className) {
    Assert.notNull(className, "ClassName is required");
    return new GeneratedTypeReference(className);
  }

  @Override
  public String getCanonicalName() {
    return this.className.canonicalName();
  }

  @Override
  protected boolean isPrimitive() {
    return this.className.isPrimitive();
  }

}
