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

package cn.taketoday.validation;

import java.io.Serializable;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.ConfigurablePropertyAccessor;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of the {@link Errors} and {@link BindingResult}
 * interfaces, for the registration and evaluation of binding errors on
 * JavaBean objects.
 *
 * <p>Performs standard JavaBean property access, also supporting nested
 * properties. Normally, application code will work with the
 * {@code Errors} interface or the {@code BindingResult} interface.
 * A {@link DataBinder} returns its {@code BindingResult} via
 * {@link DataBinder#getBindingResult()}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initBeanPropertyAccess()
 * @see DirectFieldBindingResult
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanPropertyBindingResult extends AbstractPropertyBindingResult implements Serializable {

  @Nullable
  private final Object target;

  private final boolean autoGrowNestedPaths;

  private final int autoGrowCollectionLimit;

  @Nullable
  private transient BeanWrapper beanWrapper;

  /**
   * Creates a new instance of the {@link BeanPropertyBindingResult} class.
   *
   * @param target the target bean to bind onto
   * @param objectName the name of the target object
   */
  public BeanPropertyBindingResult(@Nullable Object target, String objectName) {
    this(target, objectName, true, Integer.MAX_VALUE);
  }

  /**
   * Creates a new instance of the {@link BeanPropertyBindingResult} class.
   *
   * @param target the target bean to bind onto
   * @param objectName the name of the target object
   * @param autoGrowNestedPaths whether to "auto-grow" a nested path that contains a null value
   * @param autoGrowCollectionLimit the limit for array and collection auto-growing
   */
  public BeanPropertyBindingResult(@Nullable Object target, String objectName,
          boolean autoGrowNestedPaths, int autoGrowCollectionLimit) {

    super(objectName);
    this.target = target;
    this.autoGrowNestedPaths = autoGrowNestedPaths;
    this.autoGrowCollectionLimit = autoGrowCollectionLimit;
  }

  @Override
  @Nullable
  public final Object getTarget() {
    return this.target;
  }

  /**
   * Returns the {@link BeanWrapper} that this instance uses.
   * Creates a new one if none existed before.
   *
   * @see #createBeanWrapper()
   */
  @Override
  public final ConfigurablePropertyAccessor getPropertyAccessor() {
    if (this.beanWrapper == null) {
      this.beanWrapper = createBeanWrapper();
      this.beanWrapper.setExtractOldValueForEditor(true);
      this.beanWrapper.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
      this.beanWrapper.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
    }
    return this.beanWrapper;
  }

  /**
   * Create a new {@link BeanWrapper} for the underlying target object.
   *
   * @see #getTarget()
   */
  protected BeanWrapper createBeanWrapper() {
    if (this.target == null) {
      throw new IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'");
    }
    return BeanWrapper.forBeanPropertyAccess(this.target);
  }

}
