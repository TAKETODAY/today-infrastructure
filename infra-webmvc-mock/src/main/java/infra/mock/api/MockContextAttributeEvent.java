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
 * Event class for notifications about changes to the attributes of the MockContext of a web application.
 *
 * @see MockContextAttributeListener
 */
public class MockContextAttributeEvent extends MockContextEvent {

  private static final long serialVersionUID = -5804680734245618303L;

  private String name;
  private Object value;

  /**
   * Constructs a MockContextAttributeEvent from the given MockContext, attribute name, and attribute value.
   *
   * @param source the MockContext whose attribute changed
   * @param name the name of the MockContext attribute that changed
   * @param value the value of the MockContext attribute that changed
   */
  public MockContextAttributeEvent(MockContext source, String name, Object value) {
    super(source);
    this.name = name;
    this.value = value;
  }

  /**
   * Gets the name of the MockContext attribute that changed.
   *
   * @return the name of the MockContext attribute that changed
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the value of the MockContext attribute that changed.
   *
   * <p>
   * If the attribute was added, this is the value of the attribute. If the attribute was removed, this is the value of
   * the removed attribute. If the attribute was replaced, this is the old value of the attribute.
   *
   * @return the value of the MockContext attribute that changed
   */
  public Object getValue() {
    return this.value;
  }
}
