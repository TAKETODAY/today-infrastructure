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

package cn.taketoday.mock.api;

import java.util.Enumeration;

/**
 * A filter configuration object used by a servlet container to pass information to a filter during initialization.
 *
 * @see Filter
 * @since Servlet 2.3
 */
public interface FilterConfig {

  /**
   * Returns the filter-name of this filter as defined in the deployment descriptor.
   *
   * @return the filter name of this filter
   */
  public String getFilterName();

  /**
   * Returns a reference to the {@link MockContext} in which the caller is executing.
   *
   * @return a {@link MockContext} object, used by the caller to interact with its servlet container
   * @see MockContext
   */
  public MockContext getServletContext();

  /**
   * Returns a <code>String</code> containing the value of the named initialization parameter, or <code>null</code> if the
   * initialization parameter does not exist.
   *
   * @param name a <code>String</code> specifying the name of the initialization parameter
   * @return a <code>String</code> containing the value of the initialization parameter, or <code>null</code> if the
   * initialization parameter does not exist
   */
  public String getInitParameter(String name);

  /**
   * Returns the names of the filter's initialization parameters as an <code>Enumeration</code> of <code>String</code>
   * objects, or an empty <code>Enumeration</code> if the filter has no initialization parameters.
   *
   * @return an <code>Enumeration</code> of <code>String</code> objects containing the names of the filter's
   * initialization parameters
   */
  public Enumeration<String> getInitParameterNames();

}
