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

package infra.web.mock.bind;

import org.jspecify.annotations.Nullable;

import infra.beans.PropertyValues;
import infra.mock.api.MockRequest;
import infra.web.mock.MockUtils;

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
