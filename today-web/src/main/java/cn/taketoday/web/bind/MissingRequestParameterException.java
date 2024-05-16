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

package cn.taketoday.web.bind;

/**
 * {@link RequestBindingException} subclass that indicates a missing parameter.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:20
 */
public class MissingRequestParameterException extends MissingRequestValueException {

  private final String parameterName;

  private final String parameterType;

  /**
   * Constructor for MissingRequestParameterException.
   *
   * @param parameterName the name of the missing parameter
   * @param parameterType the expected type of the missing parameter
   */
  public MissingRequestParameterException(String parameterName, String parameterType) {
    this(parameterName, parameterType, false);
  }

  /**
   * Constructor for use when a value was present but converted to {@code null}.
   *
   * @param parameterName the name of the missing parameter
   * @param parameterType the expected type of the missing parameter
   * @param missingAfterConversion whether the value became null after conversion
   */
  public MissingRequestParameterException(
          String parameterName, String parameterType, boolean missingAfterConversion) {

    super("", missingAfterConversion);
    this.parameterName = parameterName;
    this.parameterType = parameterType;
    getBody().setDetail("Required parameter '%s' is not present.".formatted(this.parameterName));
  }

  @Override
  public String getMessage() {
    return "Required request parameter '%s' for method parameter type %s is %s"
            .formatted(this.parameterName, this.parameterType, isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the name of the offending parameter.
   */
  public final String getParameterName() {
    return this.parameterName;
  }

  /**
   * Return the expected type of the offending parameter.
   */
  public final String getParameterType() {
    return this.parameterType;
  }

}
