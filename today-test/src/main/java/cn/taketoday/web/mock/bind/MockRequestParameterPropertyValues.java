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

package cn.taketoday.web.mock.bind;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.web.mock.MockUtils;

/**
 * PropertyValues implementation created from parameters in a ServletRequest.
 * Can look for all property values beginning with a certain prefix and
 * prefix separator (default is "_").
 *
 * <p>For example, with a prefix of "today", "today_param1" and
 * "today_param2" result in a Map with "param1" and "param2" as keys.
 *
 * <p>This class is not immutable to be able to efficiently remove property
 * values that should be ignored for binding.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockUtils#getParametersStartingWith
 * @since 4.0 2022/3/2 16:31
 */
@SuppressWarnings("serial")
public class MockRequestParameterPropertyValues extends PropertyValues {

  /** Default prefix separator. */
  public static final String DEFAULT_PREFIX_SEPARATOR = "_";

  /**
   * Create new ServletRequestPropertyValues using no prefix
   * (and hence, no prefix separator).
   *
   * @param request the HTTP request
   */
  public MockRequestParameterPropertyValues(MockRequest request) {
    this(request, null, null);
  }

  /**
   * Create new ServletRequestPropertyValues using the given prefix and
   * the default prefix separator (the underscore character "_").
   *
   * @param request the HTTP request
   * @param prefix the prefix for parameters (the full prefix will
   * consist of this plus the separator)
   * @see #DEFAULT_PREFIX_SEPARATOR
   */
  public MockRequestParameterPropertyValues(MockRequest request, @Nullable String prefix) {
    this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
  }

  /**
   * Create new ServletRequestPropertyValues supplying both prefix and
   * prefix separator.
   *
   * @param request the HTTP request
   * @param prefix the prefix for parameters (the full prefix will
   * consist of this plus the separator)
   * @param prefixSeparator separator delimiting prefix (e.g. "spring")
   * and the rest of the parameter name ("param1", "param2")
   */
  public MockRequestParameterPropertyValues(
          MockRequest request, @Nullable String prefix, @Nullable String prefixSeparator) {
    super(MockUtils.getParametersStartingWith(
            request, (prefix != null ? prefix + prefixSeparator : null)));
  }

}
