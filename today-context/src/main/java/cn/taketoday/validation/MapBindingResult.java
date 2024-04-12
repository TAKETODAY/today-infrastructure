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
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Map-based implementation of the BindingResult interface,
 * supporting registration and evaluation of binding errors on
 * Map attributes.
 *
 * <p>Can be used as errors holder for custom binding onto a
 * Map, for example when invoking a Validator for a Map object.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Map
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MapBindingResult extends AbstractBindingResult implements Serializable {

  private final Map<?, ?> target;

  /**
   * Create a new MapBindingResult instance.
   *
   * @param target the target Map to bind onto
   * @param objectName the name of the target object
   */
  public MapBindingResult(Map<?, ?> target, String objectName) {
    super(objectName);
    Assert.notNull(target, "Target Map is required");
    this.target = target;
  }

  /**
   * Return the target Map to bind onto.
   */
  public final Map<?, ?> getTargetMap() {
    return this.target;
  }

  @Override
  @NonNull
  public final Object getTarget() {
    return this.target;
  }

  @Override
  @Nullable
  protected Object getActualFieldValue(String field) {
    return this.target.get(field);
  }

}
