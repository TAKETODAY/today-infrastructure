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

package infra.validation;

import infra.beans.BeanWrapper;
import infra.beans.ConfigurablePropertyAccessor;
import infra.lang.Nullable;

/**
 * Special implementation of the Errors and BindingResult interfaces,
 * supporting registration and evaluation of binding errors on value objects.
 * Performs direct field access instead of going through JavaBean getters.
 *
 * <p>this implementation is able to traverse nested fields.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initDirectFieldAccess()
 * @see BeanPropertyBindingResult
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {

  @Nullable
  private final Object target;

  private final boolean autoGrowNestedPaths;

  @Nullable
  private transient ConfigurablePropertyAccessor directFieldAccessor;

  /**
   * Create a new DirectFieldBindingResult instance.
   *
   * @param target the target object to bind onto
   * @param objectName the name of the target object
   */
  public DirectFieldBindingResult(@Nullable Object target, String objectName) {
    this(target, objectName, true);
  }

  /**
   * Create a new DirectFieldBindingResult instance.
   *
   * @param target the target object to bind onto
   * @param objectName the name of the target object
   * @param autoGrowNestedPaths whether to "auto-grow" a nested path that contains a null value
   */
  public DirectFieldBindingResult(@Nullable Object target, String objectName, boolean autoGrowNestedPaths) {
    super(objectName);
    this.target = target;
    this.autoGrowNestedPaths = autoGrowNestedPaths;
  }

  @Override
  @Nullable
  public final Object getTarget() {
    return this.target;
  }

  /**
   * Returns the DirectFieldAccessor that this instance uses.
   * Creates a new one if none existed before.
   *
   * @see #createDirectFieldAccessor()
   */
  @Override
  public final ConfigurablePropertyAccessor getPropertyAccessor() {
    if (this.directFieldAccessor == null) {
      this.directFieldAccessor = createDirectFieldAccessor();
      this.directFieldAccessor.setExtractOldValueForEditor(true);
      this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
    }
    return this.directFieldAccessor;
  }

  /**
   * Create a new DirectFieldAccessor for the underlying target object.
   *
   * @see #getTarget()
   */
  protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
    if (this.target == null) {
      throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
    }
    return BeanWrapper.forDirectFieldAccess(this.target);
  }

}
