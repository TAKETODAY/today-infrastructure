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

package infra.validation;

import org.jspecify.annotations.Nullable;

import infra.beans.BeanWrapper;
import infra.beans.ConfigurablePropertyAccessor;

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
