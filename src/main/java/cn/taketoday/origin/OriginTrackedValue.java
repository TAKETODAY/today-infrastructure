/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.origin;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * A wrapper for an {@link Object} value and {@link Origin}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #of(Object)
 * @see #of(Object, Origin)
 * @since 4.0
 */
public class OriginTrackedValue implements OriginProvider {

  @Nullable
  private final Object value;

  @Nullable
  private final Origin origin;

  private OriginTrackedValue(Object value, @Nullable Origin origin) {
    this.value = value;
    this.origin = origin;
  }

  /**
   * Return the tracked value.
   *
   * @return the tracked value
   */
  public Object getValue() {
    return this.value;
  }

  @Override
  @Nullable
  public Origin getOrigin() {
    return this.origin;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    return ObjectUtils.nullSafeEquals(this.value, ((OriginTrackedValue) obj).value);
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.value);
  }

  @Override
  @Nullable
  public String toString() {
    return value != null ? this.value.toString() : null;
  }

  @Nullable
  public static OriginTrackedValue of(Object value) {
    return of(value, null);
  }

  /**
   * Create an {@link OriginTrackedValue} containing the specified {@code value} and
   * {@code origin}. If the source value implements {@link CharSequence} then so will
   * the resulting {@link OriginTrackedValue}.
   *
   * @param value the source value
   * @param origin the origin
   * @return an {@link OriginTrackedValue} or {@code null} if the source value was
   * {@code null}.
   */
  @Nullable
  public static OriginTrackedValue of(@Nullable Object value, @Nullable Origin origin) {
    if (value == null) {
      return null;
    }
    if (value instanceof CharSequence) {
      return new OriginTrackedCharSequence((CharSequence) value, origin);
    }
    return new OriginTrackedValue(value, origin);
  }

  /**
   * {@link OriginTrackedValue} for a {@link CharSequence}.
   */
  private static class OriginTrackedCharSequence extends OriginTrackedValue implements CharSequence {

    OriginTrackedCharSequence(CharSequence value, @Nullable Origin origin) {
      super(value, origin);
    }

    @Override
    public int length() {
      return getValue().length();
    }

    @Override
    public char charAt(int index) {
      return getValue().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return getValue().subSequence(start, end);
    }

    @Override
    public CharSequence getValue() {
      return (CharSequence) super.getValue();
    }

  }

}
