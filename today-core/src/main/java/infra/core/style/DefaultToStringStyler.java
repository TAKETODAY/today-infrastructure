/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.style;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * Default {@code toString()} styler.
 *
 * <p>This class is used by {@link ToStringBuilder} to style {@code toString()}
 * output in a consistent manner according to conventions.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultToStringStyler implements ToStringStyler {

  private final ValueStyler valueStyler;

  /**
   * Create a new DefaultToStringStyler.
   *
   * @param valueStyler the ValueStyler to use
   */
  public DefaultToStringStyler(ValueStyler valueStyler) {
    Assert.notNull(valueStyler, "ValueStyler is required");
    this.valueStyler = valueStyler;
  }

  /**
   * Return the ValueStyler used by this ToStringStyler.
   */
  protected final ValueStyler getValueStyler() {
    return this.valueStyler;
  }

  @Override
  public void styleStart(StringBuilder buffer, Object obj) {
    if (!obj.getClass().isArray()) {
      buffer.append('[').append(ClassUtils.getShortName(obj.getClass()));
      styleIdentityHashCode(buffer, obj);
    }
    else {
      buffer.append('[');
      styleIdentityHashCode(buffer, obj);
      buffer.append(' ');
      styleValue(buffer, obj);
    }
  }

  private void styleIdentityHashCode(StringBuilder buffer, Object obj) {
    buffer.append('@');
    buffer.append(ObjectUtils.getIdentityHexString(obj));
  }

  @Override
  public void styleEnd(StringBuilder buffer, Object o) {
    buffer.append(']');
  }

  @Override
  public void styleField(StringBuilder buffer, String fieldName, @Nullable Object value) {
    styleFieldStart(buffer, fieldName);
    styleValue(buffer, value);
    styleFieldEnd(buffer, fieldName);
  }

  protected void styleFieldStart(StringBuilder buffer, String fieldName) {
    buffer.append(' ').append(fieldName).append(" = ");
  }

  protected void styleFieldEnd(StringBuilder buffer, String fieldName) { }

  @Override
  public void styleValue(StringBuilder buffer, @Nullable Object value) {
    buffer.append(this.valueStyler.style(value));
  }

  @Override
  public void styleFieldSeparator(StringBuilder buffer) {
    buffer.append(',');
  }

}
