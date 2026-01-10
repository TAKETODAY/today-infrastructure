/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.api;

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
