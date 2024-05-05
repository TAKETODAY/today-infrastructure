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
 * A mock configuration object used by a mock container to pass information to a mock during initialization.
 */
public interface MockConfig {

  /**
   * Returns the name of this mock instance. The name may be provided via server administration, assigned in the web
   * application deployment descriptor, or for an unregistered (and thus unnamed) mock instance it will be the
   * mock's class name.
   *
   * @return the name of the mock instance
   */
  public String getMockName();

  /**
   * Returns a reference to the {@link MockContext} in which the caller is executing.
   *
   * @return a {@link MockContext} object, used by the caller to interact with its servlet container
   * @see MockContext
   */
  public MockContext getMockContext();

  /**
   * Gets the value of the initialization parameter with the given name.
   *
   * @param name the name of the initialization parameter whose value to get
   * @return a <code>String</code> containing the value of the initialization parameter, or <code>null</code> if the
   * initialization parameter does not exist
   */
  public String getInitParameter(String name);

  /**
   * Returns the names of the servlet's initialization parameters as an <code>Enumeration</code> of <code>String</code>
   * objects, or an empty <code>Enumeration</code> if the servlet has no initialization parameters.
   *
   * @return an <code>Enumeration</code> of <code>String</code> objects containing the names of the servlet's
   * initialization parameters
   */
  public Enumeration<String> getInitParameterNames();

}
