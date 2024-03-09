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

package cn.taketoday.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.lang.Nullable;

/**
 * Thrown when the resolution of placeholder failed. This exception provides
 * the placeholder as well as the hierarchy of values that led to the issue.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PlaceholderResolutionException extends IllegalArgumentException {

  private final String reason;

  private final String placeholder;

  private final List<String> values;

  /**
   * Create an exception using the specified reason for its message.
   *
   * @param reason the reason for the exception, should contain the placeholder
   * @param placeholder the placeholder
   * @param value the original expression that led to the issue if available
   */
  PlaceholderResolutionException(String reason, String placeholder, @Nullable String value) {
    this(reason, placeholder, (value != null ? List.of(value) : Collections.emptyList()));
  }

  private PlaceholderResolutionException(String reason, String placeholder, List<String> values) {
    super(buildMessage(reason, values));
    this.reason = reason;
    this.placeholder = placeholder;
    this.values = values;
  }

  private static String buildMessage(String reason, List<String> values) {
    StringBuilder sb = new StringBuilder();
    sb.append(reason);
    if (!CollectionUtils.isEmpty(values)) {
      String valuesChain = values.stream().map(value -> "\"" + value + "\"")
              .collect(Collectors.joining(" <-- "));
      sb.append(" in value %s".formatted(valuesChain));
    }
    return sb.toString();
  }

  /**
   * Return a {@link PlaceholderResolutionException} that provides
   * an additional parent value.
   *
   * @param value the parent value to add
   * @return a new exception with the parent value added
   */
  PlaceholderResolutionException withValue(String value) {
    List<String> allValues = new ArrayList<>(this.values);
    allValues.add(value);
    return new PlaceholderResolutionException(this.reason, this.placeholder, allValues);
  }

  /**
   * Return the placeholder that could not be resolved.
   *
   * @return the unresolvable placeholder
   */
  public String getPlaceholder() {
    return this.placeholder;
  }

  /**
   * Return a contextualized list of the resolution attempts that led to this
   * exception, where the first element is the value that generated this
   * exception.
   *
   * @return the stack of values that led to this exception
   */
  public List<String> getValues() {
    return this.values;
  }

}
