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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;

import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Object representing the override of a method on a managed object by the IoC
 * container.
 *
 * <p>Note that the override mechanism is <em>not</em> intended as a generic
 * means of inserting crosscutting code: use AOP for that.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 12:42
 */
public abstract class MethodOverride implements BeanMetadataElement {

  private final String methodName;

  private boolean overloaded = true;

  @Nullable
  private Object source;

  /**
   * Construct a new override for the given method.
   *
   * @param methodName the name of the method to override
   */
  protected MethodOverride(String methodName) {
    Assert.notNull(methodName, "Method name must not be null");
    this.methodName = methodName;
  }

  /**
   * Return the name of the method to be overridden.
   */
  public String getMethodName() {
    return this.methodName;
  }

  /**
   * Set whether the overridden method is <em>overloaded</em> (i.e., whether argument
   * type matching needs to occur to disambiguate methods of the same name).
   * <p>Default is {@code true}; can be switched to {@code false} to optimize
   * runtime performance.
   */
  protected void setOverloaded(boolean overloaded) {
    this.overloaded = overloaded;
  }

  /**
   * Return whether the overridden method is <em>overloaded</em> (i.e., whether argument
   * type matching needs to occur to disambiguate methods of the same name).
   */
  protected boolean isOverloaded() {
    return this.overloaded;
  }

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

  /**
   * Subclasses must override this to indicate whether they <em>match</em> the
   * given method. This allows for argument list checking as well as method
   * name checking.
   *
   * @param method the method to check
   * @return whether this override matches the given method
   */
  public abstract boolean matches(Method method);

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MethodOverride that)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(this.methodName, that.methodName) &&
            ObjectUtils.nullSafeEquals(this.source, that.source));
  }

  @Override
  public int hashCode() {
    int hashCode = ObjectUtils.nullSafeHashCode(this.methodName);
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.source);
    return hashCode;
  }

}
