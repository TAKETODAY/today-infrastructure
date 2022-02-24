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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans;

import java.beans.PropertyChangeEvent;

import cn.taketoday.lang.Nullable;

/**
 * Superclass for exceptions related to a property access,
 * such as type mismatch or invocation target exception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:34
 */
@SuppressWarnings("serial")
public abstract class PropertyAccessException extends BeansException {

  @Nullable
  private final PropertyChangeEvent propertyChangeEvent;

  /**
   * Create a new PropertyAccessException.
   *
   * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
   * @param msg the detail message
   * @param cause the root cause
   */
  public PropertyAccessException(@Nullable PropertyChangeEvent propertyChangeEvent, String msg, @Nullable Throwable cause) {
    super(msg, cause);
    this.propertyChangeEvent = propertyChangeEvent;
  }

  /**
   * Create a new PropertyAccessException without PropertyChangeEvent.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public PropertyAccessException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
    this.propertyChangeEvent = null;
  }

  /**
   * Return the PropertyChangeEvent that resulted in the problem.
   * <p>May be {@code null}; only available if an actual bean property
   * was affected.
   */
  @Nullable
  public PropertyChangeEvent getPropertyChangeEvent() {
    return this.propertyChangeEvent;
  }

  /**
   * Return the name of the affected property, if available.
   */
  @Nullable
  public String getPropertyName() {
    return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getPropertyName() : null);
  }

  /**
   * Return the affected value that was about to be set, if any.
   */
  @Nullable
  public Object getValue() {
    return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getNewValue() : null);
  }

  /**
   * Return a corresponding error code for this type of exception.
   */
  public abstract String getErrorCode();

}
