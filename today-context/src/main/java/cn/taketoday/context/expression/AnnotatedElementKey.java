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

package cn.taketoday.context.expression;

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Represent an {@link AnnotatedElement} on a particular {@link Class}
 * and is suitable as a key.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CachedExpressionEvaluator
 * @since 4.0
 */
public final class AnnotatedElementKey implements Comparable<AnnotatedElementKey> {

  private final AnnotatedElement element;

  @Nullable
  private final Class<?> targetClass;

  /**
   * Create a new instance with the specified {@link AnnotatedElement} and
   * optional target {@link Class}.
   */
  public AnnotatedElementKey(AnnotatedElement element, @Nullable Class<?> targetClass) {
    Assert.notNull(element, "AnnotatedElement is required");
    this.element = element;
    this.targetClass = targetClass;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AnnotatedElementKey otherKey)) {
      return false;
    }
    return (this.element.equals(otherKey.element) &&
            ObjectUtils.nullSafeEquals(this.targetClass, otherKey.targetClass));
  }

  @Override
  public int hashCode() {
    return this.element.hashCode() + (this.targetClass != null ? this.targetClass.hashCode() * 29 : 0);
  }

  @Override
  public String toString() {
    return this.element + (this.targetClass != null ? " on " + this.targetClass : "");
  }

  @Override
  public int compareTo(AnnotatedElementKey other) {
    int result = this.element.toString().compareTo(other.element.toString());
    if (result == 0 && this.targetClass != null) {
      if (other.targetClass == null) {
        return 1;
      }
      result = this.targetClass.getName().compareTo(other.targetClass.getName());
    }
    return result;
  }

}
