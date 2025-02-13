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
