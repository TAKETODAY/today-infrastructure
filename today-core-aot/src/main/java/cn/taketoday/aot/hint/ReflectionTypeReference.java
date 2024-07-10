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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A {@link TypeReference} based on a {@link Class}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 4.0
 */
final class ReflectionTypeReference extends AbstractTypeReference {

  private final Class<?> type;

  private ReflectionTypeReference(Class<?> type) {
    super(type.getPackageName(), type.getSimpleName(), getEnclosingClass(type));
    this.type = type;
  }

  @Nullable
  private static TypeReference getEnclosingClass(Class<?> type) {
    Class<?> candidate = (type.isArray() ? type.getComponentType().getEnclosingClass() :
                          type.getEnclosingClass());
    return (candidate != null ? new ReflectionTypeReference(candidate) : null);
  }

  static ReflectionTypeReference of(Class<?> type) {
    Assert.notNull(type, "'type' is required");
    Assert.notNull(type.getCanonicalName(), "'type.getCanonicalName()' is required");
    return new ReflectionTypeReference(type);
  }

  @Override
  public String getCanonicalName() {
    return this.type.getCanonicalName();
  }

  @Override
  protected boolean isPrimitive() {
    return this.type.isPrimitive() ||
            (this.type.isArray() && this.type.getComponentType().isPrimitive());
  }

}
