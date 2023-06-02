/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web.error;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import cn.taketoday.util.CollectionUtils;

/**
 * Options controlling the contents of {@code ErrorAttributes}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:13
 */
public final class ErrorAttributeOptions {

  private final Set<Include> includes;

  private ErrorAttributeOptions(Set<Include> includes) {
    this.includes = includes;
  }

  /**
   * Get the option for including the specified attribute in the error response.
   *
   * @param include error attribute to get
   * @return {@code true} if the {@code Include} attribute is included in the error
   * response, {@code false} otherwise
   */
  public boolean isIncluded(Include include) {
    return this.includes.contains(include);
  }

  /**
   * Get all options for including attributes in the error response.
   *
   * @return the options
   */
  public Set<Include> getIncludes() {
    return this.includes;
  }

  /**
   * Return an {@code ErrorAttributeOptions} that includes the specified attribute
   * {@link Include} options.
   *
   * @param includes error attributes to include
   * @return an {@code ErrorAttributeOptions}
   */
  public ErrorAttributeOptions including(Include... includes) {
    EnumSet<Include> updated = copyIncludes();
    CollectionUtils.addAll(updated, includes);
    return new ErrorAttributeOptions(Collections.unmodifiableSet(updated));
  }

  /**
   * Return an {@code ErrorAttributeOptions} that excludes the specified attribute
   * {@link Include} options.
   *
   * @param excludes error attributes to exclude
   * @return an {@code ErrorAttributeOptions}
   */
  public ErrorAttributeOptions excluding(Include... excludes) {
    EnumSet<Include> updated = copyIncludes();
    for (Include exclude : excludes) {
      updated.remove(exclude);
    }
    return new ErrorAttributeOptions(Collections.unmodifiableSet(updated));
  }

  private EnumSet<Include> copyIncludes() {
    return includes.isEmpty() ? EnumSet.noneOf(Include.class) : EnumSet.copyOf(includes);
  }

  /**
   * Create an {@code ErrorAttributeOptions} with defaults.
   *
   * @return an {@code ErrorAttributeOptions}
   */
  public static ErrorAttributeOptions defaults() {
    return of();
  }

  /**
   * Create an {@code ErrorAttributeOptions} that includes the specified attribute
   * {@link Include} options.
   *
   * @param includes error attributes to include
   * @return an {@code ErrorAttributeOptions}
   */
  public static ErrorAttributeOptions of(Include... includes) {
    return of(Arrays.asList(includes));
  }

  /**
   * Create an {@code ErrorAttributeOptions} that includes the specified attribute
   * {@link Include} options.
   *
   * @param includes error attributes to include
   * @return an {@code ErrorAttributeOptions}
   */
  public static ErrorAttributeOptions of(Collection<Include> includes) {
    return new ErrorAttributeOptions(
            (includes.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(includes)));
  }

  /**
   * Error attributes that can be included in an error response.
   */
  public enum Include {

    /**
     * Include the exception class name attribute.
     */
    EXCEPTION,

    /**
     * Include the stack trace attribute.
     */
    STACK_TRACE,

    /**
     * Include the message attribute.
     */
    MESSAGE,

    /**
     * Include the binding errors attribute.
     */
    BINDING_ERRORS

  }

}
