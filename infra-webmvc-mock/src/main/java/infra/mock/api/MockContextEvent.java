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

/**
 * This is the event class for notifications about changes to the servlet context of a web application.
 *
 * @see MockContextListener
 */
public class MockContextEvent extends java.util.EventObject {

  private static final long serialVersionUID = -7501701636134222423L;

  /**
   * Construct a MockContextEvent from the given context.
   *
   * @param source - the MockContext that is sending the event.
   */
  public MockContextEvent(MockContext source) {
    super(source);
  }

  /**
   * Return the MockContext that changed.
   *
   * @return the MockContext that sent the event.
   */
  public MockContext getMockContext() {
    return (MockContext) super.getSource();
  }
}
