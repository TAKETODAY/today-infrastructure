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
