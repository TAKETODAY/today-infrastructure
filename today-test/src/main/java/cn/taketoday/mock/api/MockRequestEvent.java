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
 * Events of this kind indicate lifecycle events for a ServletRequest. The source of the event is the MockContext of
 * this web application.
 *
 * @see MockRequestListener
 */
public class MockRequestEvent extends java.util.EventObject {

  private static final long serialVersionUID = -7467864054698729101L;

  private final transient MockRequest request;

  /**
   * Construct a ServletRequestEvent for the given MockContext and ServletRequest.
   *
   * @param sc the MockContext of the web application.
   * @param request the ServletRequest that is sending the event.
   */
  public MockRequestEvent(MockContext sc, MockRequest request) {
    super(sc);
    this.request = request;
  }

  /**
   * Returns the ServletRequest that is changing.
   *
   * @return the {@link MockRequest} corresponding to this event.
   */
  public MockRequest getServletRequest() {
    return this.request;
  }

  /**
   * Returns the MockContext of this web application.
   *
   * @return the {@link MockContext} for this web application.
   */
  public MockContext getMockContext() {
    return (MockContext) super.getSource();
  }
}
