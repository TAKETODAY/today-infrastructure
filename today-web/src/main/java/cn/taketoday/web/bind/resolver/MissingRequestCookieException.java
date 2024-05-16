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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.bind.MissingRequestValueException;
import cn.taketoday.web.bind.RequestBindingException;

/**
 * {@link RequestBindingException} subclass that indicates
 * that a request cookie expected in the method parameters of an
 * {@code @RequestMapping} method is not present.
 *
 * @author TODAY 2021/3/10 20:14
 */
public class MissingRequestCookieException extends MissingRequestValueException {

  private final String cookieName;

  private final MethodParameter parameter;

  /**
   * Constructor for MissingRequestCookieException.
   *
   * @param cookieName the name of the missing request cookie
   * @param parameter the method parameter
   */
  public MissingRequestCookieException(String cookieName, MethodParameter parameter) {
    this(cookieName, parameter, false);
  }

  /**
   * Constructor for use when a value was present but converted to {@code null}.
   *
   * @param cookieName the name of the missing request cookie
   * @param parameter the method parameter
   * @param missingAfterConversion whether the value became null after conversion
   */
  public MissingRequestCookieException(
          String cookieName, MethodParameter parameter, boolean missingAfterConversion) {

    super("", missingAfterConversion);
    this.cookieName = cookieName;
    this.parameter = parameter;
    setDetail("Required cookie '%s' is not present.".formatted(this.cookieName));
  }

  @Override
  public String getMessage() {
    return "Required cookie '%s' for method parameter type %s is %s"
            .formatted(this.cookieName, this.parameter.getNestedParameterType().getSimpleName(),
                    isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the expected name of the request cookie.
   */
  public final String getCookieName() {
    return this.cookieName;
  }

  /**
   * Return the method parameter bound to the request cookie.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
  }

}
